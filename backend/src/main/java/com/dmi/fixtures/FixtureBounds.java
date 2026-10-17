package com.dmi.fixtures;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.stream.Stream;
import java.util.function.Predicate;

public final class FixtureBounds {

    public static final int DEFAULT_METADATA_MAX_ENTRIES = 4096;
    public static final long DEFAULT_METADATA_MAX_MILLIS = 1500;
    /** How far below fixture root metadata probe may descend while searching for over-depth files. */
    public static final int DEFAULT_PROBE_VISIT_DEPTH_SLACK = 6;

    private FixtureBounds() {}

    public static int relativeDepth(Path fixtureRoot, Path file) {
        return fixtureRoot.relativize(file.normalize()).getNameCount();
    }

    public enum DepthProbeStatus {
        /** No included regular file found deeper than maxDepth within probe limits. */
        CLEAR,
        /** Included file exists deeper than maxDepth — stop immediately. */
        DEEP_FILE_FOUND,
        /** Entry/time/visit-depth budget exhausted before coverage could be attested. */
        METADATA_BUDGET_EXCEEDED
    }

    public record DepthProbeResult(DepthProbeStatus status, String detail) {}

    public record ProbeLimits(int maxEntries, long maxMillis, int maxVisitDepth) {
        public static ProbeLimits defaults(int analysisMaxDepth) {
            return new ProbeLimits(
                    DEFAULT_METADATA_MAX_ENTRIES,
                    DEFAULT_METADATA_MAX_MILLIS,
                    analysisMaxDepth + DEFAULT_PROBE_VISIT_DEPTH_SLACK);
        }
    }

    public static DepthProbeResult probeBeyondDepth(Path fixtureRoot, int maxDepth, Predicate<Path> include)
            throws IOException {
        return probeBeyondDepth(fixtureRoot, maxDepth, include, ProbeLimits.defaults(maxDepth));
    }

    public static DepthProbeResult probeBeyondDepth(
            Path fixtureRoot, int maxDepth, Predicate<Path> include, ProbeLimits limits) throws IOException {
        Path root = fixtureRoot.toAbsolutePath().normalize();
        long started = System.nanoTime();
        int[] entries = {0};

        DepthProbeResult[] early = {null};

        Files.walkFileTree(root, java.util.EnumSet.noneOf(java.nio.file.FileVisitOption.class), limits.maxVisitDepth(),
                new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                        if (dir.equals(root)) {
                            return FileVisitResult.CONTINUE;
                        }
                        if (Files.isSymbolicLink(dir)) {
                            return FileVisitResult.SKIP_SUBTREE;
                        }
                        entries[0]++;
                        if (budgetExhausted(entries[0], started, limits)) {
                            early[0] = new DepthProbeResult(DepthProbeStatus.METADATA_BUDGET_EXCEEDED, "directory-entry-budget");
                            return FileVisitResult.TERMINATE;
                        }
                        int depth = relativeDepth(root, dir);
                        if (depth == limits.maxVisitDepth()) {
                            try {
                                if (hasNonSymlinkSubdirectory(dir)) {
                                    early[0] = new DepthProbeResult(
                                            DepthProbeStatus.METADATA_BUDGET_EXCEEDED, "visit-depth-cap");
                                    return FileVisitResult.TERMINATE;
                                }
                            } catch (IOException e) {
                                early[0] = new DepthProbeResult(
                                        DepthProbeStatus.METADATA_BUDGET_EXCEEDED, "visit-depth-list-failed");
                                return FileVisitResult.TERMINATE;
                            }
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                        if (Files.isSymbolicLink(file)) {
                            return FileVisitResult.CONTINUE;
                        }
                        if (!attrs.isRegularFile()) {
                            return FileVisitResult.CONTINUE;
                        }
                        entries[0]++;
                        if (budgetExhausted(entries[0], started, limits)) {
                            early[0] = new DepthProbeResult(DepthProbeStatus.METADATA_BUDGET_EXCEEDED, "file-entry-budget");
                            return FileVisitResult.TERMINATE;
                        }
                        if (include.test(file) && relativeDepth(root, file) > maxDepth) {
                            early[0] = new DepthProbeResult(
                                    DepthProbeStatus.DEEP_FILE_FOUND,
                                    root.relativize(file).toString().replace('\\', '/'));
                            return FileVisitResult.TERMINATE;
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(Path file, IOException exc) {
                        early[0] = new DepthProbeResult(DepthProbeStatus.METADATA_BUDGET_EXCEEDED, "walk-failed");
                        return FileVisitResult.TERMINATE;
                    }
                });

        if (early[0] != null) {
            return early[0];
        }
        if (hasDirectoryBeyondVisitDepth(root, limits.maxVisitDepth())) {
            return new DepthProbeResult(DepthProbeStatus.METADATA_BUDGET_EXCEEDED, "visit-depth-cap");
        }
        return new DepthProbeResult(DepthProbeStatus.CLEAR, "");
    }

    /** Bounded one-level-beyond check; fail closed if deeper directories exist. */
    private static boolean hasDirectoryBeyondVisitDepth(Path root, int maxVisitDepth) throws IOException {
        try (Stream<Path> paths = Files.walk(root, maxVisitDepth + 1)) {
            return paths.filter(Files::isDirectory)
                    .filter(p -> !Files.isSymbolicLink(p))
                    .anyMatch(p -> relativeDepth(root, p) > maxVisitDepth);
        }
    }

    private static boolean hasNonSymlinkSubdirectory(Path dir) throws IOException {
        try (Stream<Path> children = Files.list(dir)) {
            return children.anyMatch(p -> Files.isDirectory(p) && !Files.isSymbolicLink(p));
        }
    }

    private static boolean budgetExhausted(int entries, long startedNanos, ProbeLimits limits) {
        if (entries > limits.maxEntries()) {
            return true;
        }
        long elapsedMs = (System.nanoTime() - startedNanos) / 1_000_000;
        return elapsedMs > limits.maxMillis();
    }
}
