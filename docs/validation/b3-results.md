# B3 validation results — grounded explanation experiment

**Branch:** `cursor/dmi-b3-grounded-explanations`  
**Base:** `main` @ `c997eae`  
**Batch date:** 2026-10-17  
**PR review follow-up:** verifier fail-closed URLs, `REJECTED` status, `findingIds` validation, negative tests (same PR #3).

## A1 — Default disabled (VERIFIED)

| Command | Result |
|---------|--------|
| `cd backend && ./mvnw verify` | PASS — **51** tests, 0 failures |
| `cd frontend && npm ci && npm run build` | PASS |

With default properties, `POST /api/v1/analyses/explanations` returns `DISABLED` explanations while preserving full sync-equivalent `report`. Integration test asserts embedded `report.findings` equals sync response findings JSON.

## PR review fixes (VERIFIED)

| Finding | Fix |
|---------|-----|
| Verifier fail-open on unknown URLs | `urlAllowed` returns false when URL not in pack docs |
| Verifier failures surfaced as `FALLBACK` only | Invalid model output → `REJECTED` + retained `deterministicReview` |
| `findingIds` not validated | `RequestValidator.validateFindingIds` (format, traversal, duplicates) |
| Negative/security coverage gaps | `ExplanationVerifierSecurityTest`, invalid-output integration test, unsafe/duplicate id API tests |

## A2 — Sanitized evidence packs (VERIFIED)

Server-built packs include path:line references, reviewed verdict metadata, official doc URLs from adjudication, and limitations. Raw repository bodies are excluded. Injection-like substrings in reviewed fields are redacted in pack detail.

## A3 — Verifier limits (VERIFIED)

Labelled corpus: `backend/src/test/resources/validation/b3-labelled-cases.json` (**14** cases ≥12 gate).

| Metric | Numerator | Denominator |
|--------|-----------|-------------|
| Known-valid cases accepted | 4 | 4 |
| Known-invalid cases rejected | 10 | 10 |
| False ACTION_REQUIRED on pathmatch in corpus | 0 | 1 contradictory case |

Citations required per claim; semantic support is **not** fully proven — narrow structural checks only.

## A4 — Fallback / reject (VERIFIED)

Disabled provider and timeout retain original findings via embedded `report` + `deterministicReview`. Verifier rejection returns **`REJECTED`** (deterministic B2 text in summary/claims) without mutating report findings.

## A5 — Security / regression (VERIFIED)

Corpus includes adversarial prompt-injection and unsafe-merge outputs (all rejected). Existing sync integration tests unchanged.

## A6 — Corpus coverage (VERIFIED)

Categories: supported (4), contradictory (6), ambiguous (2), adversarial (4). Spans all four fixture finding ids plus synthetic adverse outputs.

## A7 — Baseline vs AI (NOT RUN)

| Mode | Status | Notes |
|------|--------|-------|
| Deterministic B2 report | VERIFIED | Existing sync + adjudication |
| `deterministic-test` provider | HARNESS_ONLY | No external inference |
| Real LLM provider | **NOT RUN** | No credentials authorized |

Latency/cost/usefulness: **NOT MEASURED**.

## A8 — Public hosted AI (VERIFIED)

Default config disables explanations; no hosted model endpoint enabled.

## Manual smoke (VERIFIED 2026-10-17)

```bash
cd backend && DMI_FIXTURES_ROOT=/workspace/fixtures ./mvnw spring-boot:run
# separate terminal:
curl -s -X POST http://127.0.0.1:8787/api/v1/analyses/explanations \
  -H 'Content-Type: application/json' \
  -d '{"analysis":{"fixtureId":"spring-migration-case","dependencyGroup":"org.springframework.boot","dependencyArtifact":"spring-boot-starter-parent","fromVersion":"2.7.18","toVersion":"3.2.12"},"findingIds":["pathmatch-strategy"]}' \
  | jq '.explanations[0].status,.explanations[0].deterministicReview.verdict'
# Observed: "DISABLED", "REVIEW_RECOMMENDED"
```

UI: `cd frontend && npm run dev` — run sync analysis, click **Request explanation** on a finding; panel shows **AI explanations disabled** with B2 review text visible above.

## Unresolved / UNVERIFIED

- Real-model usefulness vs deterministic B2 (experiment not executed).

## Merge readiness (agent)

CI green after fixes; diff limited to review items above. **Recommend human merge** of PR #3 after reviewer confirms — agent did not merge.

## Decision note

Ship deterministic-first product; optional explanation path is opt-in with explicit disabled default and honest NOT RUN labelling for real inference.
