package sdd.validate;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.gradle.api.logging.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class DivergenceReport {

    private final String domain;
    private final List<BehaviorMapping> mappings;
    private final List<String> orphanScenarioTitles;
    private final int behaviorCount;
    private final int scenarioCount;
    private final int stepImplCount;

    public DivergenceReport(String domain, List<BehaviorMapping> mappings,
                            List<String> orphanScenarioTitles,
                            int behaviorCount, int scenarioCount, int stepImplCount) {
        this.domain = domain;
        this.mappings = mappings;
        this.orphanScenarioTitles = orphanScenarioTitles;
        this.behaviorCount = behaviorCount;
        this.scenarioCount = scenarioCount;
        this.stepImplCount = stepImplCount;
    }

    public List<BehaviorMapping> getMappings() {
        return mappings;
    }

    /** Contract violations: missing scenarios or missing step impls. Orphans are warnings, not errors. */
    public boolean hasErrors() {
        return mappings.stream().anyMatch(m ->
                m.divergence() == BehaviorMapping.DivergenceType.MISSING_SCENARIO ||
                m.divergence() == BehaviorMapping.DivergenceType.MISSING_STEP_IMPL
        );
    }

    public String overallResult() {
        if (hasErrors()) return "DIVERGED";
        if (!orphanScenarioTitles.isEmpty()) return "ALIGNED (with orphans)";
        return "ALL ALIGNED";
    }

    public void printConsole(Logger logger) {
        // Single pass to count both divergence types
        int missingScenarios = 0, missingSteps = 0;
        for (BehaviorMapping m : mappings) {
            if (m.divergence() == BehaviorMapping.DivergenceType.MISSING_SCENARIO) missingScenarios++;
            else if (m.divergence() == BehaviorMapping.DivergenceType.MISSING_STEP_IMPL) missingSteps++;
        }

        logger.lifecycle("");
        logger.lifecycle("SDD Divergence Report — " + domain);
        logger.lifecycle("══════════════════════════════════");
        logger.lifecycle("  Behaviors: {}  |  Scenarios: {}  |  Step impls: {}",
                behaviorCount, scenarioCount, stepImplCount);
        logger.lifecycle("");

        for (BehaviorMapping m : mappings) {
            String command = extractCommand(m.domainWhen());
            String status = switch (m.divergence()) {
                case NONE -> "OK";
                case MISSING_SCENARIO -> "MISSING SCENARIO";
                case MISSING_STEP_IMPL -> "MISSING STEP IMPL (" + m.unmatchedSteps().size() + ")";
            };
            String scenarioLabel = m.gaugeScenarioTitle() != null
                    ? "\"" + m.gaugeScenarioTitle() + "\""
                    : "(none)";
            logger.lifecycle("  {}\t{}\t→ {}\t{}", m.id(), command, scenarioLabel, status);
        }

        if (!orphanScenarioTitles.isEmpty()) {
            logger.warn("");
            logger.warn("  Orphan scenarios (no matching domain behavior):");
            for (String title : orphanScenarioTitles) {
                logger.warn("    - {}", title);
            }
        }

        logger.lifecycle("");
        logger.lifecycle("  Missing scenarios: {}  |  Missing step impls: {}  |  Orphans: {}",
                missingScenarios, missingSteps, orphanScenarioTitles.size());
        logger.lifecycle("  RESULT: {}", overallResult());
        logger.lifecycle("");
    }

    public void writeJson(Path outputPath) throws IOException {
        Path parent = outputPath.getParent();
        if (parent != null) Files.createDirectories(parent);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        record JsonReport(String domain, String result, int behaviors, int scenarios, int stepImpls,
                          List<BehaviorMapping> mappings, List<String> orphanScenarios) {}

        JsonReport report = new JsonReport(domain, overallResult(), behaviorCount, scenarioCount,
                stepImplCount, mappings, orphanScenarioTitles);
        Files.writeString(outputPath, gson.toJson(report));
    }

    private static String extractCommand(String when) {
        int paren = when.indexOf('(');
        if (paren > 0) return when.substring(0, paren);
        int space = when.indexOf(' ');
        if (space > 0) return when.substring(0, space);
        return when;
    }
}
