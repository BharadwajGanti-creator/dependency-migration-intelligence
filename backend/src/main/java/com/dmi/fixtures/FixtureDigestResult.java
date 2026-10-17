package com.dmi.fixtures;

import java.util.List;

/**
 * Identity hash of the fixture tree snapshot included in the digest (see ADR-002).
 */
public record FixtureDigestResult(String digest, List<String> limitations) {}
