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

Structured provider output passes through `ExplanationVerifier` (schema-ish checks, evidence ID allow-list, URL allow-list on **summary and claims**, verdict contradiction guards, injection/oversize rejection). Verifier failures return **`REJECTED`** with deterministic B2 content only (no model text). Disabled provider returns **deterministic B2 fallback** without mutating findings.

Per-call timeout `ExecutorService` is used **only** when `dmi.explanation.real-inference-enabled=true` (default **false**; no external inference authorized in this batch). Harness/test providers run synchronously.

## UI

React requests explanations separately; reviewed B2 facts remain primary. AI-assisted blocks are status-labelled (`DISABLED`, `FALLBACK`, `ACCEPTED`, etc.).
