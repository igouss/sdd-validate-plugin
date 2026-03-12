# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
./gradlew build          # compile + jar
./gradlew test           # run tests (none yet)
./gradlew publishToMavenLocal  # publish to local Maven cache for use in other projects
```

To use the plugin in another project during development:
```groovy
// settings.gradle of consuming project
pluginManagement {
    repositories { mavenLocal(); gradlePluginPortal() }
}
// build.gradle
plugins { id 'sdd.validate' version '1.0.0' }
```

## What This Is

A Gradle plugin that validates alignment between three artifacts in an SDD (Software Design Document) workflow:

1. **Domain model** — `specs/models/<domain>.domain.json` — behaviors with `id`, `given`, `when`, `then`
2. **Gauge specs** — `specs/gauge/<domain>/*.spec` — scenarios headed `## BEHAVIOR-ID — title`
3. **Step implementations** — Java files scanned for `@Step("...")` annotations

It detects two classes of divergence (errors) and one warning class:
- `MISSING_SCENARIO` — domain behavior has no matching Gauge scenario
- `MISSING_STEP_IMPL` — scenario steps have no matching `@Step` annotation
- Orphan scenarios — Gauge scenarios with no matching domain behavior (warning only)

## Architecture

```
SddValidatePlugin          registers task + extension
SddValidateExtension       config: domain, specsDir, stepsDir, failOnDivergence
SddValidateTask            task action; resolves paths; drives DivergenceDetector
DivergenceDetector         orchestrator: parse → map → detect gaps
  DomainModelParser        reads *.domain.json → List<DomainBehavior>
  GaugeSpecParser          reads *.spec files → List<GaugeScenario>
  StepImplScanner          walks Java files → Set<String> step patterns
BehaviorMapping            record: per-behavior result with DivergenceType
DivergenceReport           console + JSON output; hasErrors() drives failOnDivergence
```

## File Conventions

**Domain JSON** — `specs/models/<domain>.domain.json`:
```json
{ "behaviors": [{ "id": "INV-001", "given": "...", "when": "...", "then": "..." }] }
```

**Gauge spec scenario heading** — must use `##` (not `#`) and format `## ID — title`:
```
## INV-001 — item can be added to inventory
```

**Step impl** — `@Step` annotation value must literally match or use `<param>` placeholders:
```java
@Step("the inventory contains <count> items")
```

Step matching: literal segments of the `@Step` pattern are `Pattern.quote`d and `<param>` placeholders become `.*`.

## Default Extension Values

| Property | Default |
|---|---|
| `domain` | `inventory` |
| `specsDir` | `specs` |
| `stepsDir` | `src/test/java` |
| `failOnDivergence` | `true` |

Report is written to `<specsDir>/reports/<domain>-divergence.json`.
