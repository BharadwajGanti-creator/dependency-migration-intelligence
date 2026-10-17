package com.dmi.validation;

import com.dmi.domain.Confidence;
import com.dmi.domain.Finding;
import com.dmi.domain.MigrationVerdict;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class MigrationReviewEnricherTest {

    @Autowired
    MigrationReviewEnricher enricher;

    @Test
    void enrichAttachesReviewWithoutChangingStaticConfidence() {
        Finding raw = new Finding(
                "pathmatch-strategy",
                "MIGRATION_SIGNAL",
                Confidence.CONFIRMED_STATIC,
                "summary",
                List.of(),
                List.of(),
                "static rationale",
                List.of("check"),
                null);
        Finding enriched = enricher.enrich(List.of(raw)).get(0);
        assertThat(enriched.confidence()).isEqualTo(Confidence.CONFIRMED_STATIC);
        assertThat(enriched.migrationReview().verdict()).isEqualTo(MigrationVerdict.REVIEW_RECOMMENDED);
    }

    @Test
    void commentOnlyPossibleConfidenceDoesNotImplyReviewedActionOnUnknownId() {
        assertThat(enricher.reviewFor("synthetic-negative-id").verdict()).isEqualTo(MigrationVerdict.UNVERIFIED);
    }
}
