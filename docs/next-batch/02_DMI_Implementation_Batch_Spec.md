# DMI — Batch B1: F02/F03 Correctness and Security Implementation Specification

**Execution state:** PLANNED. **Baseline:** GitHub repository `BharadwajGanti-creator/dependency-migration-intelligence`, current remote default branch. Verify `git rev-parse HEAD` before writing; never assume `f55359a` is still HEAD. Architecture delta: `01_DMI_F02_F03_Architecture_Delta.md`.

## Objective and end-to-end outcome

One bounded PR turns the currently plausible migration evidence into **trustworthy, completeness-aware evidence**. Keep the current React→Spring API→report journey functional. The batch is not complete merely because unit tests pass: HTTP response must convey incomplete analysis correctly, regressions must be in place, and the existing fixture must produce the expected report.

## Work packages — implement in this order, without stop-and-ask between them

### B1-A — Manifest shape and parser correctness

**Primary files:** `backend/.../analysis/MavenManifestReader.java`, new targeted tests. Existing parent `resolveParent` must continue working.

- Replace broad descendant node access with **direct-child-by-name** helpers for project `parent`, `properties`, and `dependencies` and their immediate fields. Detect malformed/duplicate root structural nodes and fail or diagnose explicitly.
- Distinguish direct dependency from `dependencyManagement`, `plugin/dependencies`, `profile/dependencies`; return ONLY immediate root-level `project/dependencies/dependency` in `manifest.dependencies()`.
- Preserve raw and resolved property value or diagnostic; avoid silently treating unresolved `${x}` as a concrete version. Single root-property substitution may resolve one step. Nested/circular/inherited unresolved expressions stay unknown.
- Expose minimal structured diagnostics for duplicate coordinates, empty coordinates, missing versions and unresolved property references. Missing versions are not always invalid—may be managed by BOM/parent—so label unknown, don't invent.
- Add secure XML parser hardening: DOCTYPE rejection, external general/parameter entities off, external DTD/schema access disabled when supported, XInclude off, entity references off, reasonable POM byte cap applied before parse. Preserve clean 422 on malformed XML and XXE.
- Maintain a clear boundary between Maven **parent** and a direct dependency; do not broaden API request options during this batch.

**Tests:** direct parent; root direct dependency; dependencyManagement excluded; plugin dependency excluded; profile dependency excluded; missing/multiple declarations; property resolvable; missing property; nested/cyclic property; malformed XML; DOCTYPE/XXE; oversized POM; missing parent/mismatched parent. Assertions include both data and classification/diagnostic.

### B1-B — Scanner completeness and filesystem restrictions

**Primary files:** `SourceEvidenceScanner.java`, `FixtureRegistry.java` if necessary, `SourceEvidenceScannerTest.java`.

- Return `ScanResult(matches, completeness, diagnostics)` (exact internal signature is implementation decision). Update consumers. `ScanMatch` should carry a **pattern key or index**, not just excerpt text, to permit sound classification.
- File traversal must be bounded, deterministic and closed; skip symlinks (including in-root), files outside resolved fixture root, unsupported extensions, and invalid encodings. Avoid reading source until size/budget checks pass.
- Emit PARTIAL plus diagnostic if any eligible source input was skipped for size, limits, encoding, read errors, or depth. Do not equate empty matches with completeness.
- For depth limits, ensure test proves the scanner does not claim COMPLETE when eligible files exist beyond the limit. If a truly exhaustive depth check cannot fit the bounded design, document an honest conservative completeness policy.
- Keep line numbers stable across LF/CRLF and Unicode; don't claim that regex matching in comments necessarily proves live code. Match only known catalogue patterns, scan bounded file types.
- Fix fixture digest to be finite, stable and safe under corresponding limits; protect `FixtureRegistry.resolveFixtureRoot` from symlink redirect of allowlisted fixture ID. Fail closed on invalid fixture rather than silently returning a partial digest.

**Tests:** 1-line and multi-line; LF/CRLF; UTF-8 Unicode; invalid UTF-8; >256 KiB file; >200 files; total bytes >5 MiB; depth >12; symlink escape/in-root symlink; unreadable file where feasible; deterministic sorted match order; digest stable across scan ordering; safe failure on oversized fixture. Platform-dependent permissions test can use assumptions, not false success.

