# F07 OpenRewrite baseline comparison

**Status:** PASS (dryRun executed 2026-10-17)  
**Fixture copy:** `/tmp/dmi-f07-fixture` (isolated from repo; not committed)

## Commands (exact)

```bash
cp -a fixtures/spring-migration-case /tmp/dmi-f07-fixture
/workspace/backend/mvnw -f /tmp/dmi-f07-fixture/pom.xml -q validate

/workspace/backend/mvnw -f /tmp/dmi-f07-fixture/pom.xml -U \
  org.openrewrite.maven:rewrite-maven-plugin:6.45.0:dryRun \
  -Drewrite.recipeArtifactCoordinates=org.openrewrite.recipe:rewrite-spring:6.9.0 \
  -Drewrite.activeRecipes=org.openrewrite.java.spring.boot3.UpgradeSpringBoot_3_2
```

**Note:** Plugin `6.50.0` and `rewrite-spring:6.40.0` are **absent** on Maven Central in this environment; `6.45.0` + `6.9.0` resolved successfully.

**Output artifact:** `/tmp/dmi-f07-fixture/target/rewrite/rewrite.patch` (69 lines)  
**Full log:** `/tmp/f07-dryrun.log`

## OpenRewrite would change

| Area | Change |
|------|--------|
| `pom.xml` | Parent `2.7.18` → `3.2.12`; `java.version` 11 → 17 |
| `pom.xml` | `javax.servlet-api` → `jakarta.servlet-api` |
| `LegacyServletFilter.java` | `javax.servlet.*` → `jakarta.servlet.*` imports |
| `StartupHook.java` | `javax.annotation.PostConstruct` → `jakarta.annotation.PostConstruct` |

**Not in patch:** `application.properties` (`spring.mvc.pathmatch.matching-strategy=ant_path_matcher` unchanged by this dryRun).

## DMI vs OpenRewrite vs docs

| DMI finding | OpenRewrite | Incremental? |
|-------------|-------------|--------------|
| `jakarta-servlet-api` | Yes (imports + POM dep) | **Citations + line evidence + grouped report** |
| `jakarta-annotation-postconstruct` | Yes | Same |
| `pathmatch-strategy` | **No patch hunk** | **Yes — static flag OpenRewrite dryRun did not surface here** |
| `parent-version-bump` | Yes (parent/java props) | DMI is doc-only POSSIBLE; OR is actionable diff |

## Verified incremental value

1. Curated **documentation URLs/sections** per finding (OpenRewrite patch has none).  
2. **`pathmatch-strategy`** with file:line in `application.properties` (not in rewrite.patch).  
3. Explicit **confidence** and **limitations** (no “safe to merge” claim).

## False positives / gaps

- `jakarta-servlet-api` lists multiple lines per filter import (accurate but noisy).  
- `parent-version-bump` is POSSIBLE without source pattern (by design).  
- DMI does not emit a unified diff like OpenRewrite.

## Unresolved

- Whether `SpringBootProperties_3_2` would touch pathmatch on `rewrite:run` (not dryRun-verified).  
- Manual adjudication still required for validation gate.
