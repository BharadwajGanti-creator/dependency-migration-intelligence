# DMI session handoff

**Last verified commit:** `5ce40ea` (audit re-run on same tree; post-audit doc/test commit pending push)  
**Product scope:** MVP only — synthetic fixture, no LLM, no hosting, no DB.

## What works

- **API:** `POST /api/v1/analyses/sync` on `:8787` with fixture `spring-migration-case`, parent `spring-boot-starter-parent` 2.7.18→3.2.12.
- **UI:** Vite portfolio `/` + analyzer `/analyze` on `:4321` (proxies API).
- **Tests:** `cd backend && ./mvnw verify` — 9 tests (add +1 unsupported migration after this handoff commit).
- **CI:** `.github/workflows/ci.yml` — backend verify + frontend build.

## Architecture checkpoints

- Parent coordinate via `MavenManifestReader.resolveParent()` — not treated as ordinary `<dependency>`.
- Findings from **catalogue JSON + scanner matches**, not hard-coded report IDs (`AnalysisService` → scanner → `FindingClassifier`).
- Findings sorted by `id`; limitations always included.
- **Not production:** localhost health/preview only.

## F07 baseline (done)

See `docs/baseline/openrewrite-comparison.md`. OpenRewrite dryRun **PASS** with plugin `6.45.0`, `rewrite-spring:6.9.0`.  
Incremental value: doc citations + `pathmatch-strategy` finding absent from rewrite.patch.

## Known defects / debt (low)

- `FindingClassifier.java:30` — redundant double `Pattern.compile`.
- No test asserting exact source line snapshot against `expected-findings.json`.
- OpenRewrite version pins differ from playbook doc (environment-specific).

## Do not start without user ask

- P6 hosting, P7 LLM, auth, PostgreSQL, extra fixtures, Gradle, arbitrary GitHub URLs.

## Fresh-chat starter prompt

```
Continue DMI on main. Read docs/SESSION_HANDOFF.md and docs/baseline/openrewrite-comparison.md only.
Next bounded task: [F02 edge cases OR validation gate review]. No scope expansion.
Run: backend ./mvnw verify; frontend npm run build.
```

## Key paths

| Path | Purpose |
|------|---------|
| `backend/.../AnalysisService.java` | Orchestration |
| `backend/src/main/resources/catalogue/spring-boot-2-to-3.json` | Rules + doc links |
| `fixtures/spring-migration-case/` | Allowlisted target |
| `docs/baseline/expected-findings.json` | Golden IDs/lines |
