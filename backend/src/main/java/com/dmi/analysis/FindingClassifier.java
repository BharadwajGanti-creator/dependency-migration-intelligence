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
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Component
public class FindingClassifier {

    public List<Finding> classify(List<AdvisoryCatalogue.CatalogueEntry> entries, List<SourceEvidenceScanner.ScanMatch> scanMatches) {
        Map<String, FindingBuilder> builders = new LinkedHashMap<>();
        for (AdvisoryCatalogue.CatalogueEntry entry : entries) {
            List<SourceLocation> locations = new ArrayList<>();
            for (String patternStr : entry.sourcePatterns()) {
                if (patternStr.isBlank()) {
                    continue;
                }
                Pattern pattern = Pattern.compile(patternStr);
                for (SourceEvidenceScanner.ScanMatch match : scanMatches) {
                    if (pattern.matcher(match.excerpt()).find() || Pattern.compile(patternStr).matcher(match.excerpt()).find()) {
                        locations.add(new SourceLocation(match.relativePath(), match.line()));
                    }
                }
            }
            locations = locations.stream().distinct()
                    .sorted(Comparator.comparing(SourceLocation::path).thenComparingInt(SourceLocation::line))
                    .toList();

            Confidence confidence;
            String rationale;
            if (!locations.isEmpty()) {
                confidence = Confidence.CONFIRMED_STATIC;
                rationale = "Static pattern match in fixture source with curated documentation linkage.";
            } else if (entry.sourcePatterns().isEmpty()) {
                confidence = Confidence.POSSIBLE;
                rationale = "Documentation-only guidance for the requested upgrade; no static signal configured.";
            } else {
                confidence = Confidence.NOT_ASSESSED;
                rationale = "Catalogue rule exists but no matching source evidence in bounded scan.";
            }

            List<DocumentationRef> docs = entry.documentation().stream()
                    .map(d -> new DocumentationRef(d.url(), d.title(), d.section()))
                    .toList();

            builders.put(entry.id(), new FindingBuilder(
                    entry.id(),
                    "MIGRATION_SIGNAL",
                    confidence,
                    entry.summary(),
                    new ArrayList<>(locations),
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
