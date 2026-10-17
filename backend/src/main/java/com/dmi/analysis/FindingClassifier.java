package com.dmi.analysis;

import com.dmi.catalogue.AdvisoryCatalogue;
import com.dmi.domain.Confidence;
import com.dmi.domain.DocumentationRef;
import com.dmi.domain.Finding;
import com.dmi.domain.SourceLocation;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class FindingClassifier {

    public List<Finding> classify(List<AdvisoryCatalogue.CatalogueEntry> entries, ScanResult scanResult) {
        Map<String, FindingBuilder> builders = new LinkedHashMap<>();
        boolean partial = scanResult.completeness() == ScanCompleteness.PARTIAL;

        for (AdvisoryCatalogue.CatalogueEntry entry : entries) {
            Set<SourceLocation> executableLocations = new LinkedHashSet<>();
            Set<SourceLocation> commentLocations = new LinkedHashSet<>();
            for (String patternStr : entry.sourcePatterns()) {
                if (patternStr.isBlank()) {
                    continue;
                }
                for (SourceEvidenceScanner.ScanMatch match : scanResult.matches()) {
                    if (!patternStr.equals(match.patternKey())) {
                        continue;
                    }
                    SourceLocation loc = new SourceLocation(match.relativePath(), match.line());
                    if (match.commentOnly()) {
                        commentLocations.add(loc);
                    } else {
                        executableLocations.add(loc);
                    }
                }
            }
            List<SourceLocation> sortedExecutable = executableLocations.stream()
                    .sorted(Comparator.comparing(SourceLocation::path).thenComparingInt(SourceLocation::line))
                    .toList();

            Confidence confidence;
            String rationale;
            List<SourceLocation> reportedLocations;
            if (!sortedExecutable.isEmpty()) {
                confidence = Confidence.CONFIRMED_STATIC;
                rationale = "Static pattern match on non-comment source line with curated documentation linkage.";
                reportedLocations = new ArrayList<>(sortedExecutable);
            } else if (!commentLocations.isEmpty()) {
                confidence = Confidence.POSSIBLE;
                rationale = "Pattern matched comment or non-executable line only; does not prove live code or config uses this API.";
                reportedLocations = commentLocations.stream()
                        .sorted(Comparator.comparing(SourceLocation::path).thenComparingInt(SourceLocation::line))
                        .toList();
            } else if (entry.sourcePatterns().isEmpty()) {
                confidence = Confidence.POSSIBLE;
                rationale = "Documentation-only guidance for the requested upgrade; no static signal configured.";
                reportedLocations = List.of();
            } else {
                confidence = Confidence.NOT_ASSESSED;
                rationale = partial
                        ? "Catalogue rule exists but scan was PARTIAL; absence of match is not evidence of compatibility."
                        : "Catalogue rule exists but no matching source evidence in bounded scan.";
                reportedLocations = List.of();
            }

            List<DocumentationRef> docs = entry.documentation().stream()
                    .map(d -> new DocumentationRef(d.url(), d.title(), d.section()))
                    .toList();

            builders.put(entry.id(), new FindingBuilder(
                    entry.id(),
                    "MIGRATION_SIGNAL",
                    confidence,
                    entry.summary(),
                    new ArrayList<>(reportedLocations),
                    docs,
                    rationale,
                    new ArrayList<>(entry.proposedChecks())));
        }
        return builders.values().stream()
                .map(FindingBuilder::build)
                .sorted(Comparator.comparing(Finding::id))
                .toList();
    }

    private record FindingBuilder(
            String id,
            String category,
            Confidence confidence,
            String summary,
            List<SourceLocation> sourceLocations,
            List<DocumentationRef> documentation,
            String rationale,
            List<String> proposedChecks) {

        Finding build() {
            return new Finding(
                    id,
                    category,
                    confidence,
                    summary,
                    List.copyOf(sourceLocations),
                    documentation,
                    rationale,
                    proposedChecks,
                    null);
        }
    }
}
