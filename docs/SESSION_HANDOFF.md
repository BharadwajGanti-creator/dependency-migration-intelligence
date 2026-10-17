# DMI session handoff

**Last batch:** B3 grounded explanation experiment on `cursor/dmi-b3-grounded-explanations`  
**Main reference:** `c997eae` (B2 merged)  
**Verification:** `docs/validation/b3-results.md`

## What works

- **Sync API:** `POST /api/v1/analyses/sync` — unchanged; findings include optional B2 `migrationReview`.
- **Explanations API:** `POST /api/v1/analyses/explanations` — opt-in; server recomputes report; default **disabled** (`dmi.explanation.enabled=false`).
- **Trust:** static `confidence` vs reviewed `verdict` vs optional AI status (`DISABLED`, `FALLBACK`, `ACCEPTED`, …). Pathmatch stays `REVIEW_RECOMMENDED` / `NOT_PRESENT_IN_PATCH`.
- **Tests:** `cd backend && ./mvnw verify` (**56** tests)
- **UI:** sync report + optional “Request explanation” per finding (shows disabled/fallback/accepted states separately from B2 facts).

## Run locally

```bash
cd backend && DMI_FIXTURES_ROOT=/workspace/fixtures ./mvnw spring-boot:run
cd frontend && npm install && npm run dev
```

## Real-model experiment

**NOT RUN** — no external provider configured. Harness uses `deterministic-test` provider in tests only.

## Do not start without user ask

Hosted AI, auth, PostgreSQL, new fixtures/migrations, RAG, arbitrary repo ingestion.

## Fresh-chat prompt

```
Continue DMI on main after B3 PR merge. Read docs/SESSION_HANDOFF.md and docs/validation/b3-results.md. Next bounded task per user — no scope expansion.
```

## Key paths

| Path | Purpose |
|------|---------|
| `backend/.../explanation/ExplanationService.java` | Explanations orchestration |
| `backend/.../explanation/ExplanationVerifier.java` | Output validation |
| `docs/validation/b3-api-contract.md` | Explanations API contract |
| `docs/adrs/003-grounded-explanations-b3.md` | B3 architecture ADR |
| `backend/src/test/resources/validation/b3-labelled-cases.json` | 14-case verifier corpus |
