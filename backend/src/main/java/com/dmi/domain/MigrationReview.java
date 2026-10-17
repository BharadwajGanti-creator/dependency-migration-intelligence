package com.dmi.domain;

import java.util.List;

/**
 * Human-reviewed migration consequence for a catalogue finding. Independent of {@link Confidence}
 * (static pattern detection).
 */
public record MigrationReview(
        MigrationVerdict verdict,
        OpenRewriteComparisonStatus openRewriteComparison,
        String evidenceProves,
        List<MigrationEvidenceRef> evidence,
        String testedScenario,
        String openRewriteRecipe,
        String openRewritePluginVersion,
        String openRewriteRecipeArtifactVersion,
        String reviewerNotes,
        String lastReviewedUtc,
        String nextHumanCheck) {}
