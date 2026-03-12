# sdd-validate

Gradle plugin that validates alignment between a domain model, Gauge scenarios, and Java step implementations.

Fails the build when the code drifts from the spec.

## What it checks

Given three artifacts:

1. **Domain model** — `specs/models/<domain>.domain.json` — behaviors with `id`, `given`, `when`, `then`
2. **Gauge specs** — `specs/gauge/<domain>/*.spec` — scenarios headed `## BEHAVIOR-ID — title`
3. **Step implementations** — Java files with `@Step("...")` annotations

It detects:

| Severity | Type | Meaning |
|---|---|---|
| ERROR | `MISSING_SCENARIO` | Domain behavior has no matching Gauge scenario |
| ERROR | `MISSING_STEP_IMPL` | Scenario steps have no matching `@Step` annotation |
| WARNING | Orphan scenario | Gauge scenario has no matching domain behavior |

## Setup

```groovy
// settings.gradle
pluginManagement {
    repositories { mavenLocal(); gradlePluginPortal() }
}

// build.gradle
plugins {
    id 'sdd.validate' version '1.0.0'
}
```

## Configuration

```groovy
sddValidate {
    domain           = 'inventory'       // domain name (default: 'inventory')
    specsDir         = 'specs'           // root specs directory (default: 'specs')
    stepsDir         = 'src/test/java'   // Java step implementations (default: 'src/test/java')
    failOnDivergence = true              // fail build on errors (default: true)
}
```

## Running

```bash
./gradlew sddValidate
```

Output:

```
SDD Divergence Report — inventory
══════════════════════════════════
  Behaviors: 3  |  Scenarios: 2  |  Step impls: 5

  INV-001   addItem   → "item can be added to inventory"   OK
  INV-002   removeItem   → (none)   MISSING SCENARIO
  INV-003   listItems   → "list all items"   MISSING STEP IMPL (1)

  Missing scenarios: 1  |  Missing step impls: 1  |  Orphans: 0
  RESULT: DIVERGED
```

JSON report written to `<specsDir>/reports/<domain>-divergence.json`.

## File conventions

**Domain JSON:**
```json
{
  "behaviors": [
    { "id": "INV-001", "given": "empty inventory", "when": "addItem(sku)", "then": "item is stored" }
  ]
}
```

**Gauge spec scenario heading** — must use `##` and format `## ID — title`:
```
## INV-001 — item can be added to inventory
* the inventory is empty
* I add item "SKU-1"
* the inventory contains 1 items
```

**Step implementation:**
```java
@Step("the inventory contains <count> items")
public void inventoryContainsItems(int count) { ... }
```

Step matching: literal segments of the `@Step` value are matched exactly; `<param>` placeholders match any text.

## Building

```bash
./gradlew build
./gradlew test
./gradlew publishToMavenLocal
```
