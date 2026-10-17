package com.dmi.explanation;

import com.dmi.domain.MigrationEvidenceRef;
import com.dmi.domain.MigrationReview;
import com.dmi.domain.MigrationVerdict;
import com.dmi.domain.OpenRewriteComparisonStatus;
import com.dmi.domain.StructuredExplanation;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ExplanationLabelledCorpusTest {

    private ExplanationVerifier verifier;

    @BeforeEach
    void setUp() {
        ExplanationProperties props = new ExplanationProperties();
        props.setMaxSummaryChars(2000);
        props.setMaxClaimChars(800);
        props.setMaxClaims(12);
        verifier = new ExplanationVerifier(props);
    }

    @Test
    void labelledCorpusMatchesExpectedOutcomes() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        InputStream in = getClass().getResourceAsStream("/validation/b3-labelled-cases.json");
        JsonNode root = mapper.readTree(in);
        JsonNode cases = root.get("cases");

        int acceptedKnownValid = 0;
        int rejectedKnownInvalid = 0;
        int total = cases.size();

        for (JsonNode caseNode : cases) {
            String caseId = caseNode.get("caseId").asText();
            boolean expectedAccepted = caseNode.get("expectedAccepted").asBoolean();
            EvidencePack pack = minimalPack(caseNode);
            MigrationReview review = reviewFromCase(caseNode);
            StructuredExplanation output = mapper.treeToValue(caseNode.get("modelOutput"), StructuredExplanation.class);

            var result = verifier.verify(pack, review, output);
            assertThat(result.accepted())
                    .as("case %s", caseId)
                    .isEqualTo(expectedAccepted);

            if (expectedAccepted) {
                acceptedKnownValid++;
            } else {
                rejectedKnownInvalid++;
            }
        }

        assertThat(total).isGreaterThanOrEqualTo(12);
        assertThat(rejectedKnownInvalid).isGreaterThanOrEqualTo(8);
        assertThat(acceptedKnownValid).isGreaterThanOrEqualTo(4);
    }

    private static EvidencePack minimalPack(JsonNode caseNode) {
        String findingId = caseNode.get("findingId").asText();
        String injection = caseNode.path("injectionInPackDetail").asText("");
        List<EvidenceItem> items = new ArrayList<>();
        items.add(new EvidenceItem("ev-1", EvidenceKind.MIGRATION_PAIR, "Migration pair", "2.7.18 → 3.2.12"));
        items.add(new EvidenceItem("ev-2", EvidenceKind.FIXTURE_DIGEST, "Fixture digest", "digest-demo"));
        items.add(new EvidenceItem("ev-3", EvidenceKind.SOURCE_LOCATION, "src/App.java:1", "path:line only"));
        items.add(new EvidenceItem(
                "ev-4",
                EvidenceKind.REVIEWED_VERDICT,
                "Reviewed verdict",
                caseNode.get("verdict").asText() + (injection.isBlank() ? "" : " " + injection)));
        items.add(new EvidenceItem(
                "ev-5",
                EvidenceKind.OPENREWRITE_BASELINE,
                "OpenRewrite comparison",
                caseNode.get("openRewrite").asText()));
        items.add(new EvidenceItem(
                "ev-6",
                EvidenceKind.OFFICIAL_DOC,
                "Spring Boot 3.0 Migration Guide",
                "https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-3.0-Migration-Guide — Jakarta EE 9"));
        return new EvidencePack(
                "pack-test",
                findingId,
                "2.7.18→3.2.12",
                "digest-demo",
                items,
                List.of("Harness-only pack"));
    }

    private static MigrationReview reviewFromCase(JsonNode caseNode) {
        return new MigrationReview(
                MigrationVerdict.valueOf(caseNode.get("verdict").asText()),
                OpenRewriteComparisonStatus.valueOf(caseNode.get("openRewrite").asText()),
                "Reviewed evidence proves scope for harness.",
                List.of(new MigrationEvidenceRef(
                        "https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-3.0-Migration-Guide",
                        "Spring Boot 3.0 Migration Guide",
                        "Jakarta EE 9")),
                "Static scan + offline OpenRewrite dryRun",
                "org.openrewrite.java.spring.boot3.UpgradeSpringBoot_3_2",
                "6.45.0",
                "6.9.0",
                "Harness review notes",
                "2026-10-17",
                "Human follow-up");
    }
}
