package com.dmi.analysis;

import com.dmi.api.AnalysisException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MavenManifestReaderEdgeCaseTest {

    private final MavenManifestReader reader = new MavenManifestReader();

    @Test
    void excludesDependencyManagementAndPluginAndProfileDependencies(@TempDir Path temp) throws Exception {
        copyFixture("with-dependency-management.xml", temp);
        var result = reader.read(temp);
        assertThat(result.manifest().dependencies()).extracting(MavenManifestReader.DeclaredDependency::artifactId)
                .containsExactly("rootdep");

        copyFixture("with-plugin-dependency.xml", temp);
        result = reader.read(temp);
        assertThat(result.manifest().dependencies()).isEmpty();

        copyFixture("with-profile-dependency.xml", temp);
        result = reader.read(temp);
        assertThat(result.manifest().dependencies()).extracting(MavenManifestReader.DeclaredDependency::artifactId)
                .containsExactly("rootdep");
    }

    @Test
    void marksUnresolvedAndManagedVersions(@TempDir Path temp) throws Exception {
        copyFixture("unresolved-property.xml", temp);
        var result = reader.read(temp);
        var lib = result.manifest().dependencies().stream()
                .filter(d -> d.artifactId().equals("lib"))
                .findFirst()
                .orElseThrow();
        assertThat(lib.version().resolution()).isEqualTo(VersionResolution.UNRESOLVED_PROPERTY);
        var web = result.manifest().dependencies().stream()
                .filter(d -> d.artifactId().equals("spring-boot-starter-web"))
                .findFirst()
                .orElseThrow();
        assertThat(web.version().resolution()).isEqualTo(VersionResolution.MANAGED_OR_UNKNOWN);
    }

    @Test
    void rejectsDoctype(@TempDir Path temp) throws Exception {
        Files.writeString(temp.resolve("pom.xml"), """
                <?xml version="1.0"?>
                <!DOCTYPE project [ <!ENTITY x "1"> ]>
                <project><modelVersion>4.0.0</modelVersion></project>
                """);
        assertThatThrownBy(() -> reader.read(temp))
                .isInstanceOf(AnalysisException.class)
                .extracting(e -> ((AnalysisException) e).getStatus())
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void rejectsOversizedPom(@TempDir Path temp) throws Exception {
        byte[] big = new byte[MavenManifestReader.MAX_POM_BYTES + 1];
        Files.write(temp.resolve("pom.xml"), big);
        assertThatThrownBy(() -> reader.read(temp))
                .isInstanceOf(AnalysisException.class)
                .extracting(e -> ((AnalysisException) e).getCode())
                .isEqualTo("POM_TOO_LARGE");
    }

    private void copyFixture(String name, Path temp) throws Exception {
        Files.writeString(temp.resolve("pom.xml"), Files.readString(Path.of("src/test/resources/pom-fixtures", name)));
    }
}
