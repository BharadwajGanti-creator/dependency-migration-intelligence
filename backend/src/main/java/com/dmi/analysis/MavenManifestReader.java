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
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class MavenManifestReader {

    public MavenManifest read(Path fixtureRoot) {
        Path pom = fixtureRoot.resolve("pom.xml");
        if (!Files.isRegularFile(pom)) {
            throw new AnalysisException(HttpStatus.UNPROCESSABLE_ENTITY, "MISSING_POM", "pom.xml not found in fixture");
        }
        try (InputStream in = Files.newInputStream(pom)) {
            Document doc = parseSecure(in);
            Element root = doc.getDocumentElement();
            Map<String, String> properties = readProperties(root);
            ParentInfo parent = readParent(root, properties);
            List<DeclaredDependency> dependencies = readDependencies(root, properties);
            return new MavenManifest(parent, dependencies, properties);
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
        return Optional.of(new DependencyCoordinate(parent.groupId(), parent.artifactId(), parent.version()));
    }

    private Document parseSecure(InputStream in) throws SAXException, IOException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setExpandEntityReferences(false);
            factory.setNamespaceAware(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(in);
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException(e);
        }
    }

    private Map<String, String> readProperties(Element root) {
        Map<String, String> props = new HashMap<>();
        NodeList propsNodes = root.getElementsByTagName("properties");
        if (propsNodes.getLength() == 0) {
            return props;
        }
        Element propsEl = (Element) propsNodes.item(0);
        NodeList children = propsEl.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                props.put(node.getNodeName(), node.getTextContent().trim());
            }
        }
        return props;
    }

    private ParentInfo readParent(Element root, Map<String, String> properties) {
        NodeList parents = root.getElementsByTagName("parent");
        if (parents.getLength() == 0) {
            return null;
        }
        Element parent = (Element) parents.item(0);
        String group = text(parent, "groupId");
        String artifact = text(parent, "artifactId");
        String version = resolveProperty(text(parent, "version"), properties);
        return new ParentInfo(group, artifact, version);
    }

    private List<DeclaredDependency> readDependencies(Element root, Map<String, String> properties) {
        List<DeclaredDependency> deps = new ArrayList<>();
        NodeList depNodes = root.getElementsByTagName("dependency");
        for (int i = 0; i < depNodes.getLength(); i++) {
            Element dep = (Element) depNodes.item(i);
            if (!dep.getParentNode().getNodeName().equals("dependencies")) {
                continue;
            }
            String group = text(dep, "groupId");
            String artifact = text(dep, "artifactId");
            String version = resolveProperty(text(dep, "version"), properties);
            deps.add(new DeclaredDependency(group, artifact, version));
        }
        return deps;
    }

    private String text(Element parent, String tag) {
        NodeList nodes = parent.getElementsByTagName(tag);
        if (nodes.getLength() == 0) {
            return "";
        }
        return nodes.item(0).getTextContent().trim();
    }

    private String resolveProperty(String raw, Map<String, String> properties) {
        if (raw == null || raw.isBlank()) {
            return raw;
        }
        if (raw.startsWith("${") && raw.endsWith("}")) {
            String key = raw.substring(2, raw.length() - 1);
            return properties.getOrDefault(key, raw);
        }
        return raw;
    }

    public record ParentInfo(String groupId, String artifactId, String version) {}

    public record DeclaredDependency(String groupId, String artifactId, String version) {}

    public record MavenManifest(ParentInfo parent, List<DeclaredDependency> dependencies, Map<String, String> properties) {}
}
