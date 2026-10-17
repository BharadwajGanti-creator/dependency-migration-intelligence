# B1 verification report — F02/F03 evidence integrity

**Branch:** `feat/dmi-evidence-integrity`  
**Baseline main at branch point:** `c48f6e8`  
**Spec:** `docs/next-batch/02_DMI_Implementation_Batch_Spec.md`

## Commands executed

| Command | Result |
|---------|--------|
| `cd backend && ./mvnw verify` | **PASS** — 21 tests, 0 failures |
| `cd frontend && npm run build` | **PASS** |
| API matrix (MockMvc + manual curl on running instance) | 200/400/404/422 paths covered in integration tests |

## Acceptance gates

| ID | Result | Evidence |
|----|--------|----------|
| G01 | PASS | `MavenManifestReaderEdgeCaseTest` excludes DM/plugin/profile deps |
| G02 | PASS | `unresolved-property.xml` → UNRESOLVED_PROPERTY + MANAGED_OR_UNKNOWN limitations |
| G03 | PASS | DOCTYPE rejected 422; secure JAXP features; oversized POM |
| G04 | PASS | `SourceEvidenceScannerTest` oversized/depth/UTF-8; symlink skip codes |
| G05 | PASS | `FindingClassifierTest` exact patternKey; no excerpt re-match |
| G06 | PASS | PARTIAL scan rationale + limitations propagation |
| G07 | PASS | Golden IDs/lines; React build; API contract unchanged |
| G08 | PASS | Full verify + frontend build executed |
| G09 | PASS | Feature branch pushed; draft PR (see below) |
| G10 | PASS | This file + updated handoff |

## Design notes

- **Digest algorithm v2** (`dmi-digest-v2` prefix + bounded walk) — `fixtureDigest` value changed vs MVP.
- **Manifest limitations** now include managed/unresolved direct dependency honesty for fixture `spring-boot-starter-web`.
- OpenRewrite baseline (`docs/baseline/openrewrite-comparison.md`) **unchanged**.
- `pathmatch-strategy` incremental value remains **not fully validated** (per F07).

## Residual risks

- Depth probe walks to MAX_DEPTH+3 (conservative PARTIAL; not a sandbox).
- Live dev server may lag compiled code until restarted.
- No runtime Maven effective-POM resolution (by design).
