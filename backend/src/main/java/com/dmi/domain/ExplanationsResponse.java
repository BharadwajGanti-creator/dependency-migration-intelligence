package com.dmi.domain;

import java.util.List;

public record ExplanationsResponse(AnalysisReport report, List<FindingExplanation> explanations) {}
