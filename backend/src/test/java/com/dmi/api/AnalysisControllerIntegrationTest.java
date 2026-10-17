package com.dmi.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "dmi.fixtures-root=../fixtures")
class AnalysisControllerIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    private static final String VALID_BODY = """
            {
              "fixtureId": "spring-migration-case",
              "dependencyGroup": "org.springframework.boot",
              "dependencyArtifact": "spring-boot-starter-parent",
              "fromVersion": "2.7.18",
              "toVersion": "3.2.12"
            }
            """;

    @Test
    void syncAnalysisReturnsGoldenFindings() throws Exception {
        mockMvc.perform(post("/api/v1/analyses/sync")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.findings[*].id", hasItem("jakarta-servlet-api")))
                .andExpect(jsonPath("$.findings[*].id", hasItem("jakarta-annotation-postconstruct")))
                .andExpect(jsonPath("$.findings[*].id", hasItem("pathmatch-strategy")))
                .andExpect(jsonPath("$.findings[?(@.id=='jakarta-servlet-api')].confidence", hasItem("CONFIRMED_STATIC")))
                .andExpect(jsonPath("$.findings[?(@.id=='jakarta-servlet-api')].migrationReview.verdict", hasItem("ACTION_REQUIRED")))
                .andExpect(jsonPath("$.findings[?(@.id=='pathmatch-strategy')].migrationReview.verdict", hasItem("REVIEW_RECOMMENDED")))
                .andExpect(jsonPath("$.findings[?(@.id=='pathmatch-strategy')].migrationReview.openRewriteComparison", hasItem("NOT_PRESENT_IN_PATCH")))
                .andExpect(jsonPath("$.limitations.length()").value(org.hamcrest.Matchers.greaterThan(0)));
    }

    @Test
    void unknownFixtureReturns404() throws Exception {
        mockMvc.perform(post("/api/v1/analyses/sync")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY.replace("spring-migration-case", "missing-fixture")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("UNKNOWN_FIXTURE"));
    }

    @Test
    void sameVersionReturns422() throws Exception {
        mockMvc.perform(post("/api/v1/analyses/sync")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY.replace("\"toVersion\": \"3.2.12\"", "\"toVersion\": \"2.7.18\"")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("SAME_VERSION"));
    }

    @Test
    void unsupportedMigrationReturns422() throws Exception {
        mockMvc.perform(post("/api/v1/analyses/sync")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY.replace("\"toVersion\": \"3.2.12\"", "\"toVersion\": \"9.9.9\"")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_MIGRATION"));
    }

    @Test
    void traversalInFixtureIdReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/analyses/sync")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY.replace("spring-migration-case", "../etc/passwd")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void healthEndpointIsUp() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }
}
