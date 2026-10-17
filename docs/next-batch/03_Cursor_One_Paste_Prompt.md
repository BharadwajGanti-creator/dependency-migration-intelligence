# One-paste Cursor Agent prompt — B2 (fresh chat in existing repository)

You are implementing **DMI B2 — Product Value Validation & Explainable Migration Evidence** in the existing GitHub repository, starting from updated `main` after B1 PR merge. This is **one bounded full vertical implementation batch**, not a sequence of speculative mini-prompts.

Read `docs/next-batch-b2/01_B2_Product_and_Architecture.md` and `docs/next-batch-b2/02_B2_Implementation_and_Acceptance.md` once. Inspect only the relevant existing code and docs. Check `git status`, branch, main ancestry; create a B2 feature branch. Do not regenerate the app or open new repositories.

**Goal:** Establish evidence-backed, version-specific migration review verdicts and OpenRewrite comparison for the existing Spring Boot 2.7.18→3.2.12 fixture. Deliver structured adjudications, backward-compatible API presentation, useful React UI, controlled positive/negative cases, reproducible validation evidence and tests. Carefully distinguish a regex match from a verified necessary migration action.

**Workflow:**
1. Confirm code/CI baseline and B1 merge. Fix stale `SESSION_HANDOFF.md` and B1 report during this batch.
2. Investigate official version-relevant migration docs and existing OpenRewrite baseline. Reproduce `pathmatch-strategy` behavior in a safe isolated copy if possible. Never infer an incompatibility from its absence in rewrite.patch.
3. Author a reviewed adjudication dataset. Mark gaps `UNVERIFIED` rather than inventing facts.
4. Implement minimal deterministic domain/API mapping + frontend evidence/limitations presentation. Preserve existing contract.
5. Add targeted positive and negative tests and run complete backend/frontend builds.
6. Record actual commands, versions, evidence, small labelled-case metrics, limitations and source links in `docs/validation/b2-results.md`. Commit and push the B2 branch; create a draft PR if permissions allow; never merge automatically.

**Context / cost discipline:** target <=20K active context tokens; warn at 25K, stop with a handoff at 35K if observable. If Cursor does not expose usage report `NOT AVAILABLE` and do not claim monitoring. Keep command outputs short, use targeted search, don't read entire files repeatedly, don't spawn subagents, don't re-open planning repeatedly. Complete the batch before asking broad product-preference questions; ask only if blocked by an indispensable decision. Retain only relevant docs and changed files in context.

**Stop conditions:** If official evidence is ambiguous, use UNVERIFIED and preserve uncertainty. No arbitrary repo ingestion, API-driven Maven execution, LLM, vector DB, hosting, new migration pairs, database, authentication, or UI redesign.

**Report (<=500 words):** branch/HEAD, exact backend test count, frontend build, CI status with evidence, versioned experiment outcomes, a verdict for each existing finding, actual labelled-case metrics, changed files, unresolved risks, and PR URL. Separate VERIFIED from PLANNED/UNVERIFIED. STOP and await human PR review.
