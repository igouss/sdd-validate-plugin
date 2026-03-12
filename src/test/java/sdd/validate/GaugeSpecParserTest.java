package sdd.validate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GaugeSpecParserTest {

    private final GaugeSpecParser parser = new GaugeSpecParser();

    @Test
    void parsesScenarioWithIdAndTitle(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve("inventory.spec"), """
                # Inventory
                ## INV-001 — item can be added to inventory
                * the inventory is empty
                * the user adds item "widget"
                * the inventory contains 1 item
                """);

        List<GaugeSpecParser.GaugeScenario> scenarios = parser.parse(dir);

        assertEquals(1, scenarios.size());
        GaugeSpecParser.GaugeScenario s = scenarios.get(0);
        assertEquals("INV-001", s.id());
        assertEquals("item can be added to inventory", s.title());
        assertFalse(s.missingId());
    }

    @Test
    void parsesStepLines(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve("steps.spec"), """
                # Steps
                ## INV-002 — item can be removed
                * the inventory contains item "widget"
                * the user removes item "widget"
                * the inventory is empty
                """);

        List<GaugeSpecParser.GaugeScenario> scenarios = parser.parse(dir);

        assertEquals(1, scenarios.size());
        List<String> steps = scenarios.get(0).steps();
        assertEquals(3, steps.size());
        assertEquals("the inventory contains item \"widget\"", steps.get(0));
        assertEquals("the user removes item \"widget\"", steps.get(1));
        assertEquals("the inventory is empty", steps.get(2));
    }

    @Test
    void parsesMultipleScenarios(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve("multi.spec"), """
                # Multi scenario spec
                ## INV-001 — add item
                * the inventory is empty
                * add item "x"
                ## INV-002 — remove item
                * item "x" exists
                * remove item "x"
                """);

        List<GaugeSpecParser.GaugeScenario> scenarios = parser.parse(dir);

        assertEquals(2, scenarios.size());
        assertEquals("INV-001", scenarios.get(0).id());
        assertEquals("add item", scenarios.get(0).title());
        assertEquals(2, scenarios.get(0).steps().size());
        assertEquals("INV-002", scenarios.get(1).id());
        assertEquals("remove item", scenarios.get(1).title());
        assertEquals(2, scenarios.get(1).steps().size());
    }

    @Test
    void setsAlternativeMissingIdWhenScenarioHeadingHasNoId(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve("noid.spec"), """
                # No ID spec
                ## just a plain scenario title
                * some step
                """);

        List<GaugeSpecParser.GaugeScenario> scenarios = parser.parse(dir);

        assertEquals(1, scenarios.size());
        GaugeSpecParser.GaugeScenario s = scenarios.get(0);
        assertNull(s.id());
        assertEquals("just a plain scenario title", s.title());
        assertTrue(s.missingId());
    }

    @Test
    void parsesEmptySpecFile(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve("empty.spec"), "");

        List<GaugeSpecParser.GaugeScenario> scenarios = parser.parse(dir);

        assertNotNull(scenarios);
        assertTrue(scenarios.isEmpty());
    }

    @Test
    void returnsEmptyListWhenNoDotSpecFilesPresent(@TempDir Path dir) throws IOException {
        // Write a .txt file — should be ignored
        Files.writeString(dir.resolve("ignore.txt"), "## INV-001 — should be ignored\n* step\n");

        List<GaugeSpecParser.GaugeScenario> scenarios = parser.parse(dir);

        assertTrue(scenarios.isEmpty());
    }

    @Test
    void parsesMixOfIdAndNoIdScenarios(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve("mixed.spec"), """
                # Mixed spec
                ## INV-001 — has id
                * step one
                ## no identifier here
                * step two
                """);

        List<GaugeSpecParser.GaugeScenario> scenarios = parser.parse(dir);

        assertEquals(2, scenarios.size());

        GaugeSpecParser.GaugeScenario withId = scenarios.get(0);
        assertEquals("INV-001", withId.id());
        assertFalse(withId.missingId());

        GaugeSpecParser.GaugeScenario withoutId = scenarios.get(1);
        assertNull(withoutId.id());
        assertTrue(withoutId.missingId());
    }

    @Test
    void doesNotTreatTopLevelHeadingAsScenario(@TempDir Path dir) throws IOException {
        // Lines starting with "# " (single hash, space) are spec titles — not scenarios
        Files.writeString(dir.resolve("heading.spec"), """
                # This is the spec title, not a scenario
                ## INV-001 — actual scenario
                * a step
                """);

        List<GaugeSpecParser.GaugeScenario> scenarios = parser.parse(dir);

        assertEquals(1, scenarios.size());
        assertEquals("INV-001", scenarios.get(0).id());
    }
}
