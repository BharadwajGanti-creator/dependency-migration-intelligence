package com.dmi.fixtures;

import com.dmi.api.AnalysisException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static com.dmi.analysis.SourceEvidenceScanner.MAX_DEPTH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FixtureRegistryDigestTest {

    private final FixtureRegistry registry = new FixtureRegistry("../fixtures");

    @Test
    void digestStableForBundledFixture() {
        Path fixture = Path.of("../fixtures/spring-migration-case").toAbsolutePath().normalize();
        String first = registry.digest(fixture).digest();
        String second = registry.digest(fixture).digest();
        assertThat(first).isEqualTo(second);
        assertThat(first).isNotBlank();
    }

    @Test
    void digestChangesWhenIncludedFileChanges(@TempDir Path temp) throws Exception {
        Files.writeString(temp.resolve("pom.xml"), minimalPom());
        Files.createDirectories(temp.resolve("src/main/java"));
        Path java = temp.resolve("src/main/java/App.java");
        Files.writeString(java, "class App {}\n");
        FixtureRegistry reg = new FixtureRegistry("/workspace/fixtures");
        String d1 = reg.digest(temp).digest();

        Files.writeString(temp.resolve("note.txt"), "change");
        String d2 = reg.digest(temp).digest();
        assertThat(d1).isNotEqualTo(d2);
    }

    @Test
    void failsClosedWhenFileBeyondMaxDepth(@TempDir Path temp) throws Exception {
        Files.writeString(temp.resolve("pom.xml"), minimalPom());
        Path deep = temp.resolve("a/b/c/d/e/f/g/h/i/j/k/l/m/deep.txt");
        Files.createDirectories(deep.getParent());
        Files.writeString(deep, "too deep");
        assertThatThrownBy(() -> new FixtureRegistry("/workspace/fixtures").digest(temp))
                .isInstanceOf(AnalysisException.class)
                .extracting(e -> ((AnalysisException) e).getCode())
                .isEqualTo("FIXTURE_DEPTH_EXCEEDED");
    }

    @Test
    void depthProbeDetectsAllowlistedBeyondMaxDepth(@TempDir Path temp) throws Exception {
        Path deepJava = temp.resolve("src/a/b/c/d/e/f/g/h/i/j/k/l/m/Deep.java");
        Files.createDirectories(deepJava.getParent());
        Files.writeString(deepJava, "javax.servlet.X\n");
        var scanner = new com.dmi.analysis.SourceEvidenceScanner();
        var result = scanner.scan(temp, java.util.List.of(new com.dmi.analysis.PatternDescriptor(0, "javax\\.servlet", java.util.regex.Pattern.compile("javax\\.servlet"))));
        assertThat(result.completeness()).isEqualTo(com.dmi.analysis.ScanCompleteness.PARTIAL);
        assertThat(result.diagnostics()).anyMatch(d -> d.code().equals("DEPTH_LIMIT_REACHED"));
    }

    private static String minimalPom() {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <project><modelVersion>4.0.0</modelVersion>
                <groupId>t</groupId><artifactId>a</artifactId><version>1</version></project>
                """;
    }
}
