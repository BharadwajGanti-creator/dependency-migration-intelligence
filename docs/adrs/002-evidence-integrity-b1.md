# ADR-002: B1 evidence integrity (F02/F03)

Status: accepted (Batch B1)

- Maven DOM uses **direct-child** navigation; only root `/project/dependencies/dependency` count as direct declarations.
- Property substitution is **one-step** from root `/project/properties` only; nested/unresolved `${...}` remain unknown.
- Scanner returns **ScanResult** with `COMPLETE`/`PARTIAL` and diagnostic codes; classifier matches **pattern keys** from scanner, not excerpt re-regex.
- Fixture digest uses **dmi-digest-v2** algorithm with the same byte/file/depth caps as scanning (digest value changes from v0.1).
