package com.dmi.corpus;

import com.dmi.analysis.AnalysisService;
import com.dmi.domain.AnalysisReport;
import com.dmi.domain.Finding;
import com.dmi.fixtures.FixtureRegistry;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ControlledCorpusRegressionTest {

    private static final Path FIXTURES_ROOT;

    static {
        try {
            FIXTURES_ROOT = Files.createTempDirectory("dmi-b4-corpus");
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

    @TestFactory
    Collection<DynamicTest> corpusScenariosMatchManifestExpectations() throws IOException {
        JsonNode manifest = ControlledCorpusSupport.loadManifest();
        Collection<DynamicTest> tests = new ArrayList<>();
        for (JsonNode scenario : ControlledCorpusSupport.scenarios(manifest)) {
            String scenarioId = scenario.get("id").asText();
            tests.add(DynamicTest.dynamicTest(scenarioId, () -> runScenarioAssertions(scenario)));
        }
        return tests;
    }

    @Test
    void manifestScenarioHashesMatchCheckedInContent() throws IOException {
        JsonNode manifest = ControlledCorpusSupport.loadManifest();
        for (JsonNode scenario : ControlledCorpusSupport.scenarios(manifest)) {
            Path root = ControlledCorpusSupport.locateScenarioRoot(scenario.get("scenarioDir").asText());
            String expected = scenario.get("contentSha256").asText();
            String actual = ControlledCorpusSupport.contentSha256(root);
            assertThat(actual).as("contentSha256 for " + scenario.get("id").asText()).isEqualTo(expected);
        }
    }

    @Test
    void publicFixtureAllowlistRejectsCorpusScenarioIds() {
        var registry = new FixtureRegistry(FIXTURES_ROOT.toString());
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> registry.resolveFixtureRoot("b4-scenario-jakarta-servlet-tp"))
                .hasMessageContaining("Unknown fixture");
    }

    private void runScenarioAssertions(JsonNode scenario) throws IOException {
        Path materialized = ControlledCorpusSupport.materializeScenario(
                scenario.get("scenarioDir").asText(), FIXTURES_ROOT);
        assertThat(materialized).isDirectory();

        AnalysisReport report = analysisService.analyze(ControlledCorpusSupport.STANDARD_MIGRATION_REQUEST);

        for (JsonNode expected : scenario.get("expectedOutcomes")) {
            String findingId = expected.get("findingId").asText();
            Finding finding = ControlledCorpusSupport.finding(report, findingId)
                    .orElseThrow(() -> new AssertionError("Missing finding " + findingId + " in scenario " + scenario.get("id").asText()));
            ControlledCorpusSupport.assertExpectedOutcome(finding, expected);
        }
    }

    @Test
    void corpusTaxonomyLabelsAreDocumented() {
        JsonNode manifest = ControlledCorpusSupport.loadManifest();
        long labelled = StreamSupport.stream(manifest.get("scenarios").spliterator(), false)
                .flatMap(s -> StreamSupport.stream(s.get("expectedOutcomes").spliterator(), false))
                .filter(o -> o.has("taxonomy"))
                .count();
        assertThat(labelled).isGreaterThanOrEqualTo(8);
    }
}
