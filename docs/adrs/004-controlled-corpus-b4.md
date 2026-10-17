# ADR-004: B4 controlled corpus (test-only execution seam)

**Status:** Accepted (B4 batch)  
**Date:** 2026-10-17

## Context

B4 requires 2–4 additional migration scenarios without expanding the public fixture allowlist or supported migration pairs. `FixtureRegistry.KNOWN_FIXTURES` intentionally lists only `spring-migration-case`, and `AnalysisService` resolves fixtures exclusively through that registry.

## Decision

1. Store B4 scenarios under `backend/src/test/resources/corpus/scenarios/` with a checked-in `manifest.json` (hashes, labels, expected outcomes, OpenRewrite expectations).
2. Introduce a **test-only** materialization seam (`ControlledCorpusSupport`) that copies a scenario tree into a temporary fixtures root as `spring-migration-case`, then invokes the production `AnalysisService` pipeline unchanged.
3. Do **not** add corpus scenario IDs to `KNOWN_FIXTURES`, `RequestValidator`, or public API documentation allowlists.
4. Before any second migration pair is supported publicly, change adjudication keys to `(migrationPair, catalogueVersion, findingId)` per master plan (deferred in B4).

## Architecture fit (gate)

The public contract stays: one allowlisted fixture id, one Spring Boot 2.7.18→3.2.12 pair, same sync/explanation endpoints. Corpus scenarios are **evaluation inputs**, not product inputs — identical to B2’s test-only POM fixtures pattern extended to mini Spring trees. Regression tests assert manifest hashes, confidence/verdict expectations, and that unknown fixture ids still fail closed. OpenRewrite comparisons run on isolated temp copies only; results are recorded in validation docs, not wired into runtime API behavior. This avoids silent validation relaxation while still exercising classifier, scanner, and adjudication enrichment on diverse static evidence.

## Consequences

- CI gains corpus regression coverage without widening attack surface.
- Human/API users still analyze only `fixtures/spring-migration-case`.
- Adjudicated verdicts may remain on catalogue entries even when static confidence is `NOT_ASSESSED` or `POSSIBLE`; B4 documents that taxonomy explicitly.
