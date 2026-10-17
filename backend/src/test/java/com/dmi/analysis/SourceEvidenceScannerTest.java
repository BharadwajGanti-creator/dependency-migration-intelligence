package com.dmi.analysis;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class SourceEvidenceScannerTest {

    private final SourceEvidenceScanner scanner = new SourceEvidenceScanner();

    @Test
    void detectsPatternWithLineNumber(@TempDir Path temp) throws Exception {
        Path src = temp.resolve("src/main/java/com/example/App.java");
        Files.createDirectories(src.getParent());
        Files.writeString(src, "import javax.servlet.http.HttpServletRequest;\n");
        List<SourceEvidenceScanner.ScanMatch> matches = scanner.scan(temp, List.of(Pattern.compile("javax\\.servlet")));
        assertThat(matches).hasSize(1);
        assertThat(matches.get(0).line()).isEqualTo(1);
    }

    @Test
    void skipsOversizedFile(@TempDir Path temp) throws Exception {
        Path src = temp.resolve("src/main/java/Big.java");
        Files.createDirectories(src.getParent());
        byte[] big = new byte[SourceEvidenceScanner.MAX_FILE_BYTES + 1];
        Files.write(src, big);
        List<SourceEvidenceScanner.ScanMatch> matches = scanner.scan(temp, List.of(Pattern.compile("javax")));
        assertThat(matches).isEmpty();
    }
}
