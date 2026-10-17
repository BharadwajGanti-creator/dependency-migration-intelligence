# Cursor — B3 one-session execution contract (copy into a fresh Cursor Agent chat)

You are working in the existing `dependency-migration-intelligence` GitHub repository. B1 and B2 are merged into `main`. Read `docs/master-plan/01_Master_Architecture_and_Decision_Record.md`, `docs/master-plan/02_B3_to_B7_Implementation_Contracts.md`, relevant existing B2 API contracts/adjudication registry, and touched source files ONLY. Verify branch state first. Work on a new `cursor/dmi-b3-grounded-explanations` branch; never overwrite main or auto-merge.

TASK: Deliver the complete B3 grounded-explanation experiment specified in the B3 section of the implementation contracts, as one reviewed vertical slice. Preserve deterministic report functionality and fallback. No arbitrary repo inputs, RAG/vector DB, LangGraph/agents, authentication, Kafka or hosting.

FIRST DECISION GATE: Inspect existing app boundary and propose in ≤250 words the explanation endpoint choice, provider-neutral adapter, data minimization and validation strategy. If there is no approved provider, build the disabled default, a fake/mock adapter for negative tests, and an evaluation harness. Do not secretly call an external LLM; mark real-model evaluation NOT RUN. Ask the owner only if enabling a paid/external model would be required.

IMPLEMENTATION:
1. Evidence pack keyed by exact migration pair, finding ID and provenance IDs; no fabricated docs.
2. Provider-neutral Java adapter, disabled-by-default implementation and deterministic fallback. No external call without authorization.
3. Strict structured output + validation of evidence IDs, versions, verdict consistency, dangerous/untrusted instructions, output limits. Verify semantic support through labelled checks; explicitly acknowledge that citations alone do not prove support.
4. Additive optional API behavior; keep existing analysis contract and HTTP errors stable. UI clearly distinguishes reviewed factual summary and optional AI explanation.
5. Build a labelled evaluation corpus ≥12 cases including negative, ambiguous, injection and adverse outputs; comparison to B2 deterministic baseline. If no real inference provider is approved, do not claim AI accuracy improvement.
6. Unit/integration tests, frontend build, docs/ADR, metrics report, and PR. No automatic merge.

CONTEXT BUDGET: One coherent session with milestone checkpoints. Target ≤20K active task context, warning at 25K, handoff at 35K. These are guidelines, not measurable guarantees. If context use unavailable report NOT AVAILABLE. No repository dumps, complete logs, unnecessary subagents or repeated reading of whole docs. Redirect verbose logs, show failures and short summaries. Do not mask failing tests.

GATES: Meet A1–A9 in B3 contract. Any unrun real-model evaluation, provider authorization or user tests must be clearly marked NOT RUN. Stop after pushed PR and a ≤500-word evidence-based report: branch/SHA, files, passing tests, exact CI statuses, sample accepted and rejected cases, metric denominators, costs or NOT MEASURED, unresolved risks, recommendation.
