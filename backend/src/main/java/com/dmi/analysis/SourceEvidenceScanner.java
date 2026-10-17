package com.dmi.analysis;

import com.dmi.domain.SourceLocation;
import com.dmi.fixtures.FixtureBounds;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class SourceEvidenceScanner {

    public static final int MAX_FILES = 200;
    public static final int MAX_DEPTH = 12;
    public static final int MAX_FILE_BYTES = 256 * 1024;
    public static final int MAX_TOTAL_BYTES = 5 * 1024 * 1024;

    public ScanResult scan(Path fixtureRoot, List<PatternDescriptor> patterns) {
        List<ScanMatch> matches = new ArrayList<>();
        List<ScanDiagnostic> diagnostics = new ArrayList<>();
        AtomicBoolean partial = new AtomicBoolean(false);

        Path normalizedRoot = fixtureRoot.toAbsolutePath().normalize();
        markDeepAllowlistedFiles(normalizedRoot, partial, diagnostics);

        List<Path> eligible = collectEligibleFiles(normalizedRoot, partial, diagnostics);
        int filesScanned = 0;
        long totalBytes = 0;
        for (Path file : eligible) {
            if (filesScanned >= MAX_FILES) {
                partial.set(true);
                diagnostics.add(new ScanDiagnostic("FILE_LIMIT_REACHED", "maxFiles=" + MAX_FILES));
                break;
            }
            if (Files.isSymbolicLink(file)) {
                partial.set(true);
                diagnostics.add(new ScanDiagnostic("SYMLINK_SKIPPED", rel(normalizedRoot, file)));
                continue;
            }
            long size;
            try {
                size = Files.size(file);
            } catch (IOException e) {
                partial.set(true);
                diagnostics.add(new ScanDiagnostic("UNREADABLE_FILE", rel(normalizedRoot, file)));
                continue;
            }
            if (size > MAX_FILE_BYTES) {
                partial.set(true);
                diagnostics.add(new ScanDiagnostic("OVERSIZED_FILE_SKIPPED", rel(normalizedRoot, file)));
                continue;
            }
            if (totalBytes + size > MAX_TOTAL_BYTES) {
                partial.set(true);
                diagnostics.add(new ScanDiagnostic("BYTE_LIMIT_REACHED", "maxTotalBytes=" + MAX_TOTAL_BYTES));
                break;
            }
            String relative = rel(normalizedRoot, file);
            byte[] bytes;
            try {
                bytes = Files.readAllBytes(file);
            } catch (IOException e) {
                partial.set(true);
                diagnostics.add(new ScanDiagnostic("UNREADABLE_FILE", relative));
                continue;
            }
            if (!isValidUtf8(bytes)) {
                partial.set(true);
                diagnostics.add(new ScanDiagnostic("INVALID_ENCODING", relative));
                continue;
            }
            totalBytes += size;
            filesScanned++;
            String content = new String(bytes, StandardCharsets.UTF_8);
            String[] lines = content.split("\\R", -1);
            for (int lineNum = 0; lineNum < lines.length; lineNum++) {
                String line = lines[lineNum];
                boolean commentOnly = isCommentOnlyLine(line, relative);
                for (PatternDescriptor descriptor : patterns) {
                    if (descriptor.compiled().matcher(line).find()) {
                        matches.add(new ScanMatch(
                                descriptor.index(),
                                descriptor.patternKey(),
                                relative,
                                lineNum + 1,
                                line.trim(),
                                commentOnly));
                    }
                }
            }
        }

        matches.sort(Comparator.comparing(ScanMatch::relativePath).thenComparingInt(ScanMatch::line).thenComparingInt(ScanMatch::patternIndex));
        ScanCompleteness completeness = partial.get() ? ScanCompleteness.PARTIAL : ScanCompleteness.COMPLETE;
        return new ScanResult(List.copyOf(matches), completeness, List.copyOf(diagnostics));
    }

    private void markDeepAllowlistedFiles(Path root, AtomicBoolean partial, List<ScanDiagnostic> diagnostics) {
        try {
            if (FixtureBounds.hasMatchingFileBeyondDepth(root, MAX_DEPTH, this::isAllowlisted)) {
                partial.set(true);
                diagnostics.add(new ScanDiagnostic("DEPTH_LIMIT_REACHED", "eligible files exist beyond maxDepth=" + MAX_DEPTH));
            }
        } catch (IOException e) {
            partial.set(true);
            diagnostics.add(new ScanDiagnostic("WALK_FAILED", "depth-probe"));
        }
    }

    private List<Path> collectEligibleFiles(Path root, AtomicBoolean partial, List<ScanDiagnostic> diagnostics) {
        List<Path> files = new ArrayList<>();
        try {
            Files.walkFileTree(root, java.util.EnumSet.noneOf(java.nio.file.FileVisitOption.class), MAX_DEPTH, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (Files.isSymbolicLink(file)) {
                        partial.set(true);
                        diagnostics.add(new ScanDiagnostic("SYMLINK_SKIPPED", rel(root, file)));
                        return FileVisitResult.CONTINUE;
                    }
                    if (isAllowlisted(file) && attrs.isRegularFile()) {
                        files.add(file);
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    partial.set(true);
                    diagnostics.add(new ScanDiagnostic("WALK_FAILED", rel(root, file)));
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            partial.set(true);
            diagnostics.add(new ScanDiagnostic("WALK_FAILED", "fixture-walk"));
        }
        files.sort(Comparator.naturalOrder());
        return files;
    }

    static boolean isCommentOnlyLine(String line, String relativePath) {
        String trimmed = line.trim();
        if (trimmed.isEmpty()) {
            return true;
        }
        if (relativePath.endsWith(".properties") || relativePath.endsWith(".yml") || relativePath.endsWith(".yaml")) {
            return trimmed.startsWith("#");
        }
        if (relativePath.endsWith(".java")) {
            return trimmed.startsWith("//") || trimmed.startsWith("*") || trimmed.startsWith("/*");
        }
        return false;
    }

    private boolean isValidUtf8(byte[] bytes) {
        CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        try {
            decoder.decode(ByteBuffer.wrap(bytes));
            return true;
        } catch (CharacterCodingException e) {
            return false;
        }
    }

    public List<SourceLocation> toLocations(List<ScanMatch> matches, String patternKey) {
        return matches.stream()
                .filter(m -> m.patternKey().equals(patternKey))
                .map(m -> new SourceLocation(m.relativePath(), m.line()))
                .distinct()
                .sorted(Comparator.comparing(SourceLocation::path).thenComparingInt(SourceLocation::line))
                .toList();
    }

    boolean isAllowlisted(Path path) {
        String name = path.getFileName().toString();
        if (name.equals("pom.xml")) {
            return true;
        }
        if (name.endsWith(".java") || name.endsWith(".properties") || name.endsWith(".yml") || name.endsWith(".yaml")) {
            return path.toString().contains("src" + path.getFileSystem().getSeparator());
        }
        return false;
    }

    private String rel(Path root, Path file) {
        return root.relativize(file).toString().replace('\\', '/');
    }

    public record ScanMatch(int patternIndex, String patternKey, String relativePath, int line, String excerpt, boolean commentOnly) {}
}
