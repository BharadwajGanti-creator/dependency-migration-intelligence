package com.dmi.domain;

public record ExplanationModelMetadata(
        String provider,
        String modelId,
        boolean aiAssisted,
        String experimentStatus) {}
