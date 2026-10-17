# Spring migration case fixture

Synthetic Maven project for DMI MVP validation. **Original work — no third-party repository clone.**

## Provenance

- Created for Dependency Migration Intelligence POC (October 2026).
- License: MIT (see `LICENSE` in repository root).
- Intended upgrade path: `spring-boot-starter-parent` **2.7.18 → 3.2.12**.

## Seeded signals

| Signal | Location |
|--------|----------|
| `javax.servlet` import | `src/main/java/com/example/demo/web/LegacyServletFilter.java:3` |
| `javax.annotation.PostConstruct` | `src/main/java/com/example/demo/config/StartupHook.java:5` |
| Spring Boot 2.x property | `src/main/resources/application.properties:2` |
