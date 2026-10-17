package com.dmi.explanation;

/** One citeable fact in an evidence pack; IDs are stable for a given pack content. */
public record EvidenceItem(String id, EvidenceKind kind, String label, String detail) {}
