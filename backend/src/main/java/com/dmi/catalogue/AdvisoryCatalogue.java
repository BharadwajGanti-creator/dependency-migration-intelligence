package com.dmi.catalogue;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class AdvisoryCatalogue {

    private final List<CatalogueEntry> entries;

    public AdvisoryCatalogue(ObjectMapper objectMapper) throws IOException {
        JsonNode root = objectMapper.readTree(new ClassPathResource("catalogue/spring-boot-2-to-3.json").getInputStream());
        List<CatalogueEntry> loaded = new ArrayList<>();
        for (JsonNode node : root.get("entries")) {
            List<String> patterns = new ArrayList<>();
            node.get("sourcePatterns").forEach(p -> patterns.add(p.asText()));
            List<DocRef> docs = new ArrayList<>();
            node.get("documentation").forEach(d -> docs.add(new DocRef(
                    d.get("url").asText(),
                    d.get("title").asText(),
                    d.get("section").asText())));
            List<String> checks = new ArrayList<>();
            node.get("proposedChecks").forEach(c -> checks.add(c.asText()));
            loaded.add(new CatalogueEntry(
                    node.get("id").asText(),
                    node.get("summary").asText(),
                    patterns,
                    docs,
                    checks));
        }
        this.entries = List.copyOf(loaded);
    }

    public List<CatalogueEntry> entries() {
        return entries;
    }

    public record DocRef(String url, String title, String section) {}

    public record CatalogueEntry(
            String id,
            String summary,
            List<String> sourcePatterns,
            List<DocRef> documentation,
            List<String> proposedChecks) {}
}
