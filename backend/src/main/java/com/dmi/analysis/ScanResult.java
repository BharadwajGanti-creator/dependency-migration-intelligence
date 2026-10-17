package com.dmi.analysis;

import java.util.List;

public record ScanResult(
        List<SourceEvidenceScanner.ScanMatch> matches,
        ScanCompleteness completeness,
        List<ScanDiagnostic> diagnostics) {}
