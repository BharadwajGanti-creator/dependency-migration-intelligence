package com.dmi.validation;

import com.dmi.domain.Finding;
import com.dmi.domain.MigrationReview;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MigrationReviewEnricher {

    private final AdjudicationRegistry adjudicationRegistry;

    public MigrationReviewEnricher(AdjudicationRegistry adjudicationRegistry) {
        this.adjudicationRegistry = adjudicationRegistry;
    }

    public List<Finding> enrich(List<Finding> findings) {
        return findings.stream().map(this::attachReview).toList();
    }

    public MigrationReview reviewFor(String findingId) {
        return adjudicationRegistry.resolve(findingId);
    }

    private Finding attachReview(Finding finding) {
        MigrationReview review = adjudicationRegistry.resolve(finding.id());
        return new Finding(
                finding.id(),
                finding.category(),
                finding.confidence(),
                finding.summary(),
                finding.sourceLocations(),
                finding.documentation(),
                finding.rationale(),
                finding.proposedChecks(),
                review);
    }
}
