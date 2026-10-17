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
