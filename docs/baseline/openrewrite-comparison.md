# OpenRewrite baseline (pending full run)

## Goal

Compare DMI static findings against OpenRewrite `dryRun` on the same fixture for
`spring-boot-starter-parent` 2.7.18 → 3.2.12.

## Planned commands

```bash
cd fixtures/spring-migration-case
# Requires network + Maven for recipe resolution (not executed in MVP CI)
mvn -U org.openrewrite.maven:rewrite-maven-plugin:dryRun \
  -Drewrite.activeRecipes=org.openrewrite.java.spring.boot3.UpgradeSpringBoot_3_2
```

## Initial hypothesis

OpenRewrite will propose Jakarta import rewrites and dependency changes. DMI adds
**traceable documentation citations** and **line-level static evidence** in one
structured report without executing Maven.

## Status

**NOT RUN** in this environment (MVP avoids remote execution). Record output here
when baseline gate P5 is executed locally.
