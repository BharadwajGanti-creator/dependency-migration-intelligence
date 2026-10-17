# DMI B4 — Controlled Corpus and Independent Developer Review

Status: PROPOSED, next bounded batch after merged B3. This document authorizes no new external provider, public input surface or deployment.

## Copy into one fresh Cursor Agent session

You are implementing the bounded DMI B4 corpus-and-review batch in `BharadwajGanti-creator/dependency-migration-intelligence`. Fetch latest `main`, verify B3 PR #3 is merged, record HEAD SHA, and work on branch `cursor/dmi-b4-controlled-corpus`. Never commit to main or auto-merge.

Read `docs/master-plan/01_Master_Architecture_and_Decision_Record.md`, B4 section of `docs/master-plan/02_B3_to_B7_Implementation_Contracts.md`, `docs/validation/b2-results.md`, `docs/validation/b3-results.md`, current catalogue/adjudication/fixture code and tests. Read only touched source thereafter.

**Goal:** Establish whether DMI's deterministic migration guidance is correct and useful beyond the original hand-crafted fixture. Deliver controlled, reproducible corpus evidence and a developer review protocol. Avoid implying statistically representative accuracy or real external-user demand.

**First gate — architecture fit:** Inspect existing single-fixture/single-pair restrictions and propose a concise design (max 250 words) that preserves the public API allowlist. Do not silently relax validation to accept new fixture IDs or migration pairs. If supporting additional internal fixtures requires changing a runtime contract, prefer a test-only fixture/corpus execution seam. Distinguish tests from public product support. If an internal fixture cannot be exercised without a substantial refactor, document it and keep the batch narrower rather than rebuilding the scanner.

**Implementation:**
1. Create 2–4 small original, controlled, licensed/attributed migration scenarios, using the existing Spring Boot 2.7.18 → 3.2.12 pair in this batch. This is a narrower B4 scope choice, not a permanent master-plan restriction. Before supporting any second migration pair, change adjudication keys to `(migrationPair, catalogueVersion, findingId)` and obtain explicit approval. Cover a true positive, absent-pattern negative, ambiguous/review-only or informational guidance, and abstention where feasible. Include a reproducible case that OpenRewrite patches on an isolated fixture copy if feasible; otherwise document why it was NOT RUN.
2. Record a manifest per scenario: fixture ID, immutable/hashable content, origin/license, intended migration pair, reviewed expected findings/verdicts, expected OpenRewrite behavior or NOT RUN, and meaningful limitations. Keep fixtures minimal; never paste proprietary code.
3. Add an isolated test-only corpus runner and regression assertions using current deterministic scanner, classifier and review evidence. Use existing catalogue and checked-in adjudication authority; do not fabricate new official evidence. If an expected verdict depends on unverified runtime behavior, mark it UNVERIFIED rather than promoting the judgement.
4. Explicitly test cases where nothing should be reported, where a possible pattern should not become CONFIRMED_STATIC, and where OpenRewrite may address the issue. Record false positives, false negatives and abstentions per case with visible denominators and independently reviewable labels.
5. If performing OpenRewrite comparison, operate only on isolated copies of original controlled fixtures using pinned recipe/tool versions. Never execute arbitrary client or remote repositories from the API. Treat unexecuted comparisons as NOT RUN.
6. Create an independently reproducible developer evaluation protocol and feedback template (task, expected action, elapsed time, technical correctness, trust and helpfulness). Seek 3–5 consenting developer reviews if possible, as proposed in the master plan, recording actual participation and feedback. If reviewers are unavailable, mark USER REVIEW NOT RUN and do not claim human usefulness.
7. Keep all existing B1–B3 API contracts and invariants unchanged. No new endpoint, migration pair, inference, agent, database, queue, hosting, auth or unrelated UI rewrite.
8. Update documentation: corpus manifest, independent scenario-level evaluation report with expected-versus-observed results, precision/error taxonomy, documented corrections and denominators, relevant ADR if needed, and `docs/SESSION_HANDOFF.md`. Record actual reviewer participation and unresolved limitations.

**Acceptance:** Reproducible positive, negative, informational and abstention scenarios; OpenRewrite-patched example if feasible (otherwise explicit NOT RUN); independently auditable evaluation report and corrections; 3–5 consenting developer reviews if possible (otherwise explicit USER REVIEW NOT RUN); unchanged public allowlist, green backend/frontend CI, honest denominators, no general accuracy claim, one human-reviewed PR.

**Verification:** Run `cd backend && ./mvnw verify` and `cd frontend && npm ci && npm run build`. Report actual results and corresponding CI URLs. Push and open PR; do not merge. Finish with a concise human handoff: base/head SHA, changed files, cases and expected/observed results, failures, verification, limitations, and a proposed decision between further corpus expansion and a separately authorized small model trial.

**Working style:** Execute one substantive batch in one Cursor session, with minimal repeated prompts and short relevant logs. If a gate blocks safe implementation, document a bounded partial deliverable rather than expanding scope silently.
