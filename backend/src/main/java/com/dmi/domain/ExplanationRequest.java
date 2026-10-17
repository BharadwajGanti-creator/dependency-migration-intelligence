package com.dmi.domain;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ExplanationRequest(
        @NotNull @Valid AnalysisRequest analysis,
        @NotEmpty @Size(max = 16) List<@NotNull @Size(max = 128) String> findingIds) {}
