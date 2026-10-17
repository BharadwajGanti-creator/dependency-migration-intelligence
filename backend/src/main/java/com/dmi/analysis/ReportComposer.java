package com.dmi.analysis;

import com.dmi.domain.AnalysisReport;
import com.dmi.domain.AnalysisRequest;
import com.dmi.domain.DependencyCoordinate;
import com.dmi.domain.Finding;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class ReportComposer {

    private ReportComposer() {}

    public static AnalysisReport compose(
            AnalysisRequest request,
            String fixtureDigest,
            DependencyCoordinate before,
            DependencyCoordinate after,
            List<Finding> findings,
            List<String> limitations) {
        return new AnalysisReport(
                UUID.randomUUID().toString(),
                fixtureDigest,
                Instant.now(),
                before,
                after,
                findings,
                limitations,
                "docs/baseline/openrewrite-comparison.md");
    }

    public static List<String> defaultLimitations() {
        List<String> limits = new ArrayList<>();
        limits.add("Analysis is static only; Maven was not executed and runtime compatibility is not verified.");
        limits.add("Only direct declared parent/version coordinates are evaluated; transitive resolution is not performed.");
        limits.add("Findings cite curated excerpts; always confirm against official release notes before merging.");
        return limits;
    }
}
