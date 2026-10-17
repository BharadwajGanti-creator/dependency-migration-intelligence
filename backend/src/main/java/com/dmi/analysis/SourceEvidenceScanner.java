package com.dmi.analysis;

import com.dmi.domain.SourceLocation;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class SourceEvidenceScanner {

    public static final int MAX_FILES = 200;
    public static final int MAX_DEPTH = 12;
    public static final int MAX_FILE_BYTES = 256 * 1024;
    public static final int MAX_TOTAL_BYTES = 5 * 1024 * 1024;

    public List<ScanMatch> scan(Path fixtureRoot, List<Pattern> patterns) {
        List<ScanMatch> matches = new ArrayList<>();
        int filesScanned = 0;
        long totalBytes = 0;
        List<Path> files;
        try {
            files = Files.walk(fixtureRoot, MAX_DEPTH)
                    .filter(Files::isRegularFile)
                    .filter(this::isAllowlisted)
                    .sorted(Comparator.naturalOrder())
                    .toList();
        } catch (IOException e) {
            return matches;
        }
        for (Path file : files) {
            if (filesScanned >= MAX_FILES) {
                break;
            }
            if (isSymlinkEscape(fixtureRoot, file)) {
                continue;
            }
            long size;
            try {
                size = Files.size(file);
            } catch (IOException e) {
                continue;
            }
            if (size > MAX_FILE_BYTES) {
                continue;
            }
            if (totalBytes + size > MAX_TOTAL_BYTES) {
                break;
            }
            totalBytes += size;
            filesScanned++;
            String relative = fixtureRoot.relativize(file).toString().replace('\\', '/');
            String content;
            try {
                content = Files.readString(file, StandardCharsets.UTF_8);
            } catch (IOException e) {
                continue;
            }
            String[] lines = content.split("\\R", -1);
            for (int lineNum = 0; lineNum < lines.length; lineNum++) {
                String line = lines[lineNum];
                for (Pattern pattern : patterns) {
                    Matcher matcher = pattern.matcher(line);
                    if (matcher.find()) {
                        matches.add(new ScanMatch(pattern.pattern(), relative, lineNum + 1, line.trim()));
                    }
                }
            }
        }
        matches.sort(Comparator.comparing(ScanMatch::relativePath).thenComparingInt(ScanMatch::line));
        return matches;
    }

    public List<SourceLocation> toLocations(List<ScanMatch> matches, String patternKey) {
        return matches.stream()
                .filter(m -> m.pattern().equals(patternKey))
                .map(m -> new SourceLocation(m.relativePath(), m.line()))
                .distinct()
                .sorted(Comparator.comparing(SourceLocation::path).thenComparingInt(SourceLocation::line))
                .toList();
    }

    private boolean isAllowlisted(Path path) {
        String name = path.getFileName().toString();
        if (name.equals("pom.xml")) {
            return true;
        }
        if (name.endsWith(".java") || name.endsWith(".properties") || name.endsWith(".yml") || name.endsWith(".yaml")) {
            return path.toString().contains("src" + path.getFileSystem().getSeparator());
        }
        return false;
    }

    private boolean isSymlinkEscape(Path root, Path file) {
        try {
            Path normalized = file.toRealPath().normalize();
            return !normalized.startsWith(root.toRealPath().normalize());
        } catch (IOException e) {
            return true;
        }
    }

    public record ScanMatch(String pattern, String relativePath, int line, String excerpt) {}
}
