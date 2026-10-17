# ADR-002: B1 evidence integrity (F02/F03)

Status: accepted (Batch B1 + principal review fixes)

## Maven reader

- Direct-child DOM navigation; only root `/project/dependencies/dependency` are direct declarations.
- One-step `${property}` resolution from root `/project/properties`; unresolved parent → `PARENT_VERSION_UNRESOLVED` (not `VERSION_MISMATCH`).

## Source scanner

- Returns **ScanResult** (`COMPLETE` / `PARTIAL`) with diagnostic codes.
- Depth completeness: unlimited **metadata-only** walk detects allowlisted files beyond `MAX_DEPTH`; never reports `COMPLETE` when deeper eligible files exist.
- Pattern matches carry **patternKey** and **commentOnly**; classifier uses key identity (no excerpt re-regex).
- Comment-only hits → **POSSIBLE** with explicit rationale (not executable migration impact).

## Fixture digest (`dmi-digest-v3`)

| Aspect | Digest | Source scanner |
|--------|--------|----------------|
| Purpose | Bounded **snapshot identity** for the fixture tree | **Evidence** for migration signals |
| File types | All regular non-symlink files ≤ `MAX_DEPTH` | Allowlisted (`pom.xml`, `src/**` *.java/properties/yml) |
| Depth | Fail closed (`FIXTURE_DEPTH_EXCEEDED`) if any regular file deeper than `MAX_DEPTH` | `PARTIAL` + `DEPTH_LIMIT_REACHED` if allowlisted files beyond depth |
| Limits | Same file count / per-file / total byte caps as scanner | Same |

Digest value changed from v2 → v3 when upgrading.
