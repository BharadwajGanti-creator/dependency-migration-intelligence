# B4 validation results — controlled corpus and developer review

**Branch:** `cursor/dmi-b4-controlled-corpus-503d`  
**Base:** `main` @ `1a08451` (B3 PR #3 merged @ 2026-10-17)  
**Batch date:** 2026-10-17  

## Architecture gate (VERIFIED)

Public allowlist unchanged: `FixtureRegistry` accepts only `spring-migration-case`. Corpus scenarios live under `backend/src/test/resources/corpus/` and run via test-only materialization (`ControlledCorpusSupport`). See ADR-004.

## Corpus manifest (VERIFIED)

| Scenario ID | Label | contentSha256 (prefix) | OpenRewrite (isolated dryRun) |
|-------------|-------|------------------------|-------------------------------|
| `b4-scenario-jakarta-servlet-tp` | true_positive | `28fb3d6f…` | **VERIFIED** — patches pom + javax→jakarta in `ServletStub.java` (54-line patch) |
| `b4-scenario-no-static-signals` | absent_pattern_negative | `ad2d9303…` | **NOT RUN** (no javax signals to compare) |
| `b4-scenario-pathmatch-review` | review_only_informational | `d73de84d…` | **VERIFIED** — parent/java bump only; **no** `pathmatch` hunk (23-line patch) |
| `b4-scenario-comment-abstention` | abstention_comment_only | `d6685148…` | **NOT RUN** |

Reference public fixture: `fixtures/spring-migration-case` (existing golden/integration tests; not duplicated in corpus tree).

Full manifest: `docs/validation/b4-corpus-manifest.json`

## Expected vs observed (deterministic pipeline)

Denominator: **4 corpus scenarios**, **11 labelled outcome assertions** in regression tests (`ControlledCorpusRegressionTest`).

| Scenario | Finding | Expected | Observed | Taxonomy |
|----------|---------|----------|----------|----------|
| jakarta-servlet-tp | jakarta-servlet-api | CONFIRMED_STATIC + ACTION_REQUIRED | Match | true_positive |
| jakarta-servlet-tp | jakarta-annotation-postconstruct | NOT_ASSESSED | Match | true_negative_absence |
| no-static-signals | jakarta-servlet-api | NOT_ASSESSED, not CONFIRMED_STATIC | Match | true_negative |
| no-static-signals | parent-version-bump | POSSIBLE + ACTION_REQUIRED | Match | informational_doc_only |
| pathmatch-review | pathmatch-strategy | CONFIRMED_STATIC + REVIEW_RECOMMENDED (not ACTION_REQUIRED) | Match | informational_review |
| comment-abstention | jakarta-servlet-api | POSSIBLE, not CONFIRMED_STATIC | Match | abstention |

**Documented correction / limitation:** Adjudicated `migrationReview.verdict` remains on all catalogue findings even when static `confidence` is `NOT_ASSESSED` or `POSSIBLE`. B4 assertions focus on confidence escalation rules and review-only verdict separation for pathmatch — not on suppressing catalogue entries.

## Precision / error taxonomy (exploratory, n=4 scenarios)

| Category | Count | Denominator | Notes |
|----------|-------|-------------|-------|
| True positive (static + adjudication align) | 1 | 4 scenarios | servlet-tp |
| True negative (no CONFIRMED_STATIC on absent patterns) | 2 | 4 | no-static-signals + partial absences |
| Informational / review-only | 1 | 4 | pathmatch-review |
| Abstention (POSSIBLE not CONFIRMED) | 1 | 4 | comment-only |
| False CONFIRMED_STATIC in negative scenarios | 0 | 3 negative checks | regression enforced |
| False ACTION_REQUIRED on pathmatch | 0 | 1 | REVIEW_RECOMMENDED held |

**No general accuracy claim.** Corpus size is intentionally tiny and Spring Boot 2.7.18→3.2.12 only.

## OpenRewrite comparison (VERIFIED where run)

Pinned: plugin `6.45.0`, `rewrite-spring:6.9.0`, recipe `UpgradeSpringBoot_3_2` (same as F07 baseline).

- **b4-scenario-jakarta-servlet-tp:** isolated copy under `/tmp/dmi-b4-or-tp`; patch includes jakarta servlet migration — aligns with `PATCHED_IN_BASELINE` expectation in manifest.
- **b4-scenario-pathmatch-review:** patch excludes `ant_path_matcher` property — aligns with `NOT_PRESENT_IN_PATCH`.

## Developer review (USER REVIEW NOT RUN)

No consenting external reviewers completed the protocol in this batch. Templates and tasks are ready:

- `docs/validation/b4-developer-review-protocol.md`
- `docs/validation/b4-developer-feedback-template.md`

Do **not** infer human usefulness from this batch alone.

## B1–B3 invariants (VERIFIED)

- Public endpoints unchanged; no new fixture IDs or migration pairs in runtime registry.
- Explanations remain opt-in/disabled by default (no B4 code changes to explanation stack).
- Existing integration/golden tests retained.

## Verification commands

| Command | Result |
|---------|--------|
| `cd backend && ./mvnw verify` | PASS — **63** tests, 0 failures |
| `cd frontend && npm ci && npm run build` | PASS |

## Proposed next decision

1. **Further corpus expansion** (more scenarios, optional second pair only after adjudication key migration), **or**
2. **Separately authorized small model trial (B3 A7)** with real provider credentials and pre-set thresholds.

Recommend: complete **1–2 consenting developer reviews** using the B4 protocol before either path; if reviews find static/adjudication confusion, expand corpus labels before enabling hosted AI.
