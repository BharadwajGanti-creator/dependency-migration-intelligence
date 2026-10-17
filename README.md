# Dependency Migration Intelligence (DMI)

MVP developer tool that explains a **single declared Maven dependency upgrade** with traceable documentation citations and bounded static source evidence.

## Environment scan (this workspace)

| Tool | Version |
|------|---------|
| OpenJDK | 21 |
| Node.js | 22 |
| npm | 10.x |
| Git | 2.43 |

Maven is provided via `./backend/mvnw` (system `mvn` not required).

## Supported demo migration

- **Fixture:** `spring-migration-case` (synthetic, MIT)
- **Coordinate:** `org.springframework.boot:spring-boot-starter-parent`
- **Versions:** `2.7.18` → `3.2.12`

## Quick start

```bash
# Terminal 1 — API (port 8787)
cd backend && ./mvnw spring-boot:run

# Terminal 2 — UI (port 4321, proxies /api)
cd frontend && npm install && npm run dev
```

Open http://127.0.0.1:4321 for the portfolio, then **Run the demo analysis**.

### API smoke test

```bash
curl -s -X POST http://127.0.0.1:8787/api/v1/analyses/sync \
  -H 'Content-Type: application/json' \
  -d '{
    "fixtureId":"spring-migration-case",
    "dependencyGroup":"org.springframework.boot",
    "dependencyArtifact":"spring-boot-starter-parent",
    "fromVersion":"2.7.18",
    "toVersion":"3.2.12"
  }' | head -c 400
```

## Verification

```bash
cd backend && ./mvnw verify
cd frontend && npm ci && npm run build
```

## Repository layout

- `backend/` — Spring Boot 4 REST API
- `frontend/` — React portfolio + analyzer UI
- `fixtures/spring-migration-case/` — allowlisted analysis target
- `docs/` — ADRs, baseline, implementation decisions

## Invariants

- No finding without provenance (source line and/or catalogue doc link)
- No claim that an unexecuted migration is safe
- No arbitrary GitHub URLs, Maven execution, or LLM in MVP

## License

MIT — see [LICENSE](LICENSE).
