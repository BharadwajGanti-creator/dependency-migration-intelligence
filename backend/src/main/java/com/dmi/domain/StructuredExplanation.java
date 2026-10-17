package com.dmi.domain;

import java.util.List;

/** Provider output before verification (also used for deterministic test adapter). */
public record StructuredExplanation(
        String findingId,
        String summary,
        List<ExplanationClaim> claims,
        List<String> nextChecks,
        List<String> limitations) {}
