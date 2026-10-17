# DMI — Remaining Batches: Implementation and Acceptance Contracts
Version: planning v1, 17 October 2026. **PROPOSED sequence; B4–B7 are conditional.** Prioritize evidence, working code, CI and user validation.

## B3 — Grounded Explanation Experiment (next implementation batch)
**Outcome:** demonstrate whether an optional AI explanation adds useful, supported reasoning to the existing deterministic report. Do not build a chat agent.

### Proposed work packages
1. **B3-A Evidence pack:** From server-side existing B2 findings, construct minimal immutable input per finding: IDs, migration pair, digest, static match and source location, adjudicated consequence/verdict, official doc metadata, baseline status, limitations. Attach stable evidence IDs. Exclude raw repository content beyond allowlisted excerpts; redact secrets and tokens. Fail closed on inconsistent/unknown provenance.
2. **B3-B Provider boundary:** Define `ExplanationModel` Java interface; `DisabledExplanationModel` as default; one opt-in provider implementation only if the user chooses a provider and cost/privacy controls. Credentials through environment/secrets only; never in Git, logs or browser. Explicit bounded timeouts, output length, concurrency, rate limiting and per-call cost ceiling. No silent retries or model side effects.
3. **B3-C Structured output + verifier:** `ExplanationResult {findingId, status: ACCEPTED|REJECTED|DISABLED|FALLBACK, summary, claims:[{text,evidenceIds}], nextChecks, limitations, modelMetadata}`. Parse via strict schema, reject unrecognized evidence IDs, hallucinated versions/patches/URLs, claims contradicting deterministic verdict, excessive or unsupported content. **A citation is necessary but not proof of semantic support**; add reviewed claim checks.
4. **B3-D Deterministic fallback and UI:** Show original report prominently. Explanation clearly marked AI-assisted and status-labeled; if rejected, timeout, provider off or unavailable, fall back to B2 curated explanation. No invented success or 'safe to merge'. Accessibility and loading/error states.
5. **B3-E Evaluation and controls:** Create a checked-in labelled corpus of positive, negative, ambiguous and malicious evidence-pack inputs. Include `pathmatch-strategy` as REVIEW_RECOMMENDED, not required action. Run deterministic baseline versus AI explanation under identical scenarios; record exact prompts, evidence versions, provider/model and date for external calls. A reviewer must adjudicate citation support and technical correctness. Track unsupported claim rate, invalid citation rate, false ACTION_REQUIRED rate, coverage, fallback rate, median/p95 latency and estimated cost per accepted answer. **Report denominators and confidence limits honestly**.

### B3 acceptance gates
- A1 default API/UI behaves exactly like B2 when AI disabled.
- A2 no secrets or arbitrary paths/code are sent to any provider; sanitized evidence reviewed before enabling external calls.
- A3 schema, provenance and verdict checks reject intentionally unsupported claims; explain the verifier's limits.
- A4 provider timeout/unavailability/invalid output leads to deterministic fallback without losing original findings.
- A5 full Java tests, frontend build and negative security tests pass in PR CI; new tests specifically cover prompt injection in evidence and output.
- A6 corpus has at least 12 heterogeneous labelled cases **before** experiment claims; coverage spans all existing findings plus synthetic adverse cases. This minimum is an engineering gate, not statistical validity.
- A7 present baseline vs AI outcome table with metrics, denominators, sample explanations, rejected cases, latency and actual or clearly estimated cost. If no provider access, ship an evaluation harness and fixtures with experiment status NOT RUN, not fabricated results.
- A8 no hosted AI endpoint enabled publicly without explicit owner consent, quotas and deployment secret review.
- A9 single PR with ADR, implementation docs, test report and `SESSION_HANDOFF.md`; do not auto-merge.

### B3 stop/go
GO to further AI work only if independently reviewed outputs add useful explanation with acceptably few unsupported claims at bounded cost/latency. If no improvement, retain deterministic mode and record NO-GO. No specific numerical target should be post-hoc; set thresholds before the experiment with the product owner.

## B4 — Realistic Migration Corpus & Developer Evaluation (conditional)
**Outcome:** avoid overfitting to one synthetic fixture. Add 2–4 *small licensed or original controlled* migration scenarios with carefully defined labels, positive and negative cases, version-pair context and independent reproduction. Do not blindly broaden public input API.
- Collect proper license attribution and reproducible fixture provenance.
- Include examples where detector should abstain, where OpenRewrite patches, and where DMI provides only informational guidance.
- Evaluate precision/error taxonomy and human usefulness with at least 3–5 consenting developer reviews if possible (exploratory evidence only).
- Gate: independent evaluation report and documented corrections; no unsupported general accuracy claim.
- Version-specific adjudication key becomes `(migrationPair, catalogueVersion, findingId)` before supporting a second pair.

## B5 — Product UX & Developer Workflow (conditional)
**Outcome:** turn static demo into a coherent decision workflow.
- Evidence drill-down with source and official documentation; explain what is known/unknown.
- Export stable Markdown/JSON review report, with provenance and limitations (no auto-PR).
- Use a human-in-the-loop checklist whose completion is explicitly user-entered, never inferred.
- Add frontend component/integration tests and accessibility verification.
- Gate: a developer can reproduce one review session and export a traceable report; feedback establishes usefulness.

## B6 — Controlled Hosted Demo & Operability (conditional)
**Outcome:** reproducible deployment if usage justifies operations.
- Deployment platform ADR (cost, Java support, free-tier fragility, logs, quotas, TLS and data residency).
- CI/CD, environment isolation, HTTP error handling, structured logs, trace IDs, health/readiness, uptime alerts, budget limits and rollback.
- API serves only allowlisted fixtures; hosted AI disabled by default. No open CORS or public tokens.
- Security review, dependency scan, secrets rotation playbook and README demo instructions.
- Gate: documented deploy, smoke test, failure drill, budget/latency data and owner-approved hosting.

## B7 — AI/RAG or Multi-repo Expansion Decision (NOT COMMITTED)
**Outcome:** decide whether scale requires new infrastructure, not implement by default.
- Consider RAG only if exact-key curated retrieval fails on a labelled corpus.
- Consider persistent evaluations/cache only if reproducibility and cost require it.
- Consider trusted-source repo input only with threat model, isolation/sandboxing, tenant/privacy model and resource accounting.
- Consider asynchronous processing only if bounded synchronous operations are insufficient.
- Gate: written evidence and owner approval. Otherwise close the roadmap at B6.

## Explicit non-goals across all stages
Kafka/microservices for optics; autonomous code editing; unreviewed migration PR generation; executing arbitrary source repositories; invented benchmark results; pretending prototypes are enterprise deployments; padding resume claims with AI/Java commercial experience.

## Verification record template (per batch)
Batch ID; status PLANNED/IN PROGRESS/VERIFIED/DEPLOYED; base and head SHA; exact commands and test totals; CI job links; labelled scenario IDs and outcomes; security checks; model/provider/version/cost when applicable; regressions; unresolved evidence; ADRs modified; reviewer decision; next gate.
