# B2 API contract — migration review metadata

**Status:** Implemented on `feat/dmi-b2-value-validation`  
**Endpoint:** unchanged `POST /api/v1/analyses/sync`

## Backward compatibility

- Request body unchanged (`AnalysisRequest`).
- Existing finding fields unchanged (`id`, `category`, `confidence`, `summary`, `sourceLocations`, `documentation`, `rationale`, `proposedChecks`).
- **Additive field:** each finding may include `migrationReview` (object). Clients that ignore unknown JSON fields remain compatible.

## `migrationReview` object

| Field | Type | Description |
|-------|------|-------------|
| `verdict` | enum | `ACTION_REQUIRED`, `REVIEW_RECOMMENDED`, `INFORMATIONAL`, `FALSE_POSITIVE`, `UNVERIFIED` |
| `openRewriteComparison` | enum | `PATCHED_IN_BASELINE`, `NOT_PRESENT_IN_PATCH`, `NOT_TESTED` |
| `evidenceProves` | string | What cited evidence actually establishes (not runtime proof) |
| `evidence` | array | `{ url, title, section }` reviewer citations |
| `testedScenario` | string | What was executed (static scan, offline OpenRewrite, etc.) |
| `openRewriteRecipe` | string | Recipe id used in offline baseline (empty when not applicable) |
| `openRewritePluginVersion` | string | Maven rewrite plugin version from baseline run |
| `openRewriteRecipeArtifactVersion` | string | `rewrite-spring` artifact version from baseline run |
| `reviewerNotes` | string | Human notes; separates static `confidence` from verdict |
| `lastReviewedUtc` | string | ISO date of adjudication review |
| `nextHumanCheck` | string | Suggested follow-up validation |

## Semantics

- `confidence` remains **static pattern detection** only (`CONFIRMED_STATIC`, `POSSIBLE`, `NOT_ASSESSED`).
- `migrationReview.verdict` is **human-adjudicated migration consequence** from `docs/validation/b2-adjudications.json`.
- Catalogue finding ids without a reviewed entry resolve to `UNVERIFIED` at response time (never implied safe).
- Error responses (`400`, `404`, `422`) unchanged; adjudication load failures fail application startup (invalid dataset).

## Source of truth

- Machine-readable adjudications: `docs/validation/b2-adjudications.json` (mirrored to `backend/src/main/resources/validation/b2-adjudications.json` for runtime).
