package com.dmi.validation;

import com.dmi.domain.MigrationVerdict;
import com.dmi.domain.OpenRewriteComparisonStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class AdjudicationRegistryTest {

    @Autowired
    AdjudicationRegistry registry;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void loadsFourReviewedFindings() {
        assertThat(registry.reviewedFindingIds()).containsExactlyInAnyOrder(
                "jakarta-servlet-api",
                "jakarta-annotation-postconstruct",
                "pathmatch-strategy",
                "parent-version-bump");
    }

    @Test
    void missingEntryReturnsUnverified() {
        assertThat(registry.resolve("unknown-finding").verdict()).isEqualTo(MigrationVerdict.UNVERIFIED);
        assertThat(registry.resolve("unknown-finding").openRewriteComparison())
                .isEqualTo(OpenRewriteComparisonStatus.NOT_TESTED);
    }

    @Test
    void pathmatchIsReviewRecommendedNotActionRequired() {
        assertThat(registry.resolve("pathmatch-strategy").verdict()).isEqualTo(MigrationVerdict.REVIEW_RECOMMENDED);
        assertThat(registry.resolve("pathmatch-strategy").openRewriteComparison())
                .isEqualTo(OpenRewriteComparisonStatus.NOT_PRESENT_IN_PATCH);
    }

    @Test
    void rejectsInvalidVerdictEnum() {
        assertThatThrownBy(() -> AdjudicationRegistry.loadFromClasspath(objectMapper, "validation/invalid-verdict-adjudication.json"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Unknown migration verdict");
    }

    @Test
    void rejectsDuplicateFindingIds() {
        assertThatThrownBy(() -> AdjudicationRegistry.loadFromClasspath(objectMapper, "validation/duplicate-id-adjudication.json"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Duplicate findingId");
    }
}
