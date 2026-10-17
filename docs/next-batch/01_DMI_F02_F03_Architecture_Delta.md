# DMI — Architecture Delta: Trusted Maven & Source Evidence

**Status:** PROPOSED for next development batch; not implemented. **Basis:** inspected public GitHub `BharadwajGanti-creator/dependency-migration-intelligence`, main (17 Oct 2026), plus uploaded Architecture v0.1 and Cursor playbook. **Scope:** F02 Maven-declaration integrity and F03 bounded source-scan integrity, with only necessary F05/report fixes. Preserve existing single-fixture, Maven parent upgrade demo (`2.7.18 → 3.2.12`).

## 1. Why this batch exists

The current `MavenManifestReader` uses `getElementsByTagName("dependency")` and accepts any immediate `dependencies` parent, including dependency-management, profiles or plugin-related subtrees. `readProperties` and `readParent` also use descendant searches. Unresolved `${...}` may remain a raw string. `AnalysisService` checks only the parent and does not exercise direct-dependency extraction through the API. The current scanner returns only `List<ScanMatch>`, so skipped or unreadable files can be indistinguishable from a clean no-match scan. It also walks before accounting for bounds; `FixtureRegistry.digest` walks/reads all regular files without applying the scanner's limits. Findings are re-matched against *excerpts* in `FindingClassifier`, rather than respecting the already matched pattern ID. These are source-code observations, not claims of demonstrated exploits.

## 2. Decision and constraints

- Continue **modular monolith**, existing `POST /api/v1/analyses/sync` contract, React UI, no database, no agent/LLM, no arbitrary URLs, no shelling out to Maven from the API.
- One synthetic allowlisted fixture remains the only runtime input. Add temporary **test-only POM fixtures**, not new supported user migrations.
- Explicitly separate Maven **parent**, **direct dependencies**, **dependencyManagement**, **profiles**, and **plugin dependencies**. Only root `/project/dependencies/dependency` is a direct declaration. Do not claim an effective Maven dependency graph, BOM resolution, inheritance, or transitive resolution.
- Preserve external `AnalysisReport` JSON shape when possible. Encode incomplete scans and unresolved declarations as explicit, human-readable `limitations`; do not mark absence of matches as proof of compatibility.
- Treat `CONFIRMED_STATIC` as confirmation of the static signal and source line only—not verified incompatibility, runtime breakage or recommended change.
- Prefer small typed results (`ManifestReadResult` or expanded `MavenManifest`; `ScanResult` with matches and completeness/reasons). Do not create interfaces, generic frameworks or public APIs without clear need.
- No network, subprocess or arbitrary fixture execution during request processing; strict XML parser configuration; no source content in logs.

## 3. Contracts

### Maven reader

`read(Path)` returns parent information, root direct dependencies, properties and **explicit unresolved/ambiguous declaration diagnostics**. Preserve the current parent-version path. Resolve a single root-level `${property}` reference only if unambiguous and available from root `/project/properties`; do not recursively resolve arbitrary nested or inherited properties. Detect cycles/nesting and label unresolved rather than guessing. Missing direct dependency version means `MANAGED_OR_UNKNOWN` (not version `""`). Duplicate same group/artifact direct declarations: emit diagnostic; never silently choose a winner. Missing/invalid mandatory coordinates: diagnostic or safe 422 depending on whether the target coordinate itself is unreadable. Reject DOCTYPE/XXE, non-well-formed XML and oversized POM. Do not accidentally read `dependencyManagement` declarations as direct dependencies.

### Scanner

A `ScanResult` must include matches and completeness state: `COMPLETE` or `PARTIAL`, plus stable diagnostic codes and sanitized relative paths where useful. Example codes: `FILE_LIMIT_REACHED`, `DEPTH_LIMIT_REACHED`, `BYTE_LIMIT_REACHED`, `OVERSIZED_FILE_SKIPPED`, `UNREADABLE_FILE`, `INVALID_ENCODING`, `SYMLINK_SKIPPED`, `WALK_FAILED`. No absolute host paths in API errors. Capping by depth must not silently imply all files were searched. Treat symlinks conservatively: skip even in-root symlinks unless explicitly justified with tests. Use try-with-resources for walks. Validate canonical allowlisted fixture root before access; avoid TOCTOU claims (basic path validation is not a sandbox).

### Classifier

Consume **exact matched pattern identities** associated with each rule; avoid secondary regexp matching against trimmed excerpts. Eliminate redundant `Pattern.compile`. A static hit may be reported `CONFIRMED_STATIC` for signal presence; skipped/unreadable inputs prohibit blanket no-match conclusions. Catalogue-only entries remain `POSSIBLE`, but wording must not assert migration breakage from the documentation alone. Stable location ordering and deduplication.

### Digest

Make fixture hashing deterministic and bounded, or fail closed on an invalid/oversized/unsafe fixture. Never hash unrestricted contents before enforcing limits. Hash sorted relative path + file contents with unambiguous framing; use an explicit policy for skipped files. Existing bundled fixture must retain a usable stable digest within a given algorithm version (a changed digest algorithm may alter the value; document it).

## 4. ADR matrix

| Decision | Chosen | Alternative rejected | Trade-off / promotion criterion |
|---|---|---|---|
| XML parsing | Standard secure JAXP DOM with direct-child navigation | Maven Model Builder / shell Maven | Limited effective-POM fidelity; add Maven model only with demonstrated user need and security design |
| Property lookup | Root properties; bounded one-step resolution; diagnostics otherwise | Recursive resolver and inherited BOM | Honest partiality; add advanced resolver only with fixture tests and correct precedence |
| Scanner result | Typed matches + completeness diagnostics | Plain `List` | Slight DTO complexity, essential absence-of-evidence semantics |
| Filesystem | Allowlisted, bounded, no symlink traversal | Arbitrary repo scanning | Smaller product surface, safer reproducibility |
| AI | Deferred | LLM explanations in this batch | No value case until trustworthiness and baseline proof |
| Public API | Backward compatible report + limitations | New versioned API | Minimal UI churn; revisit if typed completeness is needed by consumers |

## 5. Safety and quality invariants

1. Any confirmed static finding must map to a real line in a scanned fixture file and documented source rule.
2. No indication of *safe migration* from a missing match.
3. XXE, path escaping, symlinks, scan caps and malformed input cannot cause silent unrestricted reading.
4. Parent coordinates remain distinct from regular dependencies.
5. All tests use synthetic local fixtures; no remote repository fetching.
6. Existing API response structure and golden findings remain stable unless a documented correctness fix requires a change.
7. Every behavior that differs from v0.1 is recorded in `docs/implementation-decisions.md`.

## 6. Exclusions

No fresh architecture rewrite; no Gradle, multi-module Maven model, arbitrary GitHub URLs, auth, PostgreSQL, vector DB, embeddings, agents, OpenRewrite invocation inside the API, cloud hosting, new migration pairs or vanity portfolio changes. Existing OpenRewrite comparison remains an external baseline. DMI's market differentiation and `pathmatch-strategy` correctness remain **not fully validated**.
