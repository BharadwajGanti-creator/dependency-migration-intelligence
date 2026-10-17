package com.dmi.explanation;

import com.dmi.domain.ExplanationClaim;
import com.dmi.domain.StructuredExplanation;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Test-only provider that emits verifier-invalid output (e.g. mandatory pathmatch action).
 */
@Component
@ConditionalOnProperty(name = "dmi.explanation.provider", havingValue = "invalid-output-test")
public class InvalidOutputTestExplanationModel implements ExplanationModel {

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public StructuredExplanation generate(EvidencePack pack) {
        return new StructuredExplanation(
                pack.findingId(),
                "Mandatory migration: action required immediately.",
                List.of(new ExplanationClaim(
                        "You must change pathmatch; action required for ant_path_matcher.",
                        List.of("ev-4"))),
                List.of(),
                List.of());
    }

    @Override
    public Optional<Long> lastLatencyMs() {
        return Optional.of(0L);
    }
}
