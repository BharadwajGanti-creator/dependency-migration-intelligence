# DMI session handoff

**Last batch:** B4 controlled corpus on `cursor/dmi-b4-controlled-corpus-503d`  
**Main reference:** `1a08451` (B3 merged)  
**Verification:** `docs/validation/b4-results.md`

## What works

- **Sync API:** `POST /api/v1/analyses/sync` — unchanged; still only `spring-migration-case` at runtime.
- **Explanations API:** `POST /api/v1/analyses/explanations` — unchanged; default disabled.
- **B4 corpus:** 4 test-only scenarios + manifest; regression via `ControlledCorpusRegressionTest`.
- **Trust:** static `confidence` vs reviewed `verdict`; pathmatch stays `REVIEW_RECOMMENDED`.
- **Tests:** `cd backend && ./mvnw verify` (see b4-results for count after B4).
- **UI:** unchanged from B3.

## Run locally

```bash
cd backend && DMI_FIXTURES_ROOT=/workspace/fixtures ./mvnw spring-boot:run
cd frontend && npm install && npm run dev
```

## Developer corpus evaluation

Run corpus tests only: `cd backend && ./mvnw test -Dtest=ControlledCorpusRegressionTest`

## Do not start without user ask

Hosted AI, auth, PostgreSQL, **new public fixtures/migrations**, RAG, arbitrary repo ingestion.

## Fresh-chat prompt

```
Continue DMI on main after B4 PR merge. Read docs/SESSION_HANDOFF.md and docs/validation/b4-results.md. Next bounded task per user — no scope expansion.
```

## Key paths

| Path | Purpose |
|------|---------|
| `backend/src/test/resources/corpus/manifest.json` | B4 scenario manifest |
| `backend/src/test/java/com/dmi/corpus/ControlledCorpusRegressionTest.java` | Corpus regression |
| `docs/validation/b4-results.md` | B4 evaluation report |
| `docs/adrs/004-controlled-corpus-b4.md` | Test-only corpus seam ADR |
| `docs/validation/b4-developer-review-protocol.md` | Independent review protocol |
