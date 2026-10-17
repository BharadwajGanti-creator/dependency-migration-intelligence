# B4 independent developer evaluation protocol

**Purpose:** Exploratory usefulness signal for deterministic DMI guidance — **not** a representative accuracy study.

## Eligibility

- Consenting developer with Java/Spring Boot familiarity (any level).
- No compensation required; record only voluntary feedback submitted via the template.
- Reviewer must not be the batch author if independent bias is required (ideal).

## Session setup (≈30–45 minutes)

1. Clone repo at the B4 PR head SHA; read `docs/validation/b4-results.md` **after** attempting tasks blind if possible.
2. Start backend: `cd backend && DMI_FIXTURES_ROOT=/path/to/repo/fixtures ./mvnw spring-boot:run`
3. Optional UI: `cd frontend && npm run dev`
4. Use only `spring-migration-case` via API or UI (public allowlist unchanged).

## Tasks

| # | Task | Expected reviewer action |
|---|------|---------------------------|
| T1 | Run sync analysis for Boot 2.7.18→3.2.12 on `spring-migration-case` | Read findings; note which require code vs review-only |
| T2 | Inspect `pathmatch-strategy` finding | Decide if ant_path_matcher is acceptable; compare to OpenRewrite note in report/review |
| T3 | Inspect `jakarta-servlet-api` finding | Locate cited source lines; judge whether ACTION_REQUIRED matches evidence |
| T4 | (Optional) Request explanation with default config | Confirm disabled/fallback behavior does not alter findings |
| T5 | Export mental checklist: what would you do before merging Boot 3? | Compare to DMI `proposedChecks` |

## Metrics to capture (template)

Use `docs/validation/b4-developer-feedback-template.md` per reviewer.

## Ethics / data handling

- Do not submit proprietary repository code.
- Feedback stored only in project validation docs or attached anonymized forms; no PII required.

## Batch status

See `docs/validation/b4-results.md` for **USER REVIEW NOT RUN** vs actual participation counts.
