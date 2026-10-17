package com.dmi.analysis;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class SourceEvidenceScannerTest {

    private final SourceEvidenceScanner scanner = new SourceEvidenceScanner();

    private static PatternDescriptor pat(int i, String key) {
        return new PatternDescriptor(i, key, Pattern.compile(key));
    }

    @Test
    void detectsPatternWithLineNumber(@TempDir Path temp) throws Exception {
        Path src = temp.resolve("src/main/java/com/example/App.java");
        Files.createDirectories(src.getParent());
        Files.writeString(src, "import javax.servlet.http.HttpServletRequest;\n");
        ScanResult result = scanner.scan(temp, List.of(pat(0, "javax\\.servlet")));
        assertThat(result.matches()).hasSize(1);
        assertThat(result.matches().get(0).line()).isEqualTo(1);
        assertThat(result.completeness()).isEqualTo(ScanCompleteness.COMPLETE);
    }

    @Test
    void skipsOversizedFileWithPartial(@TempDir Path temp) throws Exception {
        Path src = temp.resolve("src/main/java/Big.java");
        Files.createDirectories(src.getParent());
        byte[] big = new byte[SourceEvidenceScanner.MAX_FILE_BYTES + 1];
        Files.write(src, big);
        ScanResult result = scanner.scan(temp, List.of(pat(0, "javax")));
        assertThat(result.matches()).isEmpty();
        assertThat(result.completeness()).isEqualTo(ScanCompleteness.PARTIAL);
        assertThat(result.diagnostics()).anyMatch(d -> d.code().equals("OVERSIZED_FILE_SKIPPED"));
    }

    @Test
    void crlfAndUnicodeStableLineNumbers(@TempDir Path temp) throws Exception {
        Path src = temp.resolve("src/main/java/com/example/Uni.java");
        Files.createDirectories(src.getParent());
        Files.writeString(src, "line1\r\n// café javax.servlet\r\nline3\n");
        ScanResult result = scanner.scan(temp, List.of(pat(0, "javax\\.servlet")));
        assertThat(result.matches()).hasSize(1);
        assertThat(result.matches().get(0).line()).isEqualTo(2);
    }

    @Test
    void invalidUtf8MarksPartial(@TempDir Path temp) throws Exception {
        Path src = temp.resolve("src/main/java/Bad.java");
        Files.createDirectories(src.getParent());
        Files.write(src, new byte[] {(byte) 0xFF, (byte) 0xFE, 0x0A});
        ScanResult result = scanner.scan(temp, List.of(pat(0, "x")));
        assertThat(result.completeness()).isEqualTo(ScanCompleteness.PARTIAL);
        assertThat(result.diagnostics()).anyMatch(d -> d.code().equals("INVALID_ENCODING"));
    }

    @Test
    void depthBeyondLimitIsPartial(@TempDir Path temp) throws Exception {
        Path deep = temp.resolve("src/a/b/c/d/e/f/g/h/i/j/k/l/m/Deep.java");
        Files.createDirectories(deep.getParent());
        Files.writeString(deep, "javax.servlet.X\n");
        ScanResult result = scanner.scan(temp, List.of(pat(0, "javax\\.servlet")));
        assertThat(result.completeness()).isEqualTo(ScanCompleteness.PARTIAL);
        assertThat(result.diagnostics()).anyMatch(d -> d.code().equals("DEPTH_LIMIT_REACHED"));
    }

    @Test
    void irrelevantExcerptDoesNotMatchWrongPattern(@TempDir Path temp) throws Exception {
        Path src = temp.resolve("src/main/java/App.java");
        Files.createDirectories(src.getParent());
        Files.writeString(src, "totally unrelated\n");
        ScanResult result = scanner.scan(temp, List.of(pat(0, "javax\\.servlet")));
        assertThat(result.matches()).isEmpty();
    }
}
