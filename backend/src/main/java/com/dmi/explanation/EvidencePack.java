package com.dmi.explanation;

import java.util.List;

public record EvidencePack(
        String packId,
        String findingId,
        String migrationPairLabel,
        String fixtureDigest,
        List<EvidenceItem> items,
        List<String> packLimitations) {}
