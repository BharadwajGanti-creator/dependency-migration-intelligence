package com.dmi.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AnalysisRequest(
        @NotBlank @Size(max = 64) String fixtureId,
        @NotBlank @Size(max = 128) String dependencyGroup,
        @NotBlank @Size(max = 128) String dependencyArtifact,
        @NotBlank @Size(max = 64) String fromVersion,
        @NotBlank @Size(max = 64) String toVersion) {}
