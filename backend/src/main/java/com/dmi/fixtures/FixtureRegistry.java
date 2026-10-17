package com.dmi.fixtures;

import com.dmi.api.AnalysisException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.dmi.analysis.SourceEvidenceScanner.MAX_DEPTH;
import static com.dmi.analysis.SourceEvidenceScanner.MAX_FILE_BYTES;
import static com.dmi.analysis.SourceEvidenceScanner.MAX_TOTAL_BYTES;

@Component
public class FixtureRegistry {

    private static final Set<String> KNOWN_FIXTURES = Set.of("spring-migration-case");

    private final Path fixturesRoot;

    public FixtureRegistry(@Value("${dmi.fixtures-root}") String fixturesRoot) {
        this.fixturesRoot = Path.of(fixturesRoot).toAbsolutePath().normalize();
    }

    public Path resolveFixtureRoot(String fixtureId) {
        if (!KNOWN_FIXTURES.contains(fixtureId)) {
            throw new AnalysisException(HttpStatus.NOT_FOUND, "UNKNOWN_FIXTURE", "Unknown fixture: " + fixtureId);
        }
        Path candidate = fixturesRoot.resolve(fixtureId);
        if (Files.isSymbolicLink(candidate)) {
            throw new AnalysisException(HttpStatus.NOT_FOUND, "UNSAFE_FIXTURE", "Fixture path must not be a symlink");
        }
        Path root = candidate.toAbsolutePath().normalize();
        if (!root.startsWith(fixturesRoot) || !Files.isDirectory(root)) {
            throw new AnalysisException(HttpStatus.NOT_FOUND, "UNKNOWN_FIXTURE", "Fixture directory not found");
        }
        try {
            if (!root.toRealPath().normalize().startsWith(fixturesRoot.toRealPath().normalize())) {
                throw new AnalysisException(HttpStatus.NOT_FOUND, "UNSAFE_FIXTURE", "Fixture resolves outside fixtures root");
            }
        } catch (IOException e) {
            throw new AnalysisException(HttpStatus.NOT_FOUND, "UNKNOWN_FIXTURE", "Fixture path could not be resolved");
        }
        return root;
    }

    public String digest(Path fixtureRoot) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update("dmi-digest-v2".getBytes());
            List<Path> files = Files.walk(fixtureRoot, MAX_DEPTH)
                    .filter(p -> Files.isRegularFile(p) && !Files.isSymbolicLink(p))
                    .sorted(Comparator.naturalOrder())
                    .toList();
            long totalBytes = 0;
            for (Path path : files) {
                long size = Files.size(path);
                if (size > MAX_FILE_BYTES || totalBytes + size > MAX_TOTAL_BYTES) {
                    throw new AnalysisException(HttpStatus.UNPROCESSABLE_ENTITY, "FIXTURE_TOO_LARGE", "Fixture exceeds digest limits");
                }
                totalBytes += size;
                String relative = fixtureRoot.relativize(path).toString().replace('\\', '/');
                md.update(relative.getBytes());
                md.update((byte) 0);
                md.update(Files.readAllBytes(path));
            }
            return HexFormat.of().formatHex(md.digest());
        } catch (NoSuchAlgorithmException | IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public boolean isSupportedMigration(String group, String artifact, String from, String to) {
        return Map.of(
                key("org.springframework.boot", "spring-boot-starter-parent", "2.7.18", "3.2.12"),
                true
        ).getOrDefault(key(group, artifact, from, to), false);
    }

    private static String key(String g, String a, String from, String to) {
        return g + ":" + a + ":" + from + "->" + to;
    }
}
