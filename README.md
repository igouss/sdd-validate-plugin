# sdd-validate

Gradle plugin that validates alignment between domain models, Gauge scenarios, and Java step implementations.

Fails the build when the code drifts from the spec.

## What it checks

Given three artifacts per domain:

1. **Domain model** — `specs/models/<domain>.domain.json` — behaviors with `id`, `given`, `when`, `then`
2. **Gauge specs** — `specs/gauge/<domain>/*.spec` — scenarios headed `## BEHAVIOR-ID — title`
3. **Step implementations** — Java files with `@Step("...")` annotations

It detects:

| Severity | Type | Meaning |
|---|---|---|
| ERROR | `MISSING_SCENARIO` | Domain behavior has no matching Gauge scenario |
| ERROR | `MISSING_STEP_IMPL` | Scenario steps have no matching `@Step` annotation |
| WARNING | Orphan scenario | Gauge scenario has no matching domain behavior |

All `*.domain.json` files under `specs/models/` are discovered and analyzed automatically.

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
sddValidation {
    specsDir         = 'specs'           // root specs directory (default: 'specs')
    stepsDir         = 'src/test/java'   // Java step implementations (default: 'src/test/java')
    failOnDivergence = true              // fail build on errors (default: true)
    includeDomains   = ['orders']        // optional whitelist — only analyze these domains
    excludeDomains   = ['legacy']        // optional blacklist — skip these domains
}
```

`includeDomains` and `excludeDomains` are both empty by default, meaning all discovered domains are analyzed. When both are set, `includeDomains` is applied first, then `excludeDomains` removes from that set.

## Running

```bash
./gradlew sddValidate
```

Output:

```
SDD Divergence Report — orders
══════════════════════════════════
  Behaviors: 2  |  Scenarios: 1  |  Step impls: 5

  ORD-001   placeOrder   → "order can be placed"   OK
  ORD-002   cancelOrder  → (none)                  MISSING SCENARIO

  Missing scenarios: 1  |  Missing step impls: 0  |  Orphans: 0
  RESULT: DIVERGED

SDD Divergence Report — payments
══════════════════════════════════
  Behaviors: 1  |  Scenarios: 1  |  Step impls: 5

  PAY-001   processPayment   → "payment is processed"   OK

  Missing scenarios: 0  |  Missing step impls: 0  |  Orphans: 0
  RESULT: ALL ALIGNED

══════════════════════════════════
OVERALL: 2 domains  |  3 behaviors  |  1 divergences  |  0 orphans  |  DIVERGED
```

Per-domain JSON reports written to `<specsDir>/reports/<domain>-divergence.json`.

## File conventions

**Domain JSON** — `specs/models/<domain>.domain.json`:
```json
{
  "behaviors": [
    { "id": "ORD-001", "given": "empty cart", "when": "placeOrder(items)", "then": "order is created" }
  ]
}
```

**Gauge spec scenario heading** — must use `##` and format `## ID — title`:
```
## ORD-001 — order can be placed
* the cart contains item "widget"
* the user places an order
* an order is created with 1 item
```

**Step implementation:**
```java
@Step("the cart contains item <name>")
public void cartContainsItem(String name) { ... }
```

Step matching: literal segments of the `@Step` value are matched exactly; `<param>` placeholders match any text.

## Building

```bash
./gradlew build
./gradlew test
./gradlew publishToMavenLocal
```
