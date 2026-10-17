package com.dmi.analysis;

import com.dmi.api.AnalysisException;
import com.dmi.catalogue.AdvisoryCatalogue;
import com.dmi.domain.AnalysisReport;
import com.dmi.domain.AnalysisRequest;
import com.dmi.domain.DependencyCoordinate;
import com.dmi.domain.Finding;
import com.dmi.fixtures.FixtureRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class AnalysisService {

    private static final Logger log = LoggerFactory.getLogger(AnalysisService.class);

    private final FixtureRegistry fixtureRegistry;
    private final MavenManifestReader mavenManifestReader;
    private final SourceEvidenceScanner sourceEvidenceScanner;
    private final AdvisoryCatalogue advisoryCatalogue;
    private final FindingClassifier findingClassifier;

    public AnalysisService(
            FixtureRegistry fixtureRegistry,
            MavenManifestReader mavenManifestReader,
            SourceEvidenceScanner sourceEvidenceScanner,
            AdvisoryCatalogue advisoryCatalogue,
            FindingClassifier findingClassifier) {
        this.fixtureRegistry = fixtureRegistry;
        this.mavenManifestReader = mavenManifestReader;
        this.sourceEvidenceScanner = sourceEvidenceScanner;
        this.advisoryCatalogue = advisoryCatalogue;
        this.findingClassifier = findingClassifier;
    }

    public AnalysisReport analyze(AnalysisRequest request) {
        long start = System.currentTimeMillis();
        if (!fixtureRegistry.isSupportedMigration(
                request.dependencyGroup(),
                request.dependencyArtifact(),
                request.fromVersion(),
                request.toVersion())) {
            throw new AnalysisException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "UNSUPPORTED_MIGRATION",
                    "The requested version pair is not supported for this MVP");
        }

        Path fixtureRoot = fixtureRegistry.resolveFixtureRoot(request.fixtureId());
        String digest = fixtureRegistry.digest(fixtureRoot);
        MavenManifestReader.MavenManifest manifest = mavenManifestReader.read(fixtureRoot);

        var declaredParent = mavenManifestReader.resolveParent(
                manifest, request.dependencyGroup(), request.dependencyArtifact());
        if (declaredParent.isEmpty()) {
            throw new AnalysisException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "COORDINATE_NOT_DECLARED",
                    "Requested dependency is not declared as parent in fixture pom.xml");
        }
        DependencyCoordinate before = declaredParent.get();
        if (!before.version().equals(request.fromVersion())) {
            throw new AnalysisException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "VERSION_MISMATCH",
                    "Fixture declares " + before.version() + " but request specified fromVersion " + request.fromVersion());
        }
        DependencyCoordinate after = new DependencyCoordinate(before.group(), before.artifact(), request.toVersion());

        List<Pattern> patterns = new ArrayList<>();
        advisoryCatalogue.entries().forEach(e -> e.sourcePatterns().forEach(p -> {
            if (!p.isBlank()) {
                patterns.add(Pattern.compile(p));
            }
        }));
        List<SourceEvidenceScanner.ScanMatch> matches = sourceEvidenceScanner.scan(fixtureRoot, patterns);
        List<Finding> findings = findingClassifier.classify(advisoryCatalogue.entries(), matches);

        List<String> limitations = new ArrayList<>(ReportComposer.defaultLimitations());
        if (manifest.dependencies().stream()
                .filter(d -> d.groupId().equals("javax.servlet") && d.artifactId().equals("javax.servlet-api"))
                .count() > 0) {
            limitations.add("Fixture declares javax.servlet-api; remove or replace when moving to Jakarta.");
        }

        AnalysisReport report = ReportComposer.compose(request, digest, before, after, findings, limitations);
        log.info("analysis_complete fixtureId={} elapsedMs={} findingCount={} traceId={}",
                request.fixtureId(),
                System.currentTimeMillis() - start,
                findings.size(),
                MDC.get("traceId"));
        return report;
    }
}
