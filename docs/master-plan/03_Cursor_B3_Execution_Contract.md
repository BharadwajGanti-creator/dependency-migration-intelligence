# Cursor — B3 one-session execution contract

Copy this prompt into one fresh Cursor Agent session.

You are implementing B3 in `BharadwajGanti-creator/dependency-migration-intelligence`. Start from current `main`, record its HEAD SHA, and create `cursor/dmi-b3-grounded-explanations`. Never commit to `main` or auto-merge.

Read:
- `docs/master-plan/01_Master_Architecture_and_Decision_Record.md`
- `docs/master-plan/02_B3_to_B7_Implementation_Contracts.md` (B3 and A1–A9 only)
- `docs/validation/b2-api-contract.md`
- `docs/validation/b2-results.md`
- Relevant existing API/domain/adjudication/UI/tests only.

**Goal:** Implement one bounded, offline-only B3 vertical slice for optional, evidence-backed migration explanations. Preserve all B1/B2 invariants. Do not claim LLM benefit.

**Decision:** Prefer a separate `POST /api/v1/analyses/explanations` endpoint taking a validated allowlisted analysis request and selected finding IDs. Recompute/retrieve authoritative report evidence server-side; never trust client-supplied evidence, paths, URLs or report snapshots. Document any justified deviation in an ADR. Keep `POST /api/v1/analyses/sync` unchanged.

**Provider restriction:** No external or paid model calls, external inference, local model downloads, credentials or hosted endpoints are authorized. Implement a provider-neutral Java interface, disabled default and fake/test adapters. Mark real-model evaluation `NOT RUN`; no fabricated AI cost, latency, correctness or usefulness metrics.

**Build:**
1. Construct minimal immutable evidence packs with stable IDs, exact migration pair, fixture digest, static findings, reviewed verdicts, official documentation references and explicit limitations. Exclude unnecessary raw source; fail closed on unknown provenance.
2. Validate bounded structured explanation output. Reject invalid/missing citations, unsupported versions, invented source locations, test or patch outcomes, verdict contradictions and oversize/malformed content. Citations alone do not prove semantic support: use narrow checked assertions and independently reviewed labelled expected outcomes, and document verification limits.
3. Preserve `pathmatch-strategy = REVIEW_RECOMMENDED`, `NOT_PRESENT_IN_PATCH`, and unverified runtime behavior; never imply mandatory breakage or an executed verification.
4. On disabled, invalid, timed-out or unavailable model behavior, return explicitly labelled deterministic B2 fallback without losing original findings.
5. Display the opt-in explanation and its status separately from reviewed B2 facts in React; provide accessible loading, disabled and failure states.
6. Create at least 12 heterogeneous labelled cases (supported, contradictory, ambiguous, adversarial/prompt-injection), with regression/security tests. Require zero accepted *known-invalid* cases in this corpus, without treating that as a general accuracy guarantee.
7. Compare observed offline harness results with the deterministic control using exact denominators. Record acceptance/rejection/fallback and any genuinely measured metrics; mark human usefulness and real-inference results `NOT RUN` or `NOT MEASURED`.
8. Add/update ADR, test/evaluation report and `docs/SESSION_HANDOFF.md`; correct stale B2 main references there.

**Boundaries:** No arbitrary repo ingestion/execution, new migration pairs, microservices, Kafka, DB, vector store, RAG, agent framework, auth, cloud deployment or unrelated rewrites. Do not silently revise other master-plan documents.

**Execution:** One coherent session; make a concise initial architecture choice and proceed with permitted implementation. Avoid repeated micro-prompts or large log dumps. Do not mask failing tests.

**Verification and delivery:** Run `cd backend && ./mvnw verify` and `cd frontend && npm ci && npm run build`. Record actual test counts and results; push one branch and open one human-review PR. Do not merge. End with a concise report: base/head SHA, changes, test/CI links and status, accepted/rejected example cases, denominators, limitations, real-model evaluation `NOT RUN`, and unresolved risks.
