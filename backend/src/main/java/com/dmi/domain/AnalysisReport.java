package com.dmi.domain;

import java.time.Instant;
import java.util.List;

public record AnalysisReport(
        String requestId,
        String fixtureDigest,
        Instant generatedAtUtc,
        DependencyCoordinate before,
        DependencyCoordinate after,
        List<Finding> findings,
        List<String> limitations,
        String baselineComparisonRef) {}
