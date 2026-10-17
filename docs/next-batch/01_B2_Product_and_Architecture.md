# DMI B2 — Product Value Validation & Explainable Migration Evidence

Status: PROPOSED, not implemented. Basis: GitHub `main` after B1 merge, Spring Boot parent 2.7.18 → 3.2.12, synthetic allowlisted fixture, baseline `docs/baseline/openrewrite-comparison.md`.

## Product question
Can DMI provide evidence-grounded, actionable **migration verification guidance** beyond the code transformations shown by OpenRewrite? A finding absent from an OpenRewrite patch is only a hypothesis, **not proof** of a missed incompatibility.

## B2 outcome
Implement an evidence-backed migration **adjudication and comparison view** for the existing fixture and existing migration path, supported by reproducible experiments and a machine-readable reviewed dataset. Avoid superficial extra migrations and any LLM implementation before proof of value.

## Existing architecture retained
Java 21 / Spring Boot API and React UI; bounded fixture; curated catalogue; deterministic results; no arbitrary repository inputs, network crawling in request path, code execution in API, DB, agents or model provider. OpenRewrite may run only as an **offline developer/baseline command on an isolated fixture copy**, never through the analyzer endpoint.

## B2 deliverable contracts
1. `docs/validation/b2-adjudications.json` holds reviewed claims for current finding IDs: verdict `ACTION_REQUIRED | REVIEW_RECOMMENDED | INFORMATIONAL | FALSE_POSITIVE | UNVERIFIED`, evidence URLs, what the evidence actually proves, tested scenario, recipe/version, reviewer notes, last-reviewed date. The values must follow proof, not be guessed by Cursor.
2. Add an optional response field (backward compatible) providing adjudication **for supported catalogue entries** only. If review is unverified, show `UNVERIFIED` and no unsupported recommendation. Distinguish `CONFIRMED_STATIC` pattern confidence from semantic migration necessity.
3. React finding detail can display review verdict and comparison status, citation, applicable checks and limitations; clearly identify what was actually executed.
4. `docs/validation/b2-results.md` contains executable reproduction commands, immutable versions, input/expected/observed behavior, OpenRewrite patch comparison, and explicit inconclusive outcomes.
5. Correct the stale `docs/SESSION_HANDOFF.md` and verification report during B2, without rewriting history.

## Required hypothesis: `pathmatch-strategy`
The fixture has `spring.mvc.pathmatch.matching-strategy=ant_path_matcher`. Evaluate its Boot 2.7.18→3.2.12 semantics against version-relevant official documentation and, if feasible, a minimal controlled runtime behavior probe. Check whether the named OpenRewrite recipe's dry run omitted a change, but don't equate absence of a diff with a broken migration. Mark REVIEW_RECOMMENDED/INFORMATIONAL/UNVERIFIED as indicated by the evidence, and adjust existing catalogue wording if overstated.

## Scope, gates and stop conditions
- One migration pair, fixture and catalogue set, with limited additional controlled **test variants** stored under tests or validation data only.
- No API-driven Maven execution, no public deployment, no LLM, no retrieval pipeline yet.
- If official documentation or runtime reproduction is inaccessible, mark `UNVERIFIED`; continue implementing transparent UX using that state, without claiming independent value.
- Prefer fewer accurate claims over wider coverage. The key metric is adjudicated actionable findings, false-positive rate on small labelled examples, evidence/citation coverage and comparison gaps; report actual counts and denominator.
- Do not use `CONFIRMED_STATIC` to mean a migration is certainly required.

## Architectural trade-off
Static evidence classifier indicates a textual code/config signal; review verdict indicates human-adjudicated migration consequence. Keep them as distinct fields so uncertainty cannot be laundered into an authoritative repair suggestion.
