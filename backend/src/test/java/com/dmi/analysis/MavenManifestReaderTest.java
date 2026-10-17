package com.dmi.analysis;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class MavenManifestReaderTest {

    private final MavenManifestReader reader = new MavenManifestReader();

    @Test
    void readsParentFromFixture() {
        Path fixture = Path.of("../fixtures/spring-migration-case").toAbsolutePath().normalize();
        var manifest = reader.read(fixture);
        assertThat(manifest.parent().artifactId()).isEqualTo("spring-boot-starter-parent");
        assertThat(manifest.parent().version()).isEqualTo("2.7.18");
    }
}
