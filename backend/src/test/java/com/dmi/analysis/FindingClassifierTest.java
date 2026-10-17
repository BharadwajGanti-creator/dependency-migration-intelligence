package com.dmi.analysis;

import com.dmi.catalogue.AdvisoryCatalogue;
import com.dmi.domain.Confidence;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FindingClassifierTest {

    private final FindingClassifier classifier = new FindingClassifier();

    @Test
    void usesExactPatternKeyNotExcerptReMatch() {
        var entry = new AdvisoryCatalogue.CatalogueEntry(
                "rule-a",
                "summary",
                List.of("javax\\.servlet"),
                List.of(),
                List.of());
        SourceEvidenceScanner.ScanMatch match = new SourceEvidenceScanner.ScanMatch(0, "javax\\.servlet", "src/App.java", 1, "javax.servlet", false);
        ScanResult complete = new ScanResult(List.of(match), ScanCompleteness.COMPLETE, List.of());
        var findings = classifier.classify(List.of(entry), complete);
        assertThat(findings.get(0).confidence()).isEqualTo(Confidence.CONFIRMED_STATIC);

        SourceEvidenceScanner.ScanMatch wrongKey = new SourceEvidenceScanner.ScanMatch(0, "other-pattern", "src/App.java", 1, "javax.servlet", false);
        ScanResult wrong = new ScanResult(List.of(wrongKey), ScanCompleteness.COMPLETE, List.of());
        findings = classifier.classify(List.of(entry), wrong);
        assertThat(findings.get(0).confidence()).isEqualTo(Confidence.NOT_ASSESSED);
    }

    @Test
    void partialScanWordingForNotAssessed() {
        var entry = new AdvisoryCatalogue.CatalogueEntry(
                "rule-b",
                "summary",
                List.of("foo"),
                List.of(),
                List.of());
        ScanResult partial = new ScanResult(List.of(), ScanCompleteness.PARTIAL, List.of(new ScanDiagnostic("FILE_LIMIT_REACHED", "")));
        var finding = classifier.classify(List.of(entry), partial).get(0);
        assertThat(finding.confidence()).isEqualTo(Confidence.NOT_ASSESSED);
        assertThat(finding.rationale()).contains("PARTIAL");
    }
}
