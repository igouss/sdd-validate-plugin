package sdd.validate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DivergenceDetectorTest {

    private final DivergenceDetector detector = new DivergenceDetector();

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private Path writeDomainJson(Path dir, String content) throws IOException {
        Path json = dir.resolve("inventory.domain.json");
        Files.writeString(json, content);
        return json;
    }

    private Path writeSpec(Path specDir, String filename, String content) throws IOException {
        Path spec = specDir.resolve(filename);
        Files.writeString(spec, content);
        return spec;
    }

    private Path writeStepFile(Path stepsDir, String filename, String content) throws IOException {
        Path file = stepsDir.resolve(filename);
        Files.writeString(file, content);
        return file;
    }

    // -----------------------------------------------------------------------
    // NONE: fully aligned
    // -----------------------------------------------------------------------

    @Test
    void noDivergenceWhenBehaviorScenarioAndStepAllAlign(@TempDir Path root) throws IOException {
        Path specDir = Files.createDirectory(root.resolve("specs"));
        Path stepsDir = Files.createDirectory(root.resolve("steps"));

        Path domainJson = writeDomainJson(root, """
                {
                  "behaviors": [
                    { "id": "INV-001", "given": "empty inventory", "when": "addItem(x)", "then": "item present" }
                  ]
                }
                """);

        writeSpec(specDir, "inventory.spec", """
                # Inventory
                ## INV-001 — item can be added
                * the inventory is empty
                * the user adds item "widget"
                """);

        writeStepFile(stepsDir, "InventorySteps.java", """
                public class InventorySteps {
                    @Step("the inventory is empty")
                    public void empty() {}
                    @Step("the user adds item <name>")
                    public void addItem(String name) {}
                }
                """);

        DivergenceReport report = detector.detect(domainJson, specDir, stepsDir);

        assertFalse(report.hasErrors());
        assertEquals("ALL ALIGNED", report.overallResult());
        assertEquals(1, report.getMappings().size());
        assertEquals(BehaviorMapping.DivergenceType.NONE, report.getMappings().get(0).divergence());
    }

    // -----------------------------------------------------------------------
    // MISSING_SCENARIO
    // -----------------------------------------------------------------------

    @Test
    void detectsMissingScenarioWhenNoDotSpecMatchesId(@TempDir Path root) throws IOException {
        Path specDir = Files.createDirectory(root.resolve("specs"));
        Path stepsDir = Files.createDirectory(root.resolve("steps"));

        Path domainJson = writeDomainJson(root, """
                {
                  "behaviors": [
                    { "id": "INV-001", "given": "g", "when": "w", "then": "t" }
                  ]
                }
                """);

        // Spec exists but ID doesn't match
        writeSpec(specDir, "inventory.spec", """
                ## INV-999 — unrelated scenario
                * some step
                """);

        writeStepFile(stepsDir, "Steps.java", "public class Steps {}");

        DivergenceReport report = detector.detect(domainJson, specDir, stepsDir);

        assertTrue(report.hasErrors());
        assertEquals("DIVERGED", report.overallResult());
        BehaviorMapping mapping = report.getMappings().get(0);
        assertEquals("INV-001", mapping.id());
        assertEquals(BehaviorMapping.DivergenceType.MISSING_SCENARIO, mapping.divergence());
        assertNull(mapping.gaugeScenarioTitle());
    }

    @Test
    void detectsMissingScenarioWhenSpecDirIsEmpty(@TempDir Path root) throws IOException {
        Path specDir = Files.createDirectory(root.resolve("specs"));
        Path stepsDir = Files.createDirectory(root.resolve("steps"));

        Path domainJson = writeDomainJson(root, """
                {
                  "behaviors": [
                    { "id": "INV-001", "given": "g", "when": "w", "then": "t" }
                  ]
                }
                """);

        writeStepFile(stepsDir, "Steps.java", "public class Steps {}");

        DivergenceReport report = detector.detect(domainJson, specDir, stepsDir);

        assertTrue(report.hasErrors());
        assertEquals(BehaviorMapping.DivergenceType.MISSING_SCENARIO, report.getMappings().get(0).divergence());
    }

    // -----------------------------------------------------------------------
    // MISSING_STEP_IMPL
    // -----------------------------------------------------------------------

    @Test
    void detectsMissingStepImplWhenScenarioExistsButStepHasNoAnnotation(@TempDir Path root) throws IOException {
        Path specDir = Files.createDirectory(root.resolve("specs"));
        Path stepsDir = Files.createDirectory(root.resolve("steps"));

        Path domainJson = writeDomainJson(root, """
                {
                  "behaviors": [
                    { "id": "INV-001", "given": "g", "when": "w", "then": "t" }
                  ]
                }
                """);

        writeSpec(specDir, "inventory.spec", """
                ## INV-001 — scenario is present
                * this step has no implementation
                """);

        // No @Step annotation for the step text
        writeStepFile(stepsDir, "Steps.java", """
                public class Steps {
                    @Step("completely different step")
                    public void other() {}
                }
                """);

        DivergenceReport report = detector.detect(domainJson, specDir, stepsDir);

        assertTrue(report.hasErrors());
        BehaviorMapping mapping = report.getMappings().get(0);
        assertEquals(BehaviorMapping.DivergenceType.MISSING_STEP_IMPL, mapping.divergence());
        assertEquals(1, mapping.unmatchedSteps().size());
        assertEquals("this step has no implementation", mapping.unmatchedSteps().get(0));
    }

    // -----------------------------------------------------------------------
    // Orphan scenarios
    // -----------------------------------------------------------------------

    @Test
    void detectsOrphanScenarioNotInDomainAsBehavior(@TempDir Path root) throws IOException {
        Path specDir = Files.createDirectory(root.resolve("specs"));
        Path stepsDir = Files.createDirectory(root.resolve("steps"));

        Path domainJson = writeDomainJson(root, """
                {
                  "behaviors": [
                    { "id": "INV-001", "given": "g", "when": "w", "then": "t" }
                  ]
                }
                """);

        writeSpec(specDir, "inventory.spec", """
                ## INV-001 — known behavior
                * step one
                ## INV-999 — orphan, no domain entry
                * step two
                """);

        writeStepFile(stepsDir, "Steps.java", """
                public class Steps {
                    @Step("step one")
                    public void one() {}
                    @Step("step two")
                    public void two() {}
                }
                """);

        DivergenceReport report = detector.detect(domainJson, specDir, stepsDir);

        // Orphans are warnings, not errors
        assertFalse(report.hasErrors());
        assertEquals("ALIGNED (with orphans)", report.overallResult());
    }

    // -----------------------------------------------------------------------
    // Mixed results
    // -----------------------------------------------------------------------

    @Test
    void handlesMixOfNoneAndMissingScenario(@TempDir Path root) throws IOException {
        Path specDir = Files.createDirectory(root.resolve("specs"));
        Path stepsDir = Files.createDirectory(root.resolve("steps"));

        Path domainJson = writeDomainJson(root, """
                {
                  "behaviors": [
                    { "id": "INV-001", "given": "g1", "when": "w1", "then": "t1" },
                    { "id": "INV-002", "given": "g2", "when": "w2", "then": "t2" }
                  ]
                }
                """);

        // Only INV-001 has a matching scenario
        writeSpec(specDir, "inventory.spec", """
                ## INV-001 — first behavior
                * some step
                """);

        writeStepFile(stepsDir, "Steps.java", """
                public class Steps {
                    @Step("some step")
                    public void someStep() {}
                }
                """);

        DivergenceReport report = detector.detect(domainJson, specDir, stepsDir);

        assertTrue(report.hasErrors());
        assertEquals(2, report.getMappings().size());

        BehaviorMapping inv001 = report.getMappings().stream()
                .filter(m -> "INV-001".equals(m.id())).findFirst().orElseThrow();
        BehaviorMapping inv002 = report.getMappings().stream()
                .filter(m -> "INV-002".equals(m.id())).findFirst().orElseThrow();

        assertEquals(BehaviorMapping.DivergenceType.NONE, inv001.divergence());
        assertEquals(BehaviorMapping.DivergenceType.MISSING_SCENARIO, inv002.divergence());
    }

    // -----------------------------------------------------------------------
    // Parameterized step matching
    // -----------------------------------------------------------------------

    @Test
    void matchesParameterizedStepPattern(@TempDir Path root) throws IOException {
        Path specDir = Files.createDirectory(root.resolve("specs"));
        Path stepsDir = Files.createDirectory(root.resolve("steps"));

        Path domainJson = writeDomainJson(root, """
                {
                  "behaviors": [
                    { "id": "INV-001", "given": "g", "when": "w", "then": "t" }
                  ]
                }
                """);

        // Scenario step uses a concrete value
        writeSpec(specDir, "inventory.spec", """
                ## INV-001 — parameterized scenario
                * the inventory contains 5 items
                """);

        // Step impl uses a <param> placeholder
        writeStepFile(stepsDir, "Steps.java", """
                public class Steps {
                    @Step("the inventory contains <count> items")
                    public void inventoryContains(int count) {}
                }
                """);

        DivergenceReport report = detector.detect(domainJson, specDir, stepsDir);

        assertFalse(report.hasErrors());
        assertEquals(BehaviorMapping.DivergenceType.NONE, report.getMappings().get(0).divergence());
    }

    @Test
    void matchesStepPatternWithParamAtEnd(@TempDir Path root) throws IOException {
        Path specDir = Files.createDirectory(root.resolve("specs"));
        Path stepsDir = Files.createDirectory(root.resolve("steps"));

        Path domainJson = writeDomainJson(root, """
                {
                  "behaviors": [
                    { "id": "INV-001", "given": "g", "when": "w", "then": "t" }
                  ]
                }
                """);

        writeSpec(specDir, "inventory.spec", """
                ## INV-001 — trailing param
                * add item widget
                """);

        writeStepFile(stepsDir, "Steps.java", """
                public class Steps {
                    @Step("add item <name>")
                    public void addItem(String name) {}
                }
                """);

        DivergenceReport report = detector.detect(domainJson, specDir, stepsDir);

        assertFalse(report.hasErrors());
        assertEquals(BehaviorMapping.DivergenceType.NONE, report.getMappings().get(0).divergence());
    }

    // -----------------------------------------------------------------------
    // Edge cases
    // -----------------------------------------------------------------------

    @Test
    void emptyDomainModelProducesNoMappingsAndNoErrors(@TempDir Path root) throws IOException {
        Path specDir = Files.createDirectory(root.resolve("specs"));
        Path stepsDir = Files.createDirectory(root.resolve("steps"));

        Path domainJson = writeDomainJson(root, """
                { "behaviors": [] }
                """);

        writeSpec(specDir, "inventory.spec", """
                ## INV-001 — scenario with no matching domain behavior
                * step
                """);

        writeStepFile(stepsDir, "Steps.java", "public class Steps {}");

        DivergenceReport report = detector.detect(domainJson, specDir, stepsDir);

        assertFalse(report.hasErrors());
        assertTrue(report.getMappings().isEmpty());
        // INV-001 is an orphan (in spec but not in empty domain)
        assertEquals("ALIGNED (with orphans)", report.overallResult());
    }

    @Test
    void emptySpecDirMeansAllBehaviorsMissingScenario(@TempDir Path root) throws IOException {
        Path specDir = Files.createDirectory(root.resolve("specs"));
        Path stepsDir = Files.createDirectory(root.resolve("steps"));

        Path domainJson = writeDomainJson(root, """
                {
                  "behaviors": [
                    { "id": "INV-001", "given": "g", "when": "w", "then": "t" },
                    { "id": "INV-002", "given": "g2", "when": "w2", "then": "t2" }
                  ]
                }
                """);

        writeStepFile(stepsDir, "Steps.java", "public class Steps {}");

        DivergenceReport report = detector.detect(domainJson, specDir, stepsDir);

        assertTrue(report.hasErrors());
        assertEquals(2, report.getMappings().size());
        report.getMappings().forEach(m ->
                assertEquals(BehaviorMapping.DivergenceType.MISSING_SCENARIO, m.divergence()));
    }

    @Test
    void scenarioWithZeroStepsIsConsideredAligned(@TempDir Path root) throws IOException {
        Path specDir = Files.createDirectory(root.resolve("specs"));
        Path stepsDir = Files.createDirectory(root.resolve("steps"));

        Path domainJson = writeDomainJson(root, """
                {
                  "behaviors": [
                    { "id": "INV-001", "given": "g", "when": "w", "then": "t" }
                  ]
                }
                """);

        // Scenario exists but has no * step lines
        writeSpec(specDir, "inventory.spec", """
                ## INV-001 — scenario with no steps
                """);

        writeStepFile(stepsDir, "Steps.java", "public class Steps {}");

        DivergenceReport report = detector.detect(domainJson, specDir, stepsDir);

        assertFalse(report.hasErrors());
        assertEquals(BehaviorMapping.DivergenceType.NONE, report.getMappings().get(0).divergence());
    }
}
