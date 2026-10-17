package com.dmi.explanation;

import com.dmi.domain.StructuredExplanation;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@ConditionalOnProperty(name = "dmi.explanation.provider", havingValue = "disabled", matchIfMissing = true)
public class DisabledExplanationModel implements ExplanationModel {

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public StructuredExplanation generate(EvidencePack pack) {
        throw new UnsupportedOperationException("Explanation provider is disabled");
    }

    @Override
    public Optional<Long> lastLatencyMs() {
        return Optional.empty();
    }
}
