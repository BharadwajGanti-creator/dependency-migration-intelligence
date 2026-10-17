package com.dmi.domain;

/** Outcome of optional AI-assisted explanation for one finding. */
public enum ExplanationStatus {
    ACCEPTED,
    REJECTED,
    DISABLED,
    FALLBACK,
    TIMEOUT
}
