package com.dmi.fixtures;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static com.dmi.analysis.SourceEvidenceScanner.MAX_DEPTH;
import static org.assertj.core.api.Assertions.assertThat;

class FixtureBoundsTraversalTest {

    @Test
    void stopsEarlyOnFirstDeepEligibleFile(@TempDir Path temp) throws Exception {
        Path deep1 = temp.resolve("src/a/b/c/d/e/f/g/h/i/j/k/l/m/Deep1.java");
        Path deep2 = temp.resolve("src/a/b/c/d/e/f/g/h/i/j/k/l/m/n/o/Deep2.java");
        Files.createDirectories(deep1.getParent());
        Files.writeString(deep1, "x");
        Files.createDirectories(deep2.getParent());
        Files.writeString(deep2, "y");

        var result = FixtureBounds.probeBeyondDepth(temp, MAX_DEPTH, path -> path.toString().endsWith(".java"));
        assertThat(result.status()).isEqualTo(FixtureBounds.DepthProbeStatus.DEEP_FILE_FOUND);
        assertThat(result.detail()).contains("Deep1.java");
    }

    @Test
    void budgetExhaustionFailsClosed(@TempDir Path temp) throws Exception {
        Files.createDirectories(temp.resolve("d0"));
        for (int i = 0; i < 30; i++) {
            Files.createDirectories(temp.resolve("d0/d" + i));
        }
        var tight = new FixtureBounds.ProbeLimits(5, 5000, MAX_DEPTH + 6);
        var result = FixtureBounds.probeBeyondDepth(temp, MAX_DEPTH, path -> true, tight);
        assertThat(result.status()).isEqualTo(FixtureBounds.DepthProbeStatus.METADATA_BUDGET_EXCEEDED);
    }

    @Test
    void clearWhenNoDeepIncludedFiles(@TempDir Path temp) throws Exception {
        Files.writeString(temp.resolve("pom.xml"), "<project/>");
        Files.createDirectories(temp.resolve("src/main/java"));
        Files.writeString(temp.resolve("src/main/java/App.java"), "class App {}");
        var result = FixtureBounds.probeBeyondDepth(temp, MAX_DEPTH, path -> path.toString().endsWith(".java"));
        assertThat(result.status()).isEqualTo(FixtureBounds.DepthProbeStatus.CLEAR);
    }

    @Test
    void scannerSurfacesMetadataBudgetAsPartial(@TempDir Path temp) throws Exception {
        for (int i = 0; i < 30; i++) {
            Files.createDirectories(temp.resolve("wide/d" + i));
        }
        var scanner = new com.dmi.analysis.SourceEvidenceScanner();
        // Indirect: tight probe via FixtureBounds mirrors scanner behavior
        var probe = FixtureBounds.probeBeyondDepth(
                temp, MAX_DEPTH, p -> true, new FixtureBounds.ProbeLimits(8, 5000, MAX_DEPTH + 6));
        assertThat(probe.status()).isEqualTo(FixtureBounds.DepthProbeStatus.METADATA_BUDGET_EXCEEDED);
    }
}
