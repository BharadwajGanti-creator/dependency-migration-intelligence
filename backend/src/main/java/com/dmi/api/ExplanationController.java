package com.dmi.api;

import com.dmi.domain.ExplanationRequest;
import com.dmi.domain.ExplanationsResponse;
import com.dmi.explanation.ExplanationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analyses")
public class ExplanationController {

    private final ExplanationService explanationService;
    private final RequestValidator requestValidator;

    public ExplanationController(ExplanationService explanationService, RequestValidator requestValidator) {
        this.explanationService = explanationService;
        this.requestValidator = requestValidator;
    }

    @PostMapping("/explanations")
    public ResponseEntity<ExplanationsResponse> explain(@Valid @RequestBody ExplanationRequest request) {
        requestValidator.validate(request.analysis());
        requestValidator.validateFindingIds(request.findingIds());
        return ResponseEntity.ok(explanationService.explain(request));
    }
}
