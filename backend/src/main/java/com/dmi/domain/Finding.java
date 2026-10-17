package com.dmi.domain;

import java.util.List;

public record Finding(
        String id,
        String category,
        Confidence confidence,
        String summary,
        List<SourceLocation> sourceLocations,
        List<DocumentationRef> documentation,
        String rationale,
        List<String> proposedChecks,
        MigrationReview migrationReview) {}
