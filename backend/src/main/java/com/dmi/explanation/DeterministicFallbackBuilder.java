package com.dmi.explanation;

import com.dmi.domain.ExplanationClaim;
import com.dmi.domain.ExplanationModelMetadata;
import com.dmi.domain.ExplanationStatus;
import com.dmi.domain.Finding;
import com.dmi.domain.FindingExplanation;
import com.dmi.domain.MigrationReview;

import java.util.List;

public final class DeterministicFallbackBuilder {

    private DeterministicFallbackBuilder() {}

    public static FindingExplanation disabled(Finding finding, EvidencePack pack) {
        return build(finding, pack, ExplanationStatus.DISABLED, "AI-assisted explanations are disabled.");
    }

    public static FindingExplanation fallback(Finding finding, EvidencePack pack, String reason) {
        return build(finding, pack, ExplanationStatus.FALLBACK, reason);
    }

    public static FindingExplanation rejected(Finding finding, EvidencePack pack, String reason) {
        FindingExplanation base = build(finding, pack, ExplanationStatus.REJECTED, reason);
        ExplanationModelMetadata metadata = new ExplanationModelMetadata(
                "deterministic-b2", "migration-review", false, "REJECTED_BY_VERIFIER");
        return new FindingExplanation(
                base.findingId(),
                base.evidencePackId(),
                ExplanationStatus.REJECTED,
                base.summary(),
                base.claims(),
                base.nextChecks(),
                base.limitations(),
                metadata,
                base.deterministicReview());
    }

    public static FindingExplanation acceptedFromVerified(
            Finding finding,
            EvidencePack pack,
            String summary,
            List<ExplanationClaim> claims,
            ExplanationModelMetadata metadata) {
        MigrationReview review = finding.migrationReview();
        return new FindingExplanation(
                finding.id(),
                pack.packId(),
                ExplanationStatus.ACCEPTED,
                summary,
                claims,
                review != null ? List.of(review.nextHumanCheck()) : List.of(),
                pack.packLimitations(),
                metadata,
                review);
    }

    private static FindingExplanation build(
            Finding finding, EvidencePack pack, ExplanationStatus status, String banner) {
        MigrationReview review = finding.migrationReview();
        String summary = review != null
                ? banner + " " + review.evidenceProves()
                : banner + " No reviewed adjudication available.";
        List<ExplanationClaim> claims = review != null
                ? List.of(new ExplanationClaim(
                        "Reviewed verdict: " + review.verdict().name() + ". " + review.reviewerNotes(),
                        List.of(firstReviewEvidenceId(pack))))
                : List.of();
        ExplanationModelMetadata metadata = new ExplanationModelMetadata(
                "deterministic-b2", "migration-review", false, "NOT_RUN");
        return new FindingExplanation(
                finding.id(),
                pack.packId(),
                status,
                summary,
                claims,
                review != null ? List.of(review.nextHumanCheck()) : List.of(),
                pack.packLimitations(),
                metadata,
                review);
    }

    private static String firstReviewEvidenceId(EvidencePack pack) {
        return pack.items().stream()
                .filter(i -> i.kind() == EvidenceKind.REVIEWED_VERDICT)
                .map(EvidenceItem::id)
                .findFirst()
                .orElse("ev-1");
    }
}
