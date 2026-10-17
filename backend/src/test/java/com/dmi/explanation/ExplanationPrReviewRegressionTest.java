package com.dmi.explanation;

import com.dmi.domain.ExplanationClaim;
import com.dmi.domain.MigrationReview;
import com.dmi.domain.MigrationVerdict;
import com.dmi.domain.OpenRewriteComparisonStatus;
import com.dmi.domain.StructuredExplanation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression tests for PR #3 review findings #1, #2, and #4 (executor deferred via real-inference gate).
 */
class ExplanationPrReviewRegressionTest {

    private ExplanationVerifier verifier;

    @BeforeEach
    void setUp() {
        ExplanationProperties props = new ExplanationProperties();
        props.setMaxSummaryChars(2000);
        props.setMaxClaimChars(800);
        props.setMaxClaims(12);
        verifier = new ExplanationVerifier(props);
    }

    /** Finding #1: unsupported URLs in summary must fail closed (not only claim bodies). */
    @Test
    void finding1_rejectsUnsupportedUrlInSummaryOnly() {
        EvidencePack pack = packWithDoc("https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-3.0-Migration-Guide");
        StructuredExplanation bad = new StructuredExplanation(
                "jakarta-servlet-api",
                "Official proof at https://evil.example/only-in-summary",
                List.of(new ExplanationClaim("Static review scope only.", List.of("ev-2"))),
                List.of(),
                List.of());

        var result = verifier.verify(pack, review(), bad);
        assertThat(result.accepted()).isFalse();
        assertThat(result.violations()).anyMatch(v -> v.contains("unsupported URL in summary"));
    }

    /** Finding #2: REJECTED builder must not retain model output — only deterministic B2 envelope. */
    @Test
    void finding2_rejectedResponseUsesDeterministicEnvelopeOnly() {
        MigrationReview review = new MigrationReview(
                MigrationVerdict.REVIEW_RECOMMENDED,
                OpenRewriteComparisonStatus.NOT_PRESENT_IN_PATCH,
                "Docs establish review need only.",
                List.of(),
                "static scan",
                "",
                "",
                "",
                "reviewer notes",
                "2026-10-17",
                "human check");
        var finding = new com.dmi.domain.Finding(
                "pathmatch-strategy",
                "config",
                com.dmi.domain.Confidence.CONFIRMED_STATIC,
                "summary",
                List.of(),
                List.of(),
                "rationale",
                List.of(),
                review);
        EvidencePack pack = new EvidencePack(
                "pack-r",
                "pathmatch-strategy",
                "2.7.18→3.2.12",
                "digest",
                List.of(new EvidenceItem("ev-1", EvidenceKind.REVIEWED_VERDICT, "Reviewed verdict", "REVIEW_RECOMMENDED")),
                List.of());

        var rejected = DeterministicFallbackBuilder.rejected(
                finding, pack, "Explanation rejected by verifier: contradicts REVIEW_RECOMMENDED");

        assertThat(rejected.status()).isEqualTo(com.dmi.domain.ExplanationStatus.REJECTED);
        assertThat(rejected.summary()).doesNotContain("Mandatory migration");
        assertThat(rejected.summary()).contains("Docs establish review need only");
        assertThat(rejected.claims()).singleElement().satisfies(c -> assertThat(c.text()).contains("REVIEW_RECOMMENDED"));
        assertThat(rejected.modelMetadata().experimentStatus()).isEqualTo("REJECTED_BY_VERIFIER");
        assertThat(rejected.modelMetadata().aiAssisted()).isFalse();
    }

    /** Finding #4: URL allowlist must not accept prefix-spoof shorter URLs. */
    @Test
    void finding4_urlAllowlistRejectsPrefixSpoof() {
        EvidencePack pack = packWithDoc("https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-3.0-Migration-Guide");
        StructuredExplanation bad = new StructuredExplanation(
                "jakarta-servlet-api",
                "See shortened spoof",
                List.of(new ExplanationClaim(
                        "Proof at https://github.com/spring-projects/spring-boot/wiki",
                        List.of("ev-2"))),
                List.of(),
                List.of());

        assertThat(verifier.verify(pack, review(), bad).accepted()).isFalse();
    }

    private static EvidencePack packWithDoc(String url) {
        return new EvidencePack(
                "pack-reg",
                "jakarta-servlet-api",
                "2.7.18→3.2.12",
                "digest",
                List.of(
                        new EvidenceItem("ev-1", EvidenceKind.REVIEWED_VERDICT, "Reviewed verdict", "ACTION_REQUIRED"),
                        new EvidenceItem("ev-2", EvidenceKind.OFFICIAL_DOC, "Guide", url + " — section")),
                List.of());
    }

    private static MigrationReview review() {
        return new MigrationReview(
                MigrationVerdict.ACTION_REQUIRED,
                OpenRewriteComparisonStatus.PATCHED_IN_BASELINE,
                "scope",
                List.of(),
                "static",
                "",
                "",
                "",
                "notes",
                "2026-10-17",
                "check");
    }
}
