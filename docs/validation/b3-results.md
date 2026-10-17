# B3 validation results — grounded explanation experiment

**Branch:** `cursor/dmi-b3-grounded-explanations`  
**Base:** `main` @ `c997eae`  
**Batch date:** 2026-10-17

## A1 — Default disabled (VERIFIED)

| Command | Result |
|---------|--------|
| `cd backend && ./mvnw verify` | PASS — **46** tests, 0 failures |
| `cd frontend && npm ci && npm run build` | PASS |

With default properties, `POST /api/v1/analyses/explanations` returns `DISABLED` explanations while preserving full sync-equivalent `report`.

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

## A4 — Fallback (VERIFIED)

Disabled provider, verifier rejection, and timeout path retain original findings via embedded `report` + `deterministicReview`.

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

## Unresolved / UNVERIFIED

- Real-model usefulness vs deterministic B2 (experiment not executed).
- End-user manual UI smoke (build-only); component maps all status labels.

## Decision note

Ship deterministic-first product; optional explanation path is opt-in with explicit disabled default and honest NOT RUN labelling for real inference.
