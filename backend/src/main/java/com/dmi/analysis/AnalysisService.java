package com.dmi.analysis;

import com.dmi.api.AnalysisException;
import com.dmi.catalogue.AdvisoryCatalogue;
import com.dmi.domain.AnalysisReport;
import com.dmi.domain.AnalysisRequest;
import com.dmi.domain.DependencyCoordinate;
import com.dmi.domain.Finding;
import com.dmi.fixtures.FixtureRegistry;
import com.dmi.validation.MigrationReviewEnricher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class AnalysisService {

    private static final Logger log = LoggerFactory.getLogger(AnalysisService.class);

    private final FixtureRegistry fixtureRegistry;
    private final MavenManifestReader mavenManifestReader;
    private final SourceEvidenceScanner sourceEvidenceScanner;
    private final AdvisoryCatalogue advisoryCatalogue;
    private final FindingClassifier findingClassifier;
    private final MigrationReviewEnricher migrationReviewEnricher;

    public AnalysisService(
            FixtureRegistry fixtureRegistry,
            MavenManifestReader mavenManifestReader,
            SourceEvidenceScanner sourceEvidenceScanner,
            AdvisoryCatalogue advisoryCatalogue,
            FindingClassifier findingClassifier,
            MigrationReviewEnricher migrationReviewEnricher) {
        this.fixtureRegistry = fixtureRegistry;
        this.mavenManifestReader = mavenManifestReader;
        this.sourceEvidenceScanner = sourceEvidenceScanner;
        this.advisoryCatalogue = advisoryCatalogue;
        this.findingClassifier = findingClassifier;
        this.migrationReviewEnricher = migrationReviewEnricher;
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
        var digestResult = fixtureRegistry.digest(fixtureRoot);
        String digest = digestResult.digest();
        MavenManifestReader.ManifestReadResult manifestResult = mavenManifestReader.read(fixtureRoot);
        MavenManifestReader.MavenManifest manifest = manifestResult.manifest();

        MavenManifestReader.ParentInfo parentInfo = manifest.parent();
        if (parentInfo != null && parentInfo.version().resolution() == VersionResolution.UNRESOLVED_PROPERTY) {
            throw new AnalysisException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "PARENT_VERSION_UNRESOLVED",
                    "Parent version property could not be resolved from pom.xml");
        }

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

        List<PatternDescriptor> patternDescriptors = buildPatternDescriptors();
        ScanResult scanResult = sourceEvidenceScanner.scan(fixtureRoot, patternDescriptors);
        List<Finding> findings = migrationReviewEnricher.enrich(
                findingClassifier.classify(advisoryCatalogue.entries(), scanResult));

        List<String> limitations = new ArrayList<>(ReportComposer.defaultLimitations());
        limitations.addAll(digestResult.limitations());
        manifestResult.diagnostics().forEach(d -> limitations.add("Manifest: " + d.code() + " — " + d.message()));
        manifest.dependencies().stream()
                .filter(d -> d.version().resolution() == VersionResolution.UNRESOLVED_PROPERTY)
                .forEach(d -> limitations.add("Manifest: unresolved property for " + d.groupId() + ":" + d.artifactId()));
        manifest.dependencies().stream()
                .filter(d -> d.version().resolution() == VersionResolution.MANAGED_OR_UNKNOWN)
                .forEach(d -> limitations.add("Manifest: direct dependency " + d.groupId() + ":" + d.artifactId() + " has no declared version (managed or unknown effective version)."));
        if (scanResult.completeness() == ScanCompleteness.PARTIAL) {
            limitations.add("Source scan completeness: PARTIAL — not all eligible files were fully scanned.");
            scanResult.diagnostics().stream()
                    .map(d -> "Scan: " + d.code() + (d.detail().isBlank() ? "" : " (" + d.detail() + ")"))
                    .distinct()
                    .forEach(limitations::add);
        }
        if (manifest.dependencies().stream()
                .anyMatch(d -> "javax.servlet".equals(d.groupId()) && "javax.servlet-api".equals(d.artifactId()))) {
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

    private List<PatternDescriptor> buildPatternDescriptors() {
        Map<String, PatternDescriptor> byKey = new LinkedHashMap<>();
        int nextIndex = 0;
        for (var entry : advisoryCatalogue.entries()) {
            for (String pattern : entry.sourcePatterns()) {
                if (pattern.isBlank() || byKey.containsKey(pattern)) {
                    continue;
                }
                byKey.put(pattern, new PatternDescriptor(nextIndex++, pattern, Pattern.compile(pattern)));
            }
        }
        return new ArrayList<>(byKey.values());
    }
}
