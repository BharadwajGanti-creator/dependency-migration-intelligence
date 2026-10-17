package com.dmi.domain;

import java.util.List;

public record ExplanationClaim(String text, List<String> evidenceIds) {}
