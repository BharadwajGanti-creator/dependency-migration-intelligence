# Implementation decisions

| Date | Decision | Rationale |
|------|----------|-----------|
| 2026-10-17 | Spring Boot **4.0.0** backend (Initializr default for Oct 2026) | Matches environment; analysis logic is framework-agnostic |
| 2026-10-17 | Single supported upgrade: `spring-boot-starter-parent` 2.7.18 → 3.2.12 | Smallest reproducible Spring 2→3 slice per architecture MVP |
| 2026-10-17 | Synthetic fixture checked into repo | ADR-002: no arbitrary GitHub URLs / SSRF |
| 2026-10-17 | Catalogue JSON in repo | ADR-005: reproducible citations |
| 2026-10-17 | No LLM adapter | Architecture: OFF by default until baseline validates value |
