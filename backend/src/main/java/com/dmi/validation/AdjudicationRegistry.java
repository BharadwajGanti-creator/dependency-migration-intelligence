package com.dmi.validation;

import com.dmi.domain.MigrationEvidenceRef;
import com.dmi.domain.MigrationReview;
import com.dmi.domain.MigrationVerdict;
import com.dmi.domain.OpenRewriteComparisonStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
public class AdjudicationRegistry {

    private static final String RESOURCE = "validation/b2-adjudications.json";

    private final Map<String, MigrationReview> byFindingId;
    private final MigrationReview unverifiedFallback;

    @Autowired
    public AdjudicationRegistry(ObjectMapper objectMapper) throws IOException {
        JsonNode root = objectMapper.readTree(new ClassPathResource(RESOURCE).getInputStream());
        var loaded = parseEntries(root);
        this.byFindingId = loaded.byFindingId();
        this.unverifiedFallback = loaded.unverifiedFallback();
    }

    static AdjudicationRegistry loadFromClasspath(ObjectMapper objectMapper, String classpathResource)
            throws IOException {
        JsonNode root = objectMapper.readTree(new ClassPathResource(classpathResource).getInputStream());
        var loaded = parseEntries(root);
        return new AdjudicationRegistry(loaded.byFindingId(), loaded.unverifiedFallback());
    }

    private AdjudicationRegistry(Map<String, MigrationReview> byFindingId, MigrationReview unverifiedFallback) {
        this.byFindingId = byFindingId;
        this.unverifiedFallback = unverifiedFallback;
    }

    private static ParsedAdjudications parseEntries(JsonNode root) {
        Map<String, MigrationReview> loaded = new LinkedHashMap<>();
        Set<String> seenIds = new HashSet<>();
        for (JsonNode node : root.get("entries")) {
            String findingId = requiredText(node, "findingId");
            if (!seenIds.add(findingId)) {
                throw new IllegalStateException("Duplicate findingId in adjudications: " + findingId);
            }
            MigrationVerdict verdict = parseVerdict(requiredText(node, "verdict"));
            OpenRewriteComparisonStatus comparison = parseComparison(requiredText(node, "openRewriteComparison"));
            List<MigrationEvidenceRef> evidence = new ArrayList<>();
            node.get("evidence").forEach(e -> evidence.add(new MigrationEvidenceRef(
                    requiredText(e, "url"),
                    requiredText(e, "title"),
                    requiredText(e, "section"))));
            loaded.put(findingId, new MigrationReview(
                    verdict,
                    comparison,
                    requiredText(node, "evidenceProves"),
                    List.copyOf(evidence),
                    requiredText(node, "testedScenario"),
                    requiredText(node, "openRewriteRecipe"),
                    requiredText(node, "openRewritePluginVersion"),
                    requiredText(node, "openRewriteRecipeArtifactVersion"),
                    requiredText(node, "reviewerNotes"),
                    requiredText(node, "lastReviewedUtc"),
                    requiredText(node, "nextHumanCheck")));
        }
        MigrationReview fallback = new MigrationReview(
                MigrationVerdict.UNVERIFIED,
                OpenRewriteComparisonStatus.NOT_TESTED,
                "No reviewed adjudication entry exists for this finding id.",
                List.of(),
                "Not reviewed",
                "",
                "",
                "",
                "Missing catalogue adjudication; treat as unknown migration consequence.",
                "",
                "Obtain version-specific evidence before acting on this finding.");
        return new ParsedAdjudications(Map.copyOf(loaded), fallback);
    }

    private record ParsedAdjudications(Map<String, MigrationReview> byFindingId, MigrationReview unverifiedFallback) {}

    public Optional<MigrationReview> find(String findingId) {
        return Optional.ofNullable(byFindingId.get(findingId));
    }

    public MigrationReview resolve(String findingId) {
        return byFindingId.getOrDefault(findingId, unverifiedFallback);
    }

    public Set<String> reviewedFindingIds() {
        return byFindingId.keySet();
    }

    private static String requiredText(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.asText().isBlank()) {
            throw new IllegalStateException("Missing adjudication field: " + field);
        }
        return value.asText();
    }

    private static MigrationVerdict parseVerdict(String raw) {
        try {
            return MigrationVerdict.valueOf(raw);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("Unknown migration verdict: " + raw, ex);
        }
    }

    private static OpenRewriteComparisonStatus parseComparison(String raw) {
        try {
            return OpenRewriteComparisonStatus.valueOf(raw);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("Unknown OpenRewrite comparison status: " + raw, ex);
        }
    }
}
