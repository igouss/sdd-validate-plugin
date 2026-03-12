package sdd.validate;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DivergenceReportTest {

    // -----------------------------------------------------------------------
    // hasErrors()
    // -----------------------------------------------------------------------

    @Test
    void hasErrorsReturnsFalseWhenAllMappingsAreNone() {
        List<BehaviorMapping> mappings = List.of(
                mapping("INV-001", BehaviorMapping.DivergenceType.NONE),
                mapping("INV-002", BehaviorMapping.DivergenceType.NONE)
        );
        DivergenceReport report = new DivergenceReport("inventory", mappings, List.of(), 2, 2, 3);

        assertFalse(report.hasErrors());
    }

    @Test
    void hasErrorsReturnsTrueWhenAnyMappingIsMissingScenario() {
        List<BehaviorMapping> mappings = List.of(
                mapping("INV-001", BehaviorMapping.DivergenceType.NONE),
                mapping("INV-002", BehaviorMapping.DivergenceType.MISSING_SCENARIO)
        );
        DivergenceReport report = new DivergenceReport("inventory", mappings, List.of(), 2, 1, 0);

        assertTrue(report.hasErrors());
    }

    @Test
    void hasErrorsReturnsTrueWhenAnyMappingIsMissingStepImpl() {
        List<BehaviorMapping> mappings = List.of(
                mapping("INV-001", BehaviorMapping.DivergenceType.NONE),
                mapping("INV-002", BehaviorMapping.DivergenceType.MISSING_STEP_IMPL)
        );
        DivergenceReport report = new DivergenceReport("inventory", mappings, List.of(), 2, 2, 1);

        assertTrue(report.hasErrors());
    }

    @Test
    void hasErrorsReturnsFalseForEmptyMappingsList() {
        DivergenceReport report = new DivergenceReport("inventory", List.of(), List.of(), 0, 0, 0);

        assertFalse(report.hasErrors());
    }

    // -----------------------------------------------------------------------
    // overallResult()
    // -----------------------------------------------------------------------

    @Test
    void overallResultIsAllAlignedWhenNoErrorsAndNoOrphans() {
        DivergenceReport report = new DivergenceReport("inventory",
                List.of(mapping("INV-001", BehaviorMapping.DivergenceType.NONE)),
                List.of(), 1, 1, 1);

        assertEquals("ALL ALIGNED", report.overallResult());
    }

    @Test
    void overallResultIsDivergedWhenHasErrors() {
        DivergenceReport report = new DivergenceReport("inventory",
                List.of(mapping("INV-001", BehaviorMapping.DivergenceType.MISSING_SCENARIO)),
                List.of(), 1, 0, 0);

        assertEquals("DIVERGED", report.overallResult());
    }

    @Test
    void overallResultIsAlignedWithOrphansWhenNoErrorsButOrphansExist() {
        DivergenceReport report = new DivergenceReport("inventory",
                List.of(mapping("INV-001", BehaviorMapping.DivergenceType.NONE)),
                List.of("INV-999 — stale scenario"),
                1, 2, 1);

        assertEquals("ALIGNED (with orphans)", report.overallResult());
    }

    @Test
    void divergedTakesPrecedenceOverOrphans() {
        // Even if there are orphans, DIVERGED wins
        DivergenceReport report = new DivergenceReport("inventory",
                List.of(mapping("INV-001", BehaviorMapping.DivergenceType.MISSING_STEP_IMPL)),
                List.of("INV-999 — orphan"),
                1, 2, 0);

        assertEquals("DIVERGED", report.overallResult());
    }

    // -----------------------------------------------------------------------
    // writeJson()
    // -----------------------------------------------------------------------

    @Test
    void writeJsonCreatesFileAtExpectedPath(@TempDir Path tempDir) throws IOException {
        DivergenceReport report = new DivergenceReport("inventory",
                List.of(mapping("INV-001", BehaviorMapping.DivergenceType.NONE)),
                List.of(), 1, 1, 2);

        Path outputPath = tempDir.resolve("reports").resolve("inventory-divergence.json");
        report.writeJson(outputPath);

        assertTrue(Files.exists(outputPath), "JSON report file should have been created");
    }

    @Test
    void writeJsonCreatesParentDirectoriesIfMissing(@TempDir Path tempDir) throws IOException {
        DivergenceReport report = new DivergenceReport("orders",
                List.of(), List.of(), 0, 0, 0);

        Path outputPath = tempDir.resolve("deeply").resolve("nested").resolve("orders-divergence.json");
        report.writeJson(outputPath);

        assertTrue(Files.exists(outputPath));
    }

    @Test
    void writeJsonContainsDomainField(@TempDir Path tempDir) throws IOException {
        DivergenceReport report = new DivergenceReport("inventory",
                List.of(), List.of(), 0, 0, 0);

        Path outputPath = tempDir.resolve("report.json");
        report.writeJson(outputPath);

        String json = Files.readString(outputPath);
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        assertEquals("inventory", root.get("domain").getAsString());
    }

    @Test
    void writeJsonContainsResultField(@TempDir Path tempDir) throws IOException {
        DivergenceReport report = new DivergenceReport("inventory",
                List.of(mapping("INV-001", BehaviorMapping.DivergenceType.MISSING_SCENARIO)),
                List.of(), 1, 0, 0);

        Path outputPath = tempDir.resolve("report.json");
        report.writeJson(outputPath);

        String json = Files.readString(outputPath);
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        assertEquals("DIVERGED", root.get("result").getAsString());
    }

    @Test
    void writeJsonContainsCountFields(@TempDir Path tempDir) throws IOException {
        DivergenceReport report = new DivergenceReport("inventory",
                List.of(), List.of(), 3, 5, 7);

        Path outputPath = tempDir.resolve("report.json");
        report.writeJson(outputPath);

        String json = Files.readString(outputPath);
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        assertEquals(3, root.get("behaviors").getAsInt());
        assertEquals(5, root.get("scenarios").getAsInt());
        assertEquals(7, root.get("stepImpls").getAsInt());
    }

    @Test
    void writeJsonContainsMappingsArray(@TempDir Path tempDir) throws IOException {
        DivergenceReport report = new DivergenceReport("inventory",
                List.of(mapping("INV-001", BehaviorMapping.DivergenceType.NONE)),
                List.of(), 1, 1, 1);

        Path outputPath = tempDir.resolve("report.json");
        report.writeJson(outputPath);

        String json = Files.readString(outputPath);
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        assertTrue(root.has("mappings"));
        assertEquals(1, root.getAsJsonArray("mappings").size());
    }

    @Test
    void writeJsonContainsOrphanScenariosArray(@TempDir Path tempDir) throws IOException {
        DivergenceReport report = new DivergenceReport("inventory",
                List.of(mapping("INV-001", BehaviorMapping.DivergenceType.NONE)),
                List.of("INV-999 — orphan scenario"),
                1, 2, 1);

        Path outputPath = tempDir.resolve("report.json");
        report.writeJson(outputPath);

        String json = Files.readString(outputPath);
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        assertTrue(root.has("orphanScenarios"));
        assertEquals(1, root.getAsJsonArray("orphanScenarios").size());
    }

    // -----------------------------------------------------------------------
    // getMappings()
    // -----------------------------------------------------------------------

    @Test
    void getMappingsReturnsAllMappings() {
        List<BehaviorMapping> input = List.of(
                mapping("INV-001", BehaviorMapping.DivergenceType.NONE),
                mapping("INV-002", BehaviorMapping.DivergenceType.MISSING_SCENARIO)
        );
        DivergenceReport report = new DivergenceReport("inventory", input, List.of(), 2, 1, 0);

        assertEquals(2, report.getMappings().size());
    }

    // -----------------------------------------------------------------------
    // printSummary()
    // -----------------------------------------------------------------------

    @Test
    void printSummaryAggregatesAcrossAllDomains() {
        DivergenceReport r1 = new DivergenceReport("orders",
                List.of(mapping("ORD-001", BehaviorMapping.DivergenceType.NONE),
                        mapping("ORD-002", BehaviorMapping.DivergenceType.MISSING_SCENARIO)),
                List.of(), 2, 1, 3);
        DivergenceReport r2 = new DivergenceReport("payments",
                List.of(mapping("PAY-001", BehaviorMapping.DivergenceType.NONE)),
                List.of("PAY-999 — orphan"), 1, 2, 2);

        Logger logger = Logging.getLogger(DivergenceReportTest.class);
        // smoke test: must not throw; actual log output goes to the Gradle logger
        assertDoesNotThrow(() ->
                DivergenceReport.printSummary(List.of(r1, r2), logger));
    }

    @Test
    void printSummaryWithEmptyListDoesNotThrow() {
        Logger logger = Logging.getLogger(DivergenceReportTest.class);
        assertDoesNotThrow(() ->
                DivergenceReport.printSummary(List.of(), logger));
    }

    @Test
    void printSummaryDetectsOverallDivergence() {
        DivergenceReport aligned = new DivergenceReport("orders",
                List.of(mapping("ORD-001", BehaviorMapping.DivergenceType.NONE)), List.of(), 1, 1, 1);
        DivergenceReport diverged = new DivergenceReport("payments",
                List.of(mapping("PAY-001", BehaviorMapping.DivergenceType.MISSING_SCENARIO)), List.of(), 1, 0, 0);

        // overall: any domain diverged means the build should fail
        assertTrue(List.of(aligned, diverged).stream().anyMatch(DivergenceReport::hasErrors));
    }

    @Test
    void printSummaryAllAlignedWhenNoDomainHasErrors() {
        DivergenceReport r1 = new DivergenceReport("orders",
                List.of(mapping("ORD-001", BehaviorMapping.DivergenceType.NONE)), List.of(), 1, 1, 1);
        DivergenceReport r2 = new DivergenceReport("payments",
                List.of(mapping("PAY-001", BehaviorMapping.DivergenceType.NONE)), List.of(), 1, 1, 1);

        assertFalse(List.of(r1, r2).stream().anyMatch(DivergenceReport::hasErrors));
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static BehaviorMapping mapping(String id, BehaviorMapping.DivergenceType type) {
        return new BehaviorMapping(id, "given", "when", "then",
                type == BehaviorMapping.DivergenceType.NONE ? "some scenario" : null,
                List.of(), List.of(), type);
    }
}
