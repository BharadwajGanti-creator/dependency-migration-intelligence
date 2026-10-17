# Dependency Migration Intelligence — Master Architecture & Product Plan
Version: planning v1, 17 October 2026. **Status: PROPOSED, not approved implementation policy.**

## Verified baseline
- Repository: https://github.com/BharadwajGanti-creator/dependency-migration-intelligence
- B1 merged (source-evidence integrity), B2 PR #2 merged. B2 adds reviewed migration adjudications to static findings, UI presentation, and an OpenRewrite dry-run baseline.
- One allowlisted synthetic Spring Boot parent migration, `org.springframework.boot:spring-boot-starter-parent:2.7.18 → 3.2.12`. Java 21 backend, Spring Boot 4, React/TypeScript frontend. No arbitrary repository ingestion or runtime Maven execution in API.
- Reported B2 verification: 40 backend tests and successful frontend build; CI 4/4 successful on B2 head. This is **not proof of user demand or AI accuracy**.

## Product thesis and falsifiable hypothesis
For developers reviewing a bounded dependency upgrade, a source-linked, version-specific, appropriately qualified explanation plus a concrete verification checklist may reduce uncertainty compared with an OpenRewrite diff alone. Measure usefulness, factual correctness, time to resolve, and trust; **do not claim 'safer upgrades' without evidence**.

## Architectural invariants
1. Deterministic scanner, catalogue, migration review and official documentation remain the authoritative evidence layer.
2. Generated explanations are *untrusted interpretations*, never evidence. Every technical claim must cite an input evidence ID; no fabricated source locations, patch edits, versions or test outcomes.
3. Explicit scope: one migration pair and one allowlisted synthetic fixture until evaluation supports expansion. No arbitrary repository URLs, user-uploaded code, shell execution, execution of untrusted builds or remote retrieval in production API.
4. Keep a modular Spring Boot monolith. No Kafka, distributed workers, vector DB, agent framework, auth or PostgreSQL until there is a quantified need.
5. Treat fixture content, LLM output, external docs and tool responses as untrusted. Do not send secrets or private code to any hosted model. Default no external inference without explicit opt-in.
6. Static confidence (pattern found), migration consequence verdict (human-reviewed) and AI response status (validated/rejected/disabled) are independent.
7. Reproducible results depend on fixture digest, catalogue and adjudication versions, model/prompt version, evaluation corpus and controlled configuration. Timing/request IDs may remain nondeterministic.

## Proposed modular architecture
React analyzer → Spring REST analysis endpoint → existing Maven reader/scanner/classifier → B2 `MigrationReviewEnricher` → optional B3 `ExplanationOrchestrator` → evidence pack builder → bounded LLM adapter → schema validator + citation verifier + factual-rule checks → accepted explanation OR deterministic fallback.

Separate artifacts: `AnalysisReport` (facts), `ExplanationResult` (derived, with status), `EvaluationResult` (test-only, metrics). Favor a separate opt-in explanation endpoint `POST /api/v1/analyses/explanations` accepting only server-known fixture ID, migration pair and selected finding IDs, or a report snapshot identifier if securely bound; **never trust client-submitted source evidence**. Final endpoint choice is an ADR decision for B3.

## Critical design trade-offs (choose with evidence)
| Decision | Default | Alternative / when justified |
|---|---|---|
| Model integration | provider-neutral Java interface and disabled-by-default hosted adapter | deterministic only if no provider budget/consent; local model if privacy demands and resources permit |
| Retrieval | filter indexed, checked-in curated evidence by migration pair and finding ID | embeddings/RAG only after exact-key retrieval misses necessary material in labelled tests |
| AI evaluation | fixed labelled prompt-evidence cases + independent factual checks + comparison to deterministic control | human studies for actual usefulness; do not equate schema adherence with correctness |
| Explanation output | concise schema with claims, evidenceRefs, uncertainty and next checks | free-form prose is harder to validate |
| Failure behavior | short timeout, retry cap, strict size budget, deterministic fallback | unavailable explanation must not block original analysis |
| Caching | none in B3 | content-addressed cache if cost/latency data justifies; secrets never cache |
| Async processing | synchronous bounded opt-in call initially | background jobs only after timeouts/real usage demonstrate need |
| Deployment | deferred until threat model, secret management and spend limits are documented | controlled public demo with model disabled can precede hosted inference |
| Multi-repo | explicitly excluded | later opt-in scope following tenancy, privacy, sandbox and operational design |

## Architecture gates
No next layer until previous layer meets functional tests, negative tests, security controls, evidence provenance and a demonstrable benefit. Each batch ends with a PR, CI, ADR changes, known limitations and a concise `docs/SESSION_HANDOFF.md`. PR merge is a human decision. The phase list is a *plan*, not a promise to ship all features.

## Remaining risks
- B2 value sample is tiny and synthetic; no independent user evaluation.
- OpenRewrite patch absence is not proof of migration necessity or superior DMI coverage.
- B1 bounded traversal is adequate for the allowlisted fixture but not general untrusted repository ingestion.
- Version-specific adjudications currently keyed by finding ID; adding migration pairs requires composite keys.
- Any LLM introduces privacy, prompt injection, citation laundering, cost, latency and variability risks.
