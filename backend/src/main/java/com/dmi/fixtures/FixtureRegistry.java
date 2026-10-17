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
import java.util.HexFormat;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

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
        Path root = fixturesRoot.resolve(fixtureId).normalize();
        if (!root.startsWith(fixturesRoot) || !Files.isDirectory(root)) {
            throw new AnalysisException(HttpStatus.NOT_FOUND, "UNKNOWN_FIXTURE", "Fixture directory not found");
        }
        return root;
    }

    public String digest(Path fixtureRoot) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            try (Stream<Path> paths = Files.walk(fixtureRoot)) {
                paths.filter(Files::isRegularFile)
                        .sorted()
                        .forEach(path -> {
                            try {
                                md.update(fixtureRoot.relativize(path).toString().getBytes());
                                md.update(Files.readAllBytes(path));
                            } catch (IOException e) {
                                throw new IllegalStateException(e);
                            }
                        });
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
