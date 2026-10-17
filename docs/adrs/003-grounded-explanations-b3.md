# ADR-003: B3 grounded explanation experiment

Status: accepted (Batch B3)

## Decision

Add an **opt-in** `POST /api/v1/analyses/explanations` endpoint. The server recomputes the authoritative analysis via existing `AnalysisService`; clients supply only the allowlisted analysis request and finding IDs. Evidence packs are built server-side; client-supplied paths, URLs, or report snapshots are never trusted.

`POST /api/v1/analyses/sync` remains unchanged. Default configuration keeps explanations **disabled** (`dmi.explanation.enabled=false`, `dmi.explanation.provider=disabled`).

## Provider boundary

- Java interface `ExplanationModel` with `DisabledExplanationModel` as the production default.
- `DeterministicTestExplanationModel` is registered only when `dmi.explanation.provider=deterministic-test` (tests/harness; not real inference).
- No external model calls, credentials, or downloads in this batch. Real-model evaluation status: **NOT RUN**.

## Verification

Structured provider output passes through `ExplanationVerifier` (schema-ish checks, evidence ID allow-list, verdict contradiction guards, injection/oversize rejection). Failures and disabled provider return **deterministic B2 fallback** without mutating findings.

## UI

React requests explanations separately; reviewed B2 facts remain primary. AI-assisted blocks are status-labelled (`DISABLED`, `FALLBACK`, `ACCEPTED`, etc.).
