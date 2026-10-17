package com.dmi.fixtures;

import com.dmi.api.AnalysisException;
import com.dmi.analysis.ScanCompleteness;
import com.dmi.analysis.SourceEvidenceScanner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static com.dmi.analysis.SourceEvidenceScanner.MAX_DEPTH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VisitDepthCapIntegrationTest {

    @Test
    void nestedDirsBeyondVisitDepthCapFailClosedViaPublicApis(@TempDir Path temp) throws Exception {
        Files.writeString(temp.resolve("pom.xml"), """
                <?xml version="1.0"?><project><modelVersion>4.0.0</modelVersion>
                <groupId>t</groupId><artifactId>a</artifactId><version>1</version></project>
                """);

        int maxVisit = MAX_DEPTH + FixtureBounds.DEFAULT_PROBE_VISIT_DEPTH_SLACK;
        Path current = temp;
        for (int i = 0; i < maxVisit + 1; i++) {
            current = current.resolve("d" + i);
            Files.createDirectories(current);
        }

        var probe = FixtureBounds.probeBeyondDepth(temp, MAX_DEPTH, path -> true);
        assertThat(probe.status()).isEqualTo(FixtureBounds.DepthProbeStatus.METADATA_BUDGET_EXCEEDED);
        assertThat(probe.detail()).contains("visit-depth-cap");

        SourceEvidenceScanner scanner = new SourceEvidenceScanner();
        var scan = scanner.scan(temp, java.util.List.of());
        assertThat(scan.completeness()).isEqualTo(ScanCompleteness.PARTIAL);
        assertThat(scan.diagnostics()).anyMatch(d -> "METADATA_BUDGET_EXCEEDED".equals(d.code()));

        FixtureRegistry registry = new FixtureRegistry("/workspace/fixtures");
        assertThatThrownBy(() -> registry.digest(temp))
                .isInstanceOf(AnalysisException.class)
                .extracting(e -> ((AnalysisException) e).getCode())
                .isEqualTo("FIXTURE_METADATA_BUDGET_EXCEEDED");
    }
}
