# B2 validation results — product value & explainable evidence

**Branch:** `cursor/dmi-b2-value-validation-f989`  
**Base:** `main` @ `f39d7d9` (includes B2 planning docs)  
**Batch date:** 2026-10-17

## V01 — Baseline commands (VERIFIED)

Isolated fixture copy (repo fixture unchanged):

```bash
cp -a fixtures/spring-migration-case /tmp/dmi-b2-fixture
/workspace/backend/mvnw -f /tmp/dmi-b2-fixture/pom.xml -q validate
/workspace/backend/mvnw -f /tmp/dmi-b2-fixture/pom.xml -U \
  org.openrewrite.maven:rewrite-maven-plugin:6.45.0:dryRun \
  -Drewrite.recipeArtifactCoordinates=org.openrewrite.recipe:rewrite-spring:6.9.0 \
  -Drewrite.activeRecipes=org.openrewrite.java.spring.boot3.UpgradeSpringBoot_3_2
```

**Observed:** BUILD SUCCESS; `target/rewrite/rewrite.patch` **69 lines**; no `pathmatch` / `application.properties` hunks (confirmed via `rg -i pathmatch` on patch). Patch excerpt committed: `docs/validation/b2-openrewrite-patch-head.txt`.

## V02 — `pathmatch-strategy` (VERIFIED docs, UNVERIFIED runtime)

| Evidence | Proves |
|----------|--------|
| [Preparing for Spring Boot 3.0](https://spring.io/blog/2022/05/24/preparing-for-spring-boot-3-0) | Boot 3 default is PathPatternParser; explicit `ant_path_matcher` still works but is legacy-oriented |
| [MatchingStrategy API 3.2.7](https://docs.spring.io/spring-boot/docs/3.2.7/api/org/springframework/boot/autoconfigure/web/servlet/WebMvcProperties.MatchingStrategy.html) | `ANT_PATH_MATCHER` remains a supported enum value on Boot 3.2.x |
| OpenRewrite dryRun above | **NOT_PRESENT_IN_PATCH** — absence is **not** proof of missed required fix |

**Adjudicated verdict:** `REVIEW_RECOMMENDED` (not `ACTION_REQUIRED`). **Runtime URL-matching probe on Boot 3.2.12:** NOT EXECUTED (UNVERIFIED for behavioral regressions).

## V03 — Finding verdicts (catalogue ids)

| Finding ID | Static confidence (fixture) | Review verdict | OpenRewrite comparison |
|------------|------------------------------|----------------|------------------------|
| `jakarta-servlet-api` | CONFIRMED_STATIC | ACTION_REQUIRED | PATCHED_IN_BASELINE |
| `jakarta-annotation-postconstruct` | CONFIRMED_STATIC | ACTION_REQUIRED | PATCHED_IN_BASELINE |
| `pathmatch-strategy` | CONFIRMED_STATIC | REVIEW_RECOMMENDED | NOT_PRESENT_IN_PATCH |
| `parent-version-bump` | POSSIBLE | ACTION_REQUIRED | PATCHED_IN_BASELINE |

Dataset: `docs/validation/b2-adjudications.json`.

## V04 — OpenRewrite comparison interpretation (VERIFIED)

Documented **what the executed dryRun patched** (parent, java.version, javax→jakarta servlet/annotation). Did **not** claim OpenRewrite is incapable of property migrations beyond tested recipe/run mode.

## V05–V06 — API/UI (VERIFIED)

- Optional `finding.migrationReview` documented in `docs/validation/b2-api-contract.md`.
- UI (`FindingList`) shows separate badges for static confidence vs reviewed verdict, OpenRewrite comparison, citations, UNVERIFIED banner.

## V07–V08 — Labelled cases & tests

| Case | Expected | Result |
|------|----------|--------|
| Positive: fixture `jakarta-servlet-api` import | CONFIRMED_STATIC + review ACTION_REQUIRED | PASS (integration test) |
| Positive: fixture `pathmatch-strategy` property | CONFIRMED_STATIC + REVIEW_RECOMMENDED | PASS (integration test) |
| Negative: comment-only `javax.servlet` (unit) | POSSIBLE static, not CONFIRMED_STATIC | PASS (`CommentOnlyFindingTest`) |
| Negative: unknown finding id adjudication | UNVERIFIED review | PASS (`AdjudicationRegistryTest`) |
| Negative: pathmatch must not be ACTION_REQUIRED | REVIEW_RECOMMENDED | PASS (registry + integration) |
| Invalid verdict enum / duplicate ids | startup/test rejection | PASS (`AdjudicationRegistryTest`) |

**Metrics (denominator = controlled cases above):** actionable review alignment **2/2** positives; false ACTION_REQUIRED on pathmatch **0/1**; UNVERIFIED fallback **1/1**; invalid dataset rejected **2/2**.

## V09 — Build verification (VERIFIED)

| Command | Result |
|---------|--------|
| `cd backend && ./mvnw verify` | **PASS — 40 tests**, 0 failures |
| `cd frontend && npm ci && npm run build` | **PASS** |

**CI:** `.github/workflows/ci.yml` — PR #2 checks **4/4 SUCCESS** (backend + frontend verify/build on `ubuntu-latest`, Java 21, Node 22). Local runs above match CI commands.

## V10 — Documentation updates

- `docs/SESSION_HANDOFF.md` → B2 handoff for B3
- `docs/verification/b1-f02-f03-report.md` — B1 merge status corrected
- Stale test counts corrected (33/28 → post-B1 **33** on main; B2 **40**)

## Unresolved / UNVERIFIED

- Boot 3.2 **runtime** MVC behavior with `ant_path_matcher` on this fixture (no app start in batch).
- Whether other OpenRewrite recipes or `rewrite:run` (non-dryRun) would modify pathmatch property.
- End-user manual UI smoke: no frontend unit harness (build-only).

## Decision note (B3 threshold)

At least one **reproduced, useful** decision: explicit `ant_path_matcher` surfaced with **REVIEW_RECOMMENDED** and OpenRewrite gap labeled **NOT_PRESENT_IN_PATCH** without equating to breakage — supports human review beyond patch-only tooling.
