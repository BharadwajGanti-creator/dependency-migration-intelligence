package com.dmi.analysis;

import com.dmi.api.AnalysisException;
import com.dmi.domain.AnalysisRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class ParentVersionUnresolvedTest {

    private static final Path FIXTURES_ROOT;

    static {
        try {
            FIXTURES_ROOT = Files.createTempDirectory("dmi-parent-unresolved");
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @DynamicPropertySource
    static void registerProps(DynamicPropertyRegistry registry) {
        registry.add("dmi.fixtures-root", () -> FIXTURES_ROOT.toString());
    }

    @Autowired
    AnalysisService analysisService;

    @BeforeEach
    void writeFixture() throws IOException {
        Path caseDir = FIXTURES_ROOT.resolve("spring-migration-case");
        Files.createDirectories(caseDir);
        Files.writeString(caseDir.resolve("pom.xml"), """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <parent>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-starter-parent</artifactId>
                    <version>${boot.version}</version>
                  </parent>
                  <groupId>t</groupId><artifactId>a</artifactId><version>1</version>
                </project>
                """);
    }

    @Test
    void unresolvedParentVersionReturnsDedicatedError() {
        assertThatThrownBy(() -> analysisService.analyze(new AnalysisRequest(
                "spring-migration-case",
                "org.springframework.boot",
                "spring-boot-starter-parent",
                "2.7.18",
                "3.2.12")))
                .isInstanceOf(AnalysisException.class)
                .extracting(e -> ((AnalysisException) e).getCode())
                .isEqualTo("PARENT_VERSION_UNRESOLVED");
    }
}
