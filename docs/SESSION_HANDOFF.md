# DMI session handoff

**Last batch:** B2 product value validation on `feat/dmi-b2-value-validation`  
**Main reference:** `f39d7d9` (B2 specs) + B1 merge `82a2737`  
**Verification:** `docs/validation/b2-results.md`

## What works

- **API:** `POST /api/v1/analyses/sync` — unchanged request; findings include optional `migrationReview` (adjudication + OpenRewrite comparison).
- **Adjudications:** `docs/validation/b2-adjudications.json` (4 reviewed finding ids; unknown → `UNVERIFIED`).
- **Trust:** static `confidence` separate from reviewed `verdict`; pathmatch is `REVIEW_RECOMMENDED`, not auto ACTION_REQUIRED.
- **Tests:** `cd backend && ./mvnw verify` (**40** tests)
- **UI:** `cd frontend && npm run build` — finding cards show review evidence and limitations.

## Run locally

```bash
cd backend && DMI_FIXTURES_ROOT=/workspace/fixtures ./mvnw spring-boot:run
cd frontend && npm install && npm run dev
```

## OpenRewrite baseline

Offline dryRun documented in `docs/baseline/openrewrite-comparison.md` and reproduced in `docs/validation/b2-results.md`. **`pathmatch-strategy`:** docs-backed review flag; runtime MVC probe still **UNVERIFIED**.

## Do not start without user ask

P6 hosting, P7 LLM, auth, PostgreSQL, new fixtures/migrations, Gradle, arbitrary GitHub URLs.

## Fresh-chat prompt

```
Continue DMI on main or feat/dmi-b2-value-validation after B2 PR merge. Read docs/SESSION_HANDOFF.md and docs/validation/b2-results.md. Next bounded task per user — no scope expansion.
```

## Key paths

| Path | Purpose |
|------|---------|
| `backend/.../validation/AdjudicationRegistry.java` | Loads reviewed adjudications |
| `backend/.../validation/MigrationReviewEnricher.java` | Attaches `migrationReview` to findings |
| `docs/validation/b2-adjudications.json` | Reviewed verdict dataset |
| `docs/validation/b2-api-contract.md` | API additive contract |
| `docs/next-batch/01_B2_*.md` | B2 product/spec (planning) |
