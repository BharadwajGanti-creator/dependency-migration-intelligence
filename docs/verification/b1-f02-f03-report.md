# B1 verification report — F02/F03 evidence integrity

**Branch:** `feat/dmi-evidence-integrity` (merged to `main` via PR #1 @ `82a2737`)  
**Baseline main at branch point:** `c48f6e8`  
**Spec:** `docs/next-batch/02_DMI_Implementation_Batch_Spec.md`  
**Follow-on:** B2 adjudication work on `cursor/dmi-b2-value-validation-f989`; see `docs/validation/b2-results.md`.

## Commands executed (latest)

| Command | Result |
|---------|--------|
| `cd backend && ./mvnw verify` | **PASS** — **33** tests at B1 merge, 0 failures |
| `cd frontend && npm run build` | **PASS** |

## Principal review fixes (same branch)

| Finding | Resolution |
|---------|------------|
| Depth completeness gap (MAX_DEPTH+3) | Unlimited metadata walk via `FixtureBounds`; `COMPLETE` only when no deeper allowlisted files |
| `fixtureDigest` semantics | **dmi-digest-v3** + `FixtureDigestResult`; fail closed `FIXTURE_DEPTH_EXCEEDED`; documented vs scanner in ADR-002 |
| Comment-only matches | `commentOnly` on `ScanMatch`; classifier → **POSSIBLE** + rationale |
| Unresolved parent version | **422 `PARENT_VERSION_UNRESOLVED`** (not misleading `VERSION_MISMATCH`) |
| Scanner vs digest rules | ADR-002 table + digest limitation string in API report |
| Regression tests | `FixtureRegistryDigestTest`, `CommentOnlyFindingTest`, `ParentVersionUnresolvedTest`, extended depth test |

## Acceptance gates

| ID | Result |
|----|--------|
| G01–G10 | **PASS** (unchanged intent; evidence updated above) |

## Residual risks

- `pathmatch-strategy` / OpenRewrite differentiation addressed in **B2** (`docs/validation/b2-results.md`); runtime MVC probe remains UNVERIFIED.
- Digest/scanner depth policy assumes fixture layout ≤ `MAX_DEPTH`; deeper layout fails digest (by design).
- No runtime Maven effective-POM resolution.
