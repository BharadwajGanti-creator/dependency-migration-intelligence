package com.dmi.explanation;

import com.dmi.domain.StructuredExplanation;

import java.util.Optional;

/** Provider-neutral boundary for optional AI explanations. */
public interface ExplanationModel {

    boolean isAvailable();

    StructuredExplanation generate(EvidencePack pack) throws ExplanationModelException;

    Optional<Long> lastLatencyMs();

    class ExplanationModelException extends Exception {
        public ExplanationModelException(String message) {
            super(message);
        }
    }
}
