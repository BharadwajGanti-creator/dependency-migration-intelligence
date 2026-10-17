package com.dmi.explanation;

import com.dmi.domain.ExplanationClaim;
import com.dmi.domain.MigrationReview;
import com.dmi.domain.MigrationVerdict;
import com.dmi.domain.StructuredExplanation;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class ExplanationVerifier {

    private static final Pattern UNSAFE_MERGE = Pattern.compile(
            "(?i)(safe\\s+to\\s+merge|production\\s+ready|fully\\s+verified|guaranteed\\s+compatible)");
    private static final Pattern RUNTIME_VERIFIED = Pattern.compile(
            "(?i)(runtime\\s+(test|probe|verification)\\s+(passed|confirmed|executed))");
    private static final Pattern INVENTED_PATCH = Pattern.compile(
            "(?i)(openrewrite\\s+(patched|fixed|changed)\\s+.*pathmatch|rewrite\\.patch\\s+.*pathmatch)");
    private static final Pattern PROMPT_INJECTION = Pattern.compile(
            "(?i)(ignore\\s+previous\\s+instructions|disregard\\s+evidence|you\\s+are\\s+now)");
    private static final Pattern VERSION_INVENTION = Pattern.compile(
            "(?i)(spring\\s+boot\\s+)(\\d+\\.\\d+\\.\\d+)");

    private final ExplanationProperties properties;

    public ExplanationVerifier(ExplanationProperties properties) {
        this.properties = properties;
    }

    public VerifierResult verify(EvidencePack pack, MigrationReview review, StructuredExplanation explanation) {
        List<String> violations = new ArrayList<>();

        if (!pack.findingId().equals(explanation.findingId())) {
            violations.add("findingId mismatch");
        }
        if (explanation.summary() == null || explanation.summary().isBlank()) {
            violations.add("missing summary");
        } else if (explanation.summary().length() > properties.maxSummaryChars()) {
            violations.add("summary too long");
        }
        if (explanation.claims() == null || explanation.claims().isEmpty()) {
            violations.add("missing claims");
        } else if (explanation.claims().size() > properties.maxClaims()) {
            violations.add("too many claims");
        }

        Set<String> allowedIds = Set.copyOf(pack.items().stream().map(EvidenceItem::id).toList());
        Set<String> allowedUrls = allowedUrlTokens(pack);

        validateUrlList(explanation.nextChecks(), allowedUrls, "nextChecks", violations);
        validateUrlList(explanation.limitations(), allowedUrls, "limitations", violations);

        String combined = combineText(explanation);
        if (UNSAFE_MERGE.matcher(combined).find()) {
            violations.add("unsafe merge claim");
        }
        if (RUNTIME_VERIFIED.matcher(combined).find()) {
            violations.add("invented runtime verification");
        }
        if (PROMPT_INJECTION.matcher(combined).find()) {
            violations.add("prompt injection pattern");
        }

        if (explanation.summary() != null) {
            for (String url : extractUrls(explanation.summary())) {
                if (!urlAllowed(url, allowedUrls)) {
                    violations.add("unsupported URL in summary: " + url);
                }
            }
        }

        if (review != null && review.verdict() == MigrationVerdict.REVIEW_RECOMMENDED) {
            if (Pattern.compile("(?i)action\\s+required|mandatory\\s+migration|must\\s+change\\s+pathmatch")
                    .matcher(combined)
                    .find()) {
                violations.add("contradicts REVIEW_RECOMMENDED with mandatory action");
            }
            if (INVENTED_PATCH.matcher(combined).find()) {
                violations.add("invented OpenRewrite pathmatch patch");
            }
        }

        if (explanation.claims() != null) {
        for (ExplanationClaim claim : explanation.claims()) {
            if (claim.text() == null || claim.text().isBlank()) {
                violations.add("empty claim");
                continue;
            }
            if (claim.text().length() > properties.maxClaimChars()) {
                violations.add("claim too long");
            }
            if (claim.evidenceIds() == null || claim.evidenceIds().isEmpty()) {
                violations.add("claim missing evidenceIds");
                continue;
            }
            for (String evidenceId : claim.evidenceIds()) {
                if (!allowedIds.contains(evidenceId)) {
                    violations.add("unknown evidenceId: " + evidenceId);
                }
            }
            for (String url : extractUrls(claim.text())) {
                if (!urlAllowed(url, allowedUrls)) {
                    violations.add("unsupported URL in claim: " + url);
                }
            }
        }
        }

        for (var match : VERSION_INVENTION.matcher(combined).results().toList()) {
            String version = match.group(2);
            if (!Set.of("2.7.18", "3.2.12").contains(version)) {
                violations.add("unsupported version reference: " + version);
            }
        }

        return new VerifierResult(violations.isEmpty(), List.copyOf(violations));
    }

    private static String combineText(StructuredExplanation explanation) {
        StringBuilder sb = new StringBuilder(explanation.summary() != null ? explanation.summary() : "");
        if (explanation.claims() != null) {
            for (ExplanationClaim claim : explanation.claims()) {
                if (claim.text() != null) {
                    sb.append(' ').append(claim.text());
                }
            }
        }
        if (explanation.nextChecks() != null) {
            explanation.nextChecks().forEach(line -> sb.append(' ').append(line));
        }
        if (explanation.limitations() != null) {
            explanation.limitations().forEach(line -> sb.append(' ').append(line));
        }
        return sb.toString();
    }

    private static void validateUrlList(
            List<String> lines, Set<String> allowedUrls, String field, List<String> violations) {
        if (lines == null) {
            return;
        }
        for (String line : lines) {
            if (line == null) {
                continue;
            }
            for (String url : extractUrls(line)) {
                if (!urlAllowed(url, allowedUrls)) {
                    violations.add("unsupported URL in " + field + ": " + url);
                }
            }
        }
    }

    private static Set<String> allowedUrlTokens(EvidencePack pack) {
        return pack.items().stream()
                .filter(i -> i.kind() == EvidenceKind.OFFICIAL_DOC)
                .flatMap(i -> extractUrls(i.detail()).stream())
                .collect(java.util.stream.Collectors.toSet());
    }

    private static List<String> extractUrls(String text) {
        List<String> urls = new ArrayList<>();
        int idx = 0;
        while (true) {
            int http = text.indexOf("http", idx);
            if (http < 0) {
                break;
            }
            int end = http;
            while (end < text.length() && !Character.isWhitespace(text.charAt(end))) {
                end++;
            }
            urls.add(text.substring(http, end));
            idx = end;
        }
        return urls;
    }

    private static boolean urlAllowed(String url, Set<String> allowed) {
        for (String allowedUrl : allowed) {
            if (url.equals(allowedUrl)) {
                return true;
            }
            if (url.startsWith(allowedUrl) && url.length() > allowedUrl.length()) {
                char boundary = url.charAt(allowedUrl.length());
                if (boundary == '#' || boundary == '?' || boundary == '/') {
                    return true;
                }
            }
        }
        return false;
    }

    public record VerifierResult(boolean accepted, List<String> violations) {}
}
