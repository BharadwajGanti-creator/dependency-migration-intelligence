# DMI session handoff

**Last batch:** B1 F02/F03 evidence integrity on `feat/dmi-evidence-integrity`  
**Main reference:** `c48f6e8` (specs) → feature branch HEAD after B1 commit  
**Verification:** `docs/verification/b1-f02-f03-report.md`

## What works

- **API:** `POST /api/v1/analyses/sync` — parent upgrade 2.7.18→3.2.12 on `spring-migration-case`
- **Trust improvements:** direct-child Maven parse, ScanResult COMPLETE/PARTIAL, pattern-key classifier, bounded digest v2
- **Tests:** `cd backend && ./mvnw verify` (21 tests)
- **UI:** `cd frontend && npm run build`

## Run locally

```bash
cd backend && DMI_FIXTURES_ROOT=/workspace/fixtures ./mvnw spring-boot:run
cd frontend && npm install && npm run dev
```

## F07 baseline (unchanged)

OpenRewrite dryRun PASS documented in `docs/baseline/openrewrite-comparison.md`. **`pathmatch-strategy` differentiation not fully validated.**

## Do not start without user ask

P6 hosting, P7 LLM, auth, PostgreSQL, new fixtures/migrations, Gradle, arbitrary GitHub URLs.

## Fresh-chat prompt

```
Continue DMI on feat/dmi-evidence-integrity (or main after merge). Read docs/SESSION_HANDOFF.md and docs/verification/b1-f02-f03-report.md. Next bounded task per user — no scope expansion.
```

## Key paths

| Path | Purpose |
|------|---------|
| `backend/.../MavenManifestReader.java` | F02 parser + diagnostics |
| `backend/.../SourceEvidenceScanner.java` | F03 ScanResult |
| `backend/.../FindingClassifier.java` | Pattern-key matching |
| `docs/next-batch/` | Batch specs 01–03 |
| `docs/adrs/002-evidence-integrity-b1.md` | B1 ADR |
