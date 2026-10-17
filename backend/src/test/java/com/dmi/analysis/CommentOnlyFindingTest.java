package com.dmi.analysis;

import com.dmi.domain.Confidence;
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
class CommentOnlyFindingTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void bundledFixtureUsesConfirmedStaticForImportsNotComments() throws Exception {
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
                .andExpect(jsonPath("$.findings[?(@.id=='jakarta-servlet-api')].confidence", hasItem("CONFIRMED_STATIC")));
    }

    @Test
    void commentOnlyMatchIsPossibleNotConfirmed() {
        var classifier = new FindingClassifier();
        var entry = new com.dmi.catalogue.AdvisoryCatalogue.CatalogueEntry(
                "c1", "s", java.util.List.of("javax\\.servlet"), java.util.List.of(), java.util.List.of());
        var match = new SourceEvidenceScanner.ScanMatch(0, "javax\\.servlet", "src/X.java", 1, "// javax.servlet", true);
        var finding = classifier.classify(java.util.List.of(entry), new ScanResult(java.util.List.of(match), ScanCompleteness.COMPLETE, java.util.List.of())).get(0);
        org.assertj.core.api.Assertions.assertThat(finding.confidence()).isEqualTo(Confidence.POSSIBLE);
        org.assertj.core.api.Assertions.assertThat(finding.rationale()).contains("comment");
    }
}
