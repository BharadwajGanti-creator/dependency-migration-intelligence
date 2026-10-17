package com.dmi.explanation;

import com.dmi.domain.AnalysisReport;
import com.dmi.domain.DocumentationRef;
import com.dmi.domain.Finding;
import com.dmi.domain.MigrationEvidenceRef;
import com.dmi.domain.MigrationReview;
import com.dmi.domain.SourceLocation;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
public class EvidencePackBuilder {

    private static final int MAX_SOURCE_EXCERPT_CHARS = 240;
    private static final int MAX_ITEMS = 48;

    public EvidencePack build(AnalysisReport report, Finding finding) {
        List<EvidenceItem> items = new ArrayList<>();
        int seq = 0;

        items.add(new EvidenceItem(
                id(seq++),
                EvidenceKind.MIGRATION_PAIR,
                "Migration pair",
                report.before().group() + ":" + report.before().artifact() + " "
                        + report.before().version() + " → " + report.after().version()));

        items.add(new EvidenceItem(
                id(seq++),
                EvidenceKind.FIXTURE_DIGEST,
                "Fixture digest",
                report.fixtureDigest()));

        for (SourceLocation loc : finding.sourceLocations()) {
            items.add(new EvidenceItem(
                    id(seq++),
                    EvidenceKind.SOURCE_LOCATION,
                    loc.path() + ":" + loc.line(),
                    "Static scan location (excerpt not included; path:line only)."));
        }

        for (DocumentationRef doc : finding.documentation()) {
            items.add(new EvidenceItem(
                    id(seq++),
                    EvidenceKind.OFFICIAL_DOC,
                    sanitize(doc.title()),
                    sanitize(doc.url()) + " — " + sanitize(doc.section())));
        }

        MigrationReview review = finding.migrationReview();
        if (review != null) {
            items.add(new EvidenceItem(
                    id(seq++),
                    EvidenceKind.REVIEWED_VERDICT,
                    "Reviewed verdict",
                    review.verdict().name()));

            items.add(new EvidenceItem(
                    id(seq++),
                    EvidenceKind.OPENREWRITE_BASELINE,
                    "OpenRewrite comparison",
                    review.openRewriteComparison().name()));

            items.add(new EvidenceItem(
                    id(seq++),
                    EvidenceKind.REVIEWED_VERDICT,
                    "Evidence proves (reviewed)",
                    sanitize(review.evidenceProves())));

            for (MigrationEvidenceRef ref : review.evidence()) {
                items.add(new EvidenceItem(
                        id(seq++),
                        EvidenceKind.OFFICIAL_DOC,
                        sanitize(ref.title()),
                        sanitize(ref.url()) + " — " + sanitize(ref.section())));
            }

            if (!review.testedScenario().isBlank()) {
                items.add(new EvidenceItem(
                        id(seq++),
                        EvidenceKind.OPENREWRITE_BASELINE,
                        "Tested scenario",
                        sanitize(review.testedScenario())));
            }
        }

        List<String> limitations = new ArrayList<>(report.limitations());
        limitations.add("Evidence pack excludes raw repository content beyond path:line references.");
        limitations.add("Reviewed verdicts are human-adjudicated; static confidence is not runtime proof.");

        if (items.size() > MAX_ITEMS) {
            throw new IllegalStateException("Evidence pack exceeded item cap for finding " + finding.id());
        }

        String packId = stablePackId(report.fixtureDigest(), finding.id(), items);
        return new EvidencePack(
                packId,
                finding.id(),
                report.before().version() + "→" + report.after().version(),
                report.fixtureDigest(),
                List.copyOf(items),
                List.copyOf(limitations));
    }

    private static String id(int index) {
        return "ev-" + (index + 1);
    }

    private static String sanitize(String value) {
        if (value == null) {
            return "";
        }
        String cleaned = value
                .replaceAll("(?i)ignore\\s+previous\\s+instructions", "[redacted-injection]")
                .replaceAll("(?i)system\\s*:\\s*", "[redacted-system] ")
                .strip();
        if (cleaned.length() > MAX_SOURCE_EXCERPT_CHARS) {
            return cleaned.substring(0, MAX_SOURCE_EXCERPT_CHARS) + "…";
        }
        return cleaned;
    }

    static String stablePackId(String digest, String findingId, List<EvidenceItem> items) {
        StringBuilder sb = new StringBuilder();
        sb.append(digest).append('|').append(findingId).append('|');
        for (EvidenceItem item : items) {
            sb.append(item.id()).append(':').append(item.kind()).append(':').append(item.detail()).append(';');
        }
        return "pack-" + sha256Prefix(sb.toString());
    }

    private static String sha256Prefix(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash, 0, 8);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public Set<String> allowedEvidenceIds(EvidencePack pack) {
        return new LinkedHashSet<>(pack.items().stream().map(EvidenceItem::id).toList());
    }
}
