# B3 API contract — optional grounded explanations

**Endpoint:** `POST /api/v1/analyses/explanations`  
**Status:** Implemented on `cursor/dmi-b3-grounded-explanations`

## Request

| Field | Type | Description |
|-------|------|-------------|
| `analysis` | `AnalysisRequest` | Same shape as sync analysis (validated allowlist) |
| `findingIds` | string[] | 1–16 catalogue finding ids; must exist in server recomputed report |

## Response

| Field | Type | Description |
|-------|------|-------------|
| `report` | `AnalysisReport` | Authoritative deterministic report (same semantics as sync) |
| `explanations` | `FindingExplanation[]` | Parallel array entries per requested finding id |

### `FindingExplanation`

| Field | Description |
|-------|-------------|
| `findingId` | Catalogue id |
| `evidencePackId` | Stable hash id for server-built pack |
| `status` | `ACCEPTED`, `REJECTED` (verifier failed; B2 review retained), `DISABLED`, `FALLBACK`, `TIMEOUT` |
| `summary` | Human-readable explanation or deterministic fallback banner |
| `claims` | `{ text, evidenceIds[] }` — evidenceIds must reference server pack items |
| `nextChecks` | Suggested follow-ups |
| `limitations` | Inherited pack/report limitations |
| `modelMetadata` | `{ provider, modelId, aiAssisted, experimentStatus }` |
| `deterministicReview` | Original B2 `migrationReview` (unchanged) |

## Defaults

With `dmi.explanation.enabled=false`, every explanation returns `status=DISABLED` and B2 curated text; sync endpoint behavior is unchanged.

`dmi.explanation.real-inference-enabled=false` (default) keeps harness providers synchronous; timeout executor applies only when real inference is explicitly enabled.

## Errors

Same validation as sync for `analysis`, plus:

- `400 UNSAFE_INPUT` / `INVALID_FINDING_ID` / `DUPLICATE_FINDING_ID` for malformed `findingIds`
- `422 UNKNOWN_FINDING` when a requested id is absent from the recomputed report
