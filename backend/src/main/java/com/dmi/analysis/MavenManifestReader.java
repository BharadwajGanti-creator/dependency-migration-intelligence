package com.dmi.analysis;

import com.dmi.api.AnalysisException;
import com.dmi.domain.DependencyCoordinate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class MavenManifestReader {

    public static final int MAX_POM_BYTES = 512 * 1024;

    public ManifestReadResult read(Path fixtureRoot) {
        Path pom = fixtureRoot.resolve("pom.xml");
        if (!Files.isRegularFile(pom)) {
            throw new AnalysisException(HttpStatus.UNPROCESSABLE_ENTITY, "MISSING_POM", "pom.xml not found in fixture");
        }
        try {
            long size = Files.size(pom);
            if (size > MAX_POM_BYTES) {
                throw new AnalysisException(HttpStatus.UNPROCESSABLE_ENTITY, "POM_TOO_LARGE", "pom.xml exceeds size limit");
            }
            byte[] bytes = Files.readAllBytes(pom);
            Document doc = parseSecure(bytes);
            Element root = doc.getDocumentElement();
            if (root == null || !"project".equals(root.getNodeName())) {
                throw new AnalysisException(HttpStatus.UNPROCESSABLE_ENTITY, "MALFORMED_POM", "Missing project root element");
            }
            List<ManifestDiagnostic> diagnostics = new ArrayList<>();
            Map<String, String> properties = readProperties(root, diagnostics);
            ParentInfo parent = readParent(root, properties, diagnostics);
            List<DeclaredDependency> dependencies = readDirectDependencies(root, properties, diagnostics);
            return new ManifestReadResult(new MavenManifest(parent, dependencies, properties), diagnostics);
        } catch (IOException | SAXException e) {
            throw new AnalysisException(HttpStatus.UNPROCESSABLE_ENTITY, "MALFORMED_POM", "Could not parse pom.xml");
        }
    }

    public Optional<DependencyCoordinate> resolveParent(MavenManifest manifest, String group, String artifact) {
        ParentInfo parent = manifest.parent();
        if (parent == null) {
            return Optional.empty();
        }
        if (!parent.groupId().equals(group) || !parent.artifactId().equals(artifact)) {
            return Optional.empty();
        }
        return Optional.of(new DependencyCoordinate(parent.groupId(), parent.artifactId(), parent.resolvedVersion()));
    }

    private Document parseSecure(byte[] bytes) throws SAXException, IOException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            factory.setNamespaceAware(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(new ByteArrayInputStream(bytes));
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException(e);
        }
    }

    private Map<String, String> readProperties(Element root, List<ManifestDiagnostic> diagnostics) {
        Map<String, String> props = new HashMap<>();
        int count = countDirectChildElements(root, "properties");
        if (count > 1) {
            diagnostics.add(new ManifestDiagnostic("DUPLICATE_ROOT_PROPERTIES", "Multiple project/properties elements"));
        }
        Element propsEl = directChildElement(root, "properties");
        if (propsEl == null) {
            return props;
        }
        NodeList children = propsEl.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                props.put(node.getNodeName(), node.getTextContent().trim());
            }
        }
        return props;
    }

    private ParentInfo readParent(Element root, Map<String, String> properties, List<ManifestDiagnostic> diagnostics) {
        int count = countDirectChildElements(root, "parent");
        if (count > 1) {
            diagnostics.add(new ManifestDiagnostic("DUPLICATE_ROOT_PARENT", "Multiple project/parent elements"));
        }
        Element parentEl = directChildElement(root, "parent");
        if (parentEl == null) {
            return null;
        }
        String group = directChildText(parentEl, "groupId");
        String artifact = directChildText(parentEl, "artifactId");
        String versionRaw = directChildText(parentEl, "version");
        ResolvedVersion version = resolveProperty(versionRaw, properties);
        if (group.isBlank() || artifact.isBlank()) {
            diagnostics.add(new ManifestDiagnostic("INCOMPLETE_PARENT", "Parent coordinates missing groupId or artifactId"));
        }
        return new ParentInfo(group, artifact, versionRaw, version);
    }

    private List<DeclaredDependency> readDirectDependencies(Element root, Map<String, String> properties, List<ManifestDiagnostic> diagnostics) {
        int depsBlocks = countDirectChildElements(root, "dependencies");
        if (depsBlocks > 1) {
            diagnostics.add(new ManifestDiagnostic("DUPLICATE_ROOT_DEPENDENCIES", "Multiple project/dependencies blocks"));
        }
        Element depsEl = directChildElement(root, "dependencies");
        List<DeclaredDependency> deps = new ArrayList<>();
        if (depsEl == null) {
            return deps;
        }
        Map<String, Integer> seen = new LinkedHashMap<>();
        NodeList children = depsEl.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE || !"dependency".equals(node.getNodeName())) {
                continue;
            }
            Element dep = (Element) node;
            String group = directChildText(dep, "groupId");
            String artifact = directChildText(dep, "artifactId");
            String versionRaw = directChildText(dep, "version");
            ResolvedVersion version = resolveProperty(versionRaw, properties);
            if (group.isBlank() || artifact.isBlank()) {
                diagnostics.add(new ManifestDiagnostic("INCOMPLETE_DEPENDENCY", "Direct dependency missing groupId or artifactId"));
                continue;
            }
            String key = group + ":" + artifact;
            seen.merge(key, 1, Integer::sum);
            deps.add(new DeclaredDependency(group, artifact, versionRaw, version));
        }
        seen.forEach((k, n) -> {
            if (n > 1) {
                diagnostics.add(new ManifestDiagnostic("DUPLICATE_DIRECT_DEPENDENCY", k + " declared " + n + " times"));
            }
        });
        return deps;
    }

    private ResolvedVersion resolveProperty(String raw, Map<String, String> properties) {
        if (raw == null || raw.isBlank()) {
            return new ResolvedVersion("", VersionResolution.MANAGED_OR_UNKNOWN);
        }
        if (!raw.startsWith("${") || !raw.endsWith("}")) {
            return new ResolvedVersion(raw, VersionResolution.LITERAL);
        }
        String key = raw.substring(2, raw.length() - 1);
        if (!properties.containsKey(key)) {
            return new ResolvedVersion(raw, VersionResolution.UNRESOLVED_PROPERTY);
        }
        String value = properties.get(key);
        if (value.startsWith("${") && value.endsWith("}")) {
            return new ResolvedVersion(raw, VersionResolution.UNRESOLVED_PROPERTY);
        }
        return new ResolvedVersion(value, VersionResolution.RESOLVED_FROM_PROPERTY);
    }

    private Element directChildElement(Element parent, String name) {
        NodeList children = parent.getChildNodes();
        Element found = null;
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE && name.equals(node.getNodeName())) {
                if (found != null) {
                    return found;
                }
                found = (Element) node;
            }
        }
        return found;
    }

    private int countDirectChildElements(Element parent, String name) {
        int count = 0;
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE && name.equals(node.getNodeName())) {
                count++;
            }
        }
        return count;
    }

    private String directChildText(Element parent, String name) {
        Element child = directChildElement(parent, name);
        if (child == null) {
            return "";
        }
        return child.getTextContent().trim();
    }

    public record ResolvedVersion(String display, VersionResolution resolution) {}

    public record ParentInfo(String groupId, String artifactId, String versionRaw, ResolvedVersion version) {
        public String resolvedVersion() {
            return version.display().isBlank() && version.resolution() == VersionResolution.MANAGED_OR_UNKNOWN
                    ? versionRaw
                    : version.display();
        }
    }

    public record DeclaredDependency(
            String groupId,
            String artifactId,
            String versionRaw,
            ResolvedVersion version) {}

    public record MavenManifest(ParentInfo parent, List<DeclaredDependency> dependencies, Map<String, String> properties) {}

    public record ManifestReadResult(MavenManifest manifest, List<ManifestDiagnostic> diagnostics) {}
}
