package com.dmi.analysis;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "dmi.fixtures-root=../fixtures")
class GoldenExpectedFindingsTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void goldenFindingIdsAndKeyLines() throws Exception {
        mockMvc.perform(post("/api/v1/analyses/sync")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fixtureId": "spring-migration-case",
                                  "dependencyGroup": "org.springframework.boot",
                                  "dependencyArtifact": "spring-boot-starter-parent",
                                  "fromVersion": "2.7.18",
                                  "toVersion": "3.2.12"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.findings[*].id", hasItem("jakarta-servlet-api")))
                .andExpect(jsonPath("$.findings[*].id", hasItem("jakarta-annotation-postconstruct")))
                .andExpect(jsonPath("$.findings[*].id", hasItem("pathmatch-strategy")))
                .andExpect(jsonPath("$.findings[?(@.id=='jakarta-annotation-postconstruct')].sourceLocations[0].line").value(3))
                .andExpect(jsonPath("$.findings[?(@.id=='pathmatch-strategy')].sourceLocations[0].path").value("src/main/resources/application.properties"));
    }
}
