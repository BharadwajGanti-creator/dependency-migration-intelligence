package com.dmi.corpus;

import com.dmi.domain.AnalysisRequest;
import com.dmi.domain.AnalysisReport;
import com.dmi.domain.Confidence;
import com.dmi.domain.Finding;
import com.dmi.domain.MigrationReview;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

/**
 * Test-only seam: materializes corpus scenarios as the sole allowlisted runtime fixture id
 * ({@code spring-migration-case}) under a temporary fixtures root. Public {@code FixtureRegistry}
 * allowlist is not expanded.
 */
public final class ControlledCorpusSupport {

    public static final String RUNTIME_FIXTURE_ID = "spring-migration-case";
    public static final AnalysisRequest STANDARD_MIGRATION_REQUEST = new AnalysisRequest(
            RUNTIME_FIXTURE_ID,
            "org.springframework.boot",
            "spring-boot-starter-parent",
            "2.7.18",
            "3.2.12");

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ControlledCorpusSupport() {
    }

    public static JsonNode loadManifest() {
        try (InputStream in = ControlledCorpusSupport.class.getResourceAsStream("/corpus/manifest.json")) {
            if (in == null) {
                throw new IllegalStateException("Missing /corpus/manifest.json");
            }
            return MAPPER.readTree(in);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static Path materializeScenario(String scenarioDirRelative, Path fixturesRoot) throws IOException {
        Path source = locateScenarioRoot(scenarioDirRelative);
        Path target = fixturesRoot.resolve(RUNTIME_FIXTURE_ID);
        deleteRecursively(target);
        copyRecursively(source, target);
        return target;
    }

    static Path locateScenarioRoot(String scenarioDirRelative) {
        Path cwd = Path.of("").toAbsolutePath().normalize();
        for (Path base : List.of(
                cwd.resolve("src/test/resources/corpus"),
                cwd.resolve("backend/src/test/resources/corpus"),
                cwd.getParent() != null ? cwd.getParent().resolve("backend/src/test/resources/corpus") : cwd)) {
            Path candidate = base.resolve(scenarioDirRelative).normalize();
            if (Files.isDirectory(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Cannot locate corpus scenario: " + scenarioDirRelative);
    }

    public static String contentSha256(Path scenarioRoot) throws IOException {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
        List<Path> files;
        try (var walk = Files.walk(scenarioRoot)) {
            files = walk.filter(Files::isRegularFile).sorted(Comparator.naturalOrder()).toList();
        }
        for (Path file : files) {
            String relative = scenarioRoot.relativize(file).toString().replace('\\', '/');
            md.update(relative.getBytes());
            md.update((byte) 0);
            md.update(Files.readAllBytes(file));
        }
        return HexFormat.of().formatHex(md.digest());
    }

    public static Optional<Finding> finding(AnalysisReport report, String findingId) {
        return report.findings().stream().filter(f -> f.id().equals(findingId)).findFirst();
    }

    public static void assertExpectedOutcome(Finding finding, JsonNode expected) {
        String findingId = expected.get("findingId").asText();
        if (expected.hasNonNull("expectedConfidence")) {
            Confidence expectedConfidence = Confidence.valueOf(expected.get("expectedConfidence").asText());
            if (finding.confidence() != expectedConfidence) {
                throw new AssertionError(findingId + " confidence expected " + expectedConfidence + " but was " + finding.confidence());
            }
        }
        if (expected.has("mustNotBeConfidence")) {
            Confidence forbidden = Confidence.valueOf(expected.get("mustNotBeConfidence").asText());
            if (finding.confidence() == forbidden) {
                throw new AssertionError(findingId + " must not be " + forbidden + " but was");
            }
        }
        MigrationReview review = finding.migrationReview();
        if (expected.hasNonNull("expectedVerdict")) {
            if (review == null) {
                throw new AssertionError(findingId + " expected migrationReview verdict but review was null");
            }
            String expectedVerdict = expected.get("expectedVerdict").asText();
            if (!expectedVerdict.equals(review.verdict().name())) {
                throw new AssertionError(findingId + " verdict expected " + expectedVerdict + " but was " + review.verdict().name());
            }
        }
        if (expected.has("mustNotBeVerdict") && review != null) {
            String forbidden = expected.get("mustNotBeVerdict").asText();
            if (forbidden.equals(review.verdict().name())) {
                throw new AssertionError(findingId + " must not have verdict " + forbidden);
            }
        }
    }

    public static Iterable<JsonNode> scenarios(JsonNode manifest) {
        return () -> StreamSupport.stream(manifest.get("scenarios").spliterator(), false).iterator();
    }

    private static void copyRecursively(Path source, Path target) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path rel = source.relativize(dir);
                Files.createDirectories(target.resolve(rel));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Path rel = source.relativize(file);
                Files.copy(file, target.resolve(rel), StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static void deleteRecursively(Path root) throws IOException {
        if (!Files.exists(root)) {
            return;
        }
        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