### B1-C — Classifier and report propagation

**Primary files:** `FindingClassifier.java`, `AnalysisService.java`, `ReportComposer.java`, targeted tests.

- Match finding rules by **exact pattern identity** from the scanner, never by a second search against arbitrary matched excerpts. Remove redundant duplicate compile.
- Deduplicate source locations; preserve sorted findings/locations.
- Propagate scanner and manifest diagnostics into `AnalysisReport.limitations`, without leaking absolute file paths or complete source.
- If scan is PARTIAL, make this explicit in report limitations, especially for `NOT_ASSESSED` results. Never elevate confidence because a catalogue entry exists; retain static-only semantics.
- Keep existing schema stable (`requestId`, `generatedAtUtc`, `before`, `after`, `findings`, `limitations`, etc.). Dynamic timestamps/UUIDs are fine; compare golden responses excluding nondeterministic metadata.
- Do not convert unknown Maven effective dependency versions to verified upgrade facts.

**Tests:** irrelevant source excerpt does not create finding; pattern identity matters; duplicate patterns produce one location; empty complete vs empty partial difference; correct finding ordering; documentation references remain attached; absent match never labeled safe.

### B1-D — Full regression, docs and delivery

- Verify `./backend/mvnw verify` from backend directory and `npm ci && npm run build` from frontend. Report exact counts and commands.
- API smoke cases: 200 supported parent transition; 400 malformed body/traversal; 404 unknown fixture; 422 same version, wrong parent version, unsupported migration. Use existing endpoint, no new public endpoints.
- Validate golden IDs and line numbers against `docs/baseline/expected-findings.json`; where fixture changes necessary, explain why before updating expected outputs. Do not silently rewrite golden data to match a buggy implementation.
- Document evidence in `docs/verification/b1-f02-f03-report.md`: command results, tables of edge cases, any skipped tests, safe-scan diagnostics, OpenRewrite baseline unchanged, unresolved risks.
- Update `docs/SESSION_HANDOFF.md` accurately, max 600 words; preserve references to F07 and the unresolved `pathmatch-strategy` validation; add implementation decisions.
- Make a feature branch `feat/dmi-evidence-integrity` from verified `main` (if branch not already present). Commit changes, push branch, open PR if authentication allows. Do NOT merge to main automatically. If unavailable, retain clean local commit and give exact commands. Don't claim pushed or PR created without evidence.

## Acceptance gates (all mandatory)

| ID | Gate | PASS evidence |
|---|---|---|
| G01 | Maven tree correctness | Test proves nested dependencyManagement/plugin/profile entries excluded |
| G02 | Unknown version honesty | Unresolved or managed versions explicitly unknown, no guessed effective version |
| G03 | XML safety | XXE/DOCTYPE/malformed POM safely rejected with correct status |
| G04 | Source boundaries | Symlink, file count/bytes/depth/encoding cases safely bounded and diagnostics retained |
| G05 | Classification integrity | Every CONFIRMED_STATIC line derives from exact pattern and actual scanned file |
| G06 | Incomplete-analysis honesty | PARTIAL scanning visible to API user, no safe-migration inference |
| G07 | Compatibility | Existing report and React flow still work; golden tests pass |
| G08 | Reproducible build | Backend verification + frontend build pass, no unrun tests claimed |
| G09 | Source control | One feature branch + reproducible commit/PR details or explicit blocker |
| G10 | Evidence | `docs/verification/b1-f02-f03-report.md` with command/test summary and residual risks |

## Failure/stop rules

**STOP** and report a blocker if a requirement would require executing untrusted code, destructive Git operations, exposing credentials, modifying original OpenRewrite baseline data, or changing the public API incompatibly without justification. Otherwise make the narrowest reversible choice, document it and continue. Never claim PASS if tests could not execute.

## Not included in this batch

No P6 deployment, P7 LLM, new product features, more migration paths, arbitrary repository ingestion, Spring stack upgrades, Gradle, persistence, auth, Kafka, Kubernetes, UI redesign. If the batch reveals a larger redesign requirement, record it as deferred architecture debt.
