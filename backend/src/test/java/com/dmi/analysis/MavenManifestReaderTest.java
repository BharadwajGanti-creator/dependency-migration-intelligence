package com.dmi.analysis;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class MavenManifestReaderTest {

    private final MavenManifestReader reader = new MavenManifestReader();

    @Test
    void readsParentFromFixture() {
        Path fixture = Path.of("../fixtures/spring-migration-case").toAbsolutePath().normalize();
        var result = reader.read(fixture);
        assertThat(result.manifest().parent().artifactId()).isEqualTo("spring-boot-starter-parent");
        assertThat(result.manifest().parent().resolvedVersion()).isEqualTo("2.7.18");
    }
}
