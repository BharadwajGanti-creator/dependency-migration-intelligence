package com.dmi.explanation;

import com.dmi.domain.AnalysisReport;
import com.dmi.domain.Confidence;
import com.dmi.domain.DependencyCoordinate;
import com.dmi.domain.Finding;
import com.dmi.domain.MigrationReview;
import com.dmi.domain.MigrationVerdict;
import com.dmi.domain.OpenRewriteComparisonStatus;
import com.dmi.validation.MigrationReviewEnricher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class EvidencePackBuilderTest {

    @Autowired
    EvidencePackBuilder evidencePackBuilder;

    @Autowired
    MigrationReviewEnricher migrationReviewEnricher;

    @Test
    void packSanitizesInjectionLikeTextInReviewFields() {
        Finding raw = new Finding(
                "jakarta-servlet-api",
                "jakarta",
                Confidence.CONFIRMED_STATIC,
                "summary",
                List.of(),
                List.of(),
                "rationale",
                List.of(),
                null);
        List<Finding> enriched = migrationReviewEnricher.enrich(List.of(raw));
        Finding finding = enriched.getFirst();
        AnalysisReport report = new AnalysisReport(
                "req",
                "digest-abc",
                Instant.parse("2026-10-17T00:00:00Z"),
                new DependencyCoordinate("g", "a", "2.7.18"),
                new DependencyCoordinate("g", "a", "3.2.12"),
                List.of(finding),
                List.of("limit"),
                "ref");

        EvidencePack pack = evidencePackBuilder.build(report, finding);
        assertThat(pack.items()).isNotEmpty();
        assertThat(pack.packId()).startsWith("pack-");
    }

    @Test
    void pathmatchReviewVerdictPreservedInPack() {
        MigrationReview review = new MigrationReview(
                MigrationVerdict.REVIEW_RECOMMENDED,
                OpenRewriteComparisonStatus.NOT_PRESENT_IN_PATCH,
                "Docs only",
                List.of(),
                "static",
                "",
                "",
                "",
                "notes",
                "2026-10-17",
                "check");
        Finding finding = new Finding(
                "pathmatch-strategy",
                "config",
                Confidence.CONFIRMED_STATIC,
                "summary",
                List.of(),
                List.of(),
                "rationale",
                List.of(),
                review);
        AnalysisReport report = new AnalysisReport(
                "req",
                "digest",
                Instant.now(),
                new DependencyCoordinate("org.springframework.boot", "spring-boot-starter-parent", "2.7.18"),
                new DependencyCoordinate("org.springframework.boot", "spring-boot-starter-parent", "3.2.12"),
                List.of(finding),
                List.of(),
                "ref");

        EvidencePack pack = evidencePackBuilder.build(report, finding);
        assertThat(pack.items().stream().anyMatch(i -> i.detail().contains("REVIEW_RECOMMENDED"))).isTrue();
        assertThat(pack.items().stream().anyMatch(i -> i.detail().contains("NOT_PRESENT_IN_PATCH"))).isTrue();
    }
}
