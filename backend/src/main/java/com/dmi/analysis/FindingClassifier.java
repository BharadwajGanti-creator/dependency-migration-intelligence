package com.dmi.analysis;

import com.dmi.catalogue.AdvisoryCatalogue;
import com.dmi.domain.Confidence;
import com.dmi.domain.DocumentationRef;
import com.dmi.domain.Finding;
import com.dmi.domain.SourceLocation;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class FindingClassifier {

    public List<Finding> classify(List<AdvisoryCatalogue.CatalogueEntry> entries, ScanResult scanResult) {
        Map<String, FindingBuilder> builders = new LinkedHashMap<>();
        boolean partial = scanResult.completeness() == ScanCompleteness.PARTIAL;

        for (AdvisoryCatalogue.CatalogueEntry entry : entries) {
            Set<SourceLocation> locations = new HashSet<>();
            for (String patternStr : entry.sourcePatterns()) {
                if (patternStr.isBlank()) {
                    continue;
                }
                for (SourceEvidenceScanner.ScanMatch match : scanResult.matches()) {
                    if (patternStr.equals(match.patternKey())) {
                        locations.add(new SourceLocation(match.relativePath(), match.line()));
                    }
                }
            }
            List<SourceLocation> sortedLocations = locations.stream()
                    .sorted(Comparator.comparing(SourceLocation::path).thenComparingInt(SourceLocation::line))
                    .toList();

            Confidence confidence;
            String rationale;
            if (!sortedLocations.isEmpty()) {
                confidence = Confidence.CONFIRMED_STATIC;
                rationale = "Static pattern match in fixture source with curated documentation linkage.";
            } else if (entry.sourcePatterns().isEmpty()) {
                confidence = Confidence.POSSIBLE;
                rationale = "Documentation-only guidance for the requested upgrade; no static signal configured.";
            } else {
                confidence = Confidence.NOT_ASSESSED;
                rationale = partial
                        ? "Catalogue rule exists but scan was PARTIAL; absence of match is not evidence of compatibility."
                        : "Catalogue rule exists but no matching source evidence in bounded scan.";
            }

            List<DocumentationRef> docs = entry.documentation().stream()
                    .map(d -> new DocumentationRef(d.url(), d.title(), d.section()))
                    .toList();

            builders.put(entry.id(), new FindingBuilder(
                    entry.id(),
                    "MIGRATION_SIGNAL",
                    confidence,
                    entry.summary(),
                    new ArrayList<>(sortedLocations),
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
            return new Finding(id, category, confidence, summary, List.copyOf(sourceLocations), documentation, rationale, proposedChecks);
        }
    }
}
