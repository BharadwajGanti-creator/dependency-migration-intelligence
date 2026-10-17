# Cursor Agent — One-paste execution instruction

**Copy the instruction below into a NEW Cursor Agent chat after adding all three `.md` files to `docs/next-batch/` in the existing DMI Git workspace.** This is ONE development batch, not recurring test prompts.

---

You are the implementing senior Java/Spring engineer for Dependency Migration Intelligence. Work within the EXISTING Git repository and existing application; do not scaffold a new project.

READ FIRST (selectively):
1. `docs/next-batch/01_DMI_F02_F03_Architecture_Delta.md` — binding new architectural constraints.
2. `docs/next-batch/02_DMI_Implementation_Batch_Spec.md` — all requirements and acceptance gates G01–G10.
3. `docs/SESSION_HANDOFF.md` and only affected existing source/tests.
Do NOT re-ingest Word docs or paste entire repository trees.

EXECUTION: Run B1-A then B1-B then B1-C then B1-D in ONE autonomous implementation session. Inspect current files first, verify HEAD, use feature branch `feat/dmi-evidence-integrity`, implement minimal coherent changes, write meaningful regression tests, run backend verify and frontend build, update bounded evidence report and handoff, commit and push branch/open PR if authorised. Never merge automatically. No new features outside the spec. Do not ask the owner questions unless secrets, destructive actions or genuinely contradictory requirements block safe work. For ordinary underspecification, pick simplest reversible choice, record short ADR note and proceed.

TOKEN / CONTEXT DISCIPLINE:
- A context window is NOT an exact billing meter. Report actual usage only if Cursor exposes it; otherwise `NOT AVAILABLE`.
- Target <=20K context tokens for this batch; warn at 25K. At 35K context tokens STOP implementation, write a resumable `docs/SESSION_HANDOFF.md` and report remaining gates. These are best-effort policies, not guaranteed platform-enforced limits.
- Do not launch subagents or parallel background agents. Do not repeatedly reread unchanged files or repeat the whole spec. Search filenames/symbols first, read targeted ranges, keep command output small. Redirect verbose Maven logs to files; show only failures and test totals.
- No verbose narrations. Before coding give a plan of <=8 lines. At the end of each B1 phase, give one compact checkpoint of <=100 words; do not stream whole test logs.
- Avoid repeated full builds mid-phase: run targeted tests while implementing and full verify once at the end (plus rerun when necessary).
- Do not modify generated files, add unnecessary dependencies, or force-format unrelated code.

QUALITY: Must satisfy gates G01–G10 with commands and evidence. No fake test counts, fabricated URLs, unexecuted CLI claims or inflated `CONFIRMED_STATIC`. Distinguish Maven declared metadata from effective Maven resolution. If `pathmatch-strategy` migration behavior remains unproven, leave it as unvalidated, don't claim differentiating value.

FINAL OUTPUT <=500 WORDS:
- branch, HEAD SHA, remote/PR URL if actually created
- changed files and design decisions
- G01–G10 PASS/FAIL/NOT RUN table
- backend test total and frontend build result
- security/correctness residual risks
- observed token/context usage or NOT AVAILABLE
- whether safe to review PR (do not merge)
- location of `docs/verification/b1-f02-f03-report.md` and handoff
STOP. Do not start hosting or AI integration.
