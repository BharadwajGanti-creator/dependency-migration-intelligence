package com.dmi.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "dmi.fixtures-root=../fixtures",
        "dmi.explanation.enabled=false"
})
class ExplanationControllerIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    private static final String BODY = """
            {
              "analysis": {
                "fixtureId": "spring-migration-case",
                "dependencyGroup": "org.springframework.boot",
                "dependencyArtifact": "spring-boot-starter-parent",
                "fromVersion": "2.7.18",
                "toVersion": "3.2.12"
              },
              "findingIds": ["pathmatch-strategy", "jakarta-servlet-api"]
            }
            """;

    @Test
    void explanationsDisabledMatchesB2Report() throws Exception {
        mockMvc.perform(post("/api/v1/analyses/explanations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.report.findings", hasSize(4)))
                .andExpect(jsonPath("$.explanations", hasSize(2)))
                .andExpect(jsonPath("$.explanations[*].status", hasItem("DISABLED")))
                .andExpect(jsonPath("$.explanations[?(@.findingId=='pathmatch-strategy')].deterministicReview.verdict", hasItem("REVIEW_RECOMMENDED")))
                .andExpect(jsonPath("$.explanations[?(@.findingId=='pathmatch-strategy')].deterministicReview.openRewriteComparison", hasItem("NOT_PRESENT_IN_PATCH")));
    }

    @Test
    void unknownFindingIdReturns422() throws Exception {
        mockMvc.perform(post("/api/v1/analyses/explanations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY.replace("jakarta-servlet-api", "not-a-real-id")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("UNKNOWN_FINDING"));
    }

    @Test
    void unsafeFindingIdReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/analyses/explanations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY.replace("jakarta-servlet-api", "../etc/passwd")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("UNSAFE_INPUT"));
    }

    @Test
    void duplicateFindingIdsReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/analyses/explanations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY.replace(
                                "\"findingIds\": [\"pathmatch-strategy\", \"jakarta-servlet-api\"]",
                                "\"findingIds\": [\"pathmatch-strategy\", \"pathmatch-strategy\"]")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("DUPLICATE_FINDING_ID"));
    }

    @Test
    void embeddedReportFindingsMatchSyncEndpoint() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        String sync = mockMvc.perform(post("/api/v1/analyses/sync")
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
                .andReturn()
                .getResponse()
                .getContentAsString();

        String explanations = mockMvc.perform(post("/api/v1/analyses/explanations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode syncFindings = mapper.readTree(sync).get("findings");
        JsonNode reportFindings = mapper.readTree(explanations).get("report").get("findings");
        assertThat(reportFindings).isEqualTo(syncFindings);
    }
}

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "dmi.fixtures-root=../fixtures",
        "dmi.explanation.enabled=true",
        "dmi.explanation.provider=deterministic-test"
})
class ExplanationDeterministicProviderIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void deterministicTestProviderAcceptsVerifiedOutput() throws Exception {
        String body = """
                {
                  "analysis": {
                    "fixtureId": "spring-migration-case",
                    "dependencyGroup": "org.springframework.boot",
                    "dependencyArtifact": "spring-boot-starter-parent",
                    "fromVersion": "2.7.18",
                    "toVersion": "3.2.12"
                  },
                  "findingIds": ["pathmatch-strategy"]
                }
                """;
        mockMvc.perform(post("/api/v1/analyses/explanations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.explanations[0].status").value("ACCEPTED"))
                .andExpect(jsonPath("$.explanations[0].modelMetadata.experimentStatus").value("HARNESS_ONLY"));
    }
}

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "dmi.fixtures-root=../fixtures",
        "dmi.explanation.enabled=true",
        "dmi.explanation.provider=invalid-output-test"
})
class ExplanationInvalidOutputIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void invalidModelOutputReturnsRejectedWithDeterministicReview() throws Exception {
        String body = """
                {
                  "analysis": {
                    "fixtureId": "spring-migration-case",
                    "dependencyGroup": "org.springframework.boot",
                    "dependencyArtifact": "spring-boot-starter-parent",
                    "fromVersion": "2.7.18",
                    "toVersion": "3.2.12"
                  },
                  "findingIds": ["pathmatch-strategy"]
                }
                """;
        mockMvc.perform(post("/api/v1/analyses/explanations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.explanations[0].status").value("REJECTED"))
                .andExpect(jsonPath("$.explanations[0].deterministicReview.verdict").value("REVIEW_RECOMMENDED"))
                .andExpect(jsonPath("$.explanations[0].modelMetadata.experimentStatus").value("REJECTED_BY_VERIFIER"))
                .andExpect(jsonPath("$.explanations[0].summary").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Mandatory migration"))))
                .andExpect(jsonPath("$.explanations[0].claims[0].text").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("action required for ant_path_matcher"))))
                .andExpect(jsonPath("$.report.findings", hasSize(4)));
    }
}
