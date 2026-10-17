# DMI B2 — Implementation Specification and Acceptance Gates

Status: proposal for one Cursor implementation batch after merging B1 to `main`.

## Work order

### B2-A — Baseline and ground truth
- Pull fresh `main`, confirm B1 merged and clean tree; create `feat/dmi-b2-value-validation`.
- Read only `docs/baseline/openrewrite-comparison.md`, catalogue, fixture, relevant backend/API/frontend classes and current tests.
- Verify **which** OpenRewrite recipe, plugin and recipe artifact were executed, with reproducible commands and pinned versions. Existing documentation is not by itself execution proof for a new test.
- Inspect Spring Boot 3.0 migration guide and Boot 3.2 version-specific docs directly; store exact URLs and applicability, distinguish inference from tested outcomes.
- Perform a minimal runtime/properties investigation where feasible on a copied fixture. Never change or execute the analyzer's checked-in original fixture through untrusted inputs. Preserve logs or concise transcripts in `docs/validation/` (no machine-specific `/tmp` path as sole evidence).
- Review each of the four currently reported findings: Jakarta servlet, PostConstruct, path-matching strategy and parent upgrade. Determine if actionable, review-only, informational, false-positive or unverified. Explain OpenRewrite's coverage, not just patch presence.

### B2-B — Deterministic review dataset
- Add `docs/validation/b2-adjudications.json` with a defined schema, reviewed findings, and evidence provenance. No generated unsupported claims.
- Reject unknown verdict enums and duplicate finding IDs at test or startup validation. Missing entries must map to `UNVERIFIED` (never implied safe).
- Record comparison status independently, e.g. `PATCHED_IN_BASELINE | NOT_PRESENT_IN_PATCH | NOT_TESTED`; use statuses that match the evidence.
- Separate pattern detection confidence (`CONFIRMED_STATIC`, `POSSIBLE`, `NOT_ASSESSED`) from migration verdict.

### B2-C — API + UI
- Provide a backward-compatible way of returning adjudication for existing finding IDs. Prefer optional per-finding metadata; document exact JSON schema and error semantics in `docs/validation/b2-api-contract.md`.
- Present confidence, adjudication verdict, evidence links and next human check in the analyzer, with an obvious `UNVERIFIED` state.
- Avoid claiming safety, migration success, or correctness based solely on the report.
- Preserve existing API request shape, response fields, fixtureDigest semantics, sorting, error codes and performance bounds.

### B2-D — Evidence and verification
- Create at least one controlled positive and one controlled negative example for each rule being asserted as meaningful; mark cases that cannot be run, don't fabricate.
- Test persistence-free deterministic mapping; duplicate IDs; unknown IDs; missing adjudication; invalid verdict; citation data; status independence from code pattern confidence.
- Test React display for at least `ACTION_REQUIRED`, `INFORMATIONAL` and `UNVERIFIED` (if unit-test tooling exists; otherwise document manual smoke evidence).
- Ensure 200/400/404/422 API regression remains green. Run `cd backend && ./mvnw verify` and `cd frontend && npm ci && npm run build`.
- Produce `docs/validation/b2-results.md` with experiment logs/commands, reviewer rationale, numerator/denominator for each metric, unresolved questions and exact evidence source.
- Update stale B1 handoff/docs facts, and write `docs/SESSION_HANDOFF.md` for B3.
- Commit and push feature branch, open draft PR if possible, do not auto-merge.

## Acceptance gates (must all be evidenced, or explicitly NOT VERIFIED)
- V01 baseline versions and commands recorded; no fabricated execution.
- V02 `pathmatch-strategy` verdict grounded in applicable docs and experimental findings, or explicitly UNVERIFIED.
- V03 all current findings assigned evidence-backed or UNVERIFIED verdicts.
- V04 baseline comparison says what was patched, not automatically what OpenRewrite could/couldn't handle.
- V05 API contract backward-compatible; no breaking changes without approved ADR.
- V06 UI cleanly separates static detection and reviewed migration consequence.
- V07 negative case does not trigger false `ACTION_REQUIRED` recommendation.
- V08 missing reviewed entry handled as UNVERIFIED, with tests.
- V09 backend and frontend checks run; exact test counts, failures and CI state reported.
- V10 source control, documentation and reproducible validation evidence included.

## Decision threshold
GO for B3 only if there is at least one **reproduced, useful** user decision supported by DMI's traceable evidence (even if OpenRewrite can also achieve it), the system does not overstate necessity, and a small labelled set reveals acceptable errors with honest counts. Otherwise refine the value proposition, not add AI to conceal weak evidence.

## Prohibited
No second migration pair, importing random GitHub repositories, production deployment, external LLM, LangChain/LangGraph, vector store, auth, multi-tenant architecture, autosuggested upgrades, Kafka or new persistence.
