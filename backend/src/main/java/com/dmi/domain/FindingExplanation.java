package com.dmi.domain;

import java.util.List;

public record FindingExplanation(
        String findingId,
        String evidencePackId,
        ExplanationStatus status,
        String summary,
        List<ExplanationClaim> claims,
        List<String> nextChecks,
        List<String> limitations,
        ExplanationModelMetadata modelMetadata,
        MigrationReview deterministicReview) {}
