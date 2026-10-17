package com.dmi.api;

import com.dmi.analysis.AnalysisService;
import com.dmi.domain.AnalysisReport;
import com.dmi.domain.AnalysisRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analyses")
public class AnalysisController {

    private final AnalysisService analysisService;
    private final RequestValidator requestValidator;

    public AnalysisController(AnalysisService analysisService, RequestValidator requestValidator) {
        this.analysisService = analysisService;
        this.requestValidator = requestValidator;
    }

    @PostMapping("/sync")
    public ResponseEntity<AnalysisReport> analyzeSync(@Valid @RequestBody AnalysisRequest request) {
        requestValidator.validate(request);
        return ResponseEntity.ok(analysisService.analyze(request));
    }
}
