package com.dmi.domain;

import java.util.List;

public record ApiError(String code, String message, String traceId, List<String> details) {}
