package com.dmi.explanation;

import com.dmi.analysis.AnalysisService;
import com.dmi.api.AnalysisException;
import com.dmi.domain.AnalysisReport;
import com.dmi.domain.ExplanationModelMetadata;
import com.dmi.domain.ExplanationRequest;
import com.dmi.domain.ExplanationStatus;
import com.dmi.domain.ExplanationsResponse;
import com.dmi.domain.Finding;
import com.dmi.domain.FindingExplanation;
import com.dmi.domain.StructuredExplanation;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

@Service
public class ExplanationService {

    private final AnalysisService analysisService;
    private final EvidencePackBuilder evidencePackBuilder;
    private final ExplanationModel explanationModel;
    private final ExplanationVerifier explanationVerifier;
    private final ExplanationProperties properties;

    public ExplanationService(
            AnalysisService analysisService,
            EvidencePackBuilder evidencePackBuilder,
            ExplanationModel explanationModel,
            ExplanationVerifier explanationVerifier,
            ExplanationProperties properties) {
        this.analysisService = analysisService;
        this.evidencePackBuilder = evidencePackBuilder;
        this.explanationModel = explanationModel;
        this.explanationVerifier = explanationVerifier;
        this.properties = properties;
    }

    public ExplanationsResponse explain(ExplanationRequest request) {
        AnalysisReport report = analysisService.analyze(request.analysis());
        Map<String, Finding> byId = new LinkedHashMap<>();
        for (Finding finding : report.findings()) {
            byId.put(finding.id(), finding);
        }

        List<FindingExplanation> explanations = new ArrayList<>();
        for (String findingId : request.findingIds()) {
            Finding finding = byId.get(findingId);
            if (finding == null) {
                throw new AnalysisException(
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "UNKNOWN_FINDING",
                        "findingId not present in authoritative report: " + findingId);
            }
            EvidencePack pack = evidencePackBuilder.build(report, finding);
            explanations.add(resolveExplanation(finding, pack));
        }
        return new ExplanationsResponse(report, explanations);
    }

    private FindingExplanation resolveExplanation(Finding finding, EvidencePack pack) {
        if (!properties.enabled()) {
            return DeterministicFallbackBuilder.disabled(finding, pack);
        }
        if (!explanationModel.isAvailable()) {
            return DeterministicFallbackBuilder.fallback(finding, pack, "Explanation provider unavailable.");
        }

        try {
            StructuredExplanation generated = generateWithOptionalTimeout(pack);
            var verifierResult = explanationVerifier.verify(pack, finding.migrationReview(), generated);
            if (verifierResult.accepted()) {
                ExplanationModelMetadata metadata = new ExplanationModelMetadata(
                        properties.provider(),
                        properties.provider(),
                        !"deterministic-test".equals(properties.provider()),
                        "deterministic-test".equals(properties.provider()) ? "HARNESS_ONLY" : "NOT_RUN");
                return DeterministicFallbackBuilder.acceptedFromVerified(
                        finding,
                        pack,
                        generated.summary(),
                        generated.claims(),
                        metadata);
            }
            return DeterministicFallbackBuilder.rejected(
                    finding,
                    pack,
                    "Explanation rejected by verifier: " + String.join("; ", verifierResult.violations()));
        } catch (TimeoutException e) {
            return timeoutFallback(finding, pack);
        } catch (Exception e) {
            return DeterministicFallbackBuilder.fallback(
                    finding, pack, "Explanation generation failed: " + e.getMessage());
        }
    }

    private StructuredExplanation generateWithOptionalTimeout(EvidencePack pack)
            throws TimeoutException, ExecutionException, InterruptedException, ExplanationModel.ExplanationModelException {
        if (!properties.realInferenceEnabled()) {
            return explanationModel.generate(pack);
        }
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<StructuredExplanation> future = executor.submit(() -> explanationModel.generate(pack));
            return future.get(properties.timeoutMs(), TimeUnit.MILLISECONDS);
        } finally {
            executor.shutdownNow();
        }
    }

    private FindingExplanation timeoutFallback(Finding finding, EvidencePack pack) {
        FindingExplanation base = DeterministicFallbackBuilder.fallback(finding, pack, "Explanation timed out.");
        return new FindingExplanation(
                base.findingId(),
                base.evidencePackId(),
                ExplanationStatus.TIMEOUT,
                base.summary(),
                base.claims(),
                base.nextChecks(),
                base.limitations(),
                base.modelMetadata(),
                base.deterministicReview());
    }
}
