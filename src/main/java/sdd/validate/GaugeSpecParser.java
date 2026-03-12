package sdd.validate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class GaugeSpecParser {

    public record GaugeScenario(String id, String title, List<String> steps, boolean missingId) {}

    private static final Pattern SCENARIO_WITH_ID = Pattern.compile("^##\\s+([A-Z]+-\\d+)\\s+—\\s+(.+)$");
    private static final Pattern SCENARIO_NO_ID = Pattern.compile("^##\\s+(.+)$");
    private static final Pattern STEP_LINE = Pattern.compile("^\\*\\s+(.+)$");

    public List<GaugeScenario> parse(Path specDir) throws IOException {
        List<GaugeScenario> scenarios = new ArrayList<>();

        List<Path> specFiles;
        try (Stream<Path> stream = Files.list(specDir)) {
            specFiles = stream.filter(p -> p.toString().endsWith(".spec")).toList();
        }

        for (Path specFile : specFiles) {
            List<String> lines = Files.readAllLines(specFile);
            String currentId = null;
            String currentTitle = null;
            boolean currentMissingId = false;
            List<String> currentSteps = new ArrayList<>();

            for (String line : lines) {
                // Cache match results — Matcher state is consumed after first .matches() call
                Matcher withIdMatcher = SCENARIO_WITH_ID.matcher(line);
                boolean withIdMatch = withIdMatcher.matches();
                boolean noIdMatch = !withIdMatch && !line.startsWith("# ")
                        && SCENARIO_NO_ID.matcher(line).matches();

                if (withIdMatch || noIdMatch) {
                    // Flush previous scenario
                    if (currentTitle != null) {
                        scenarios.add(new GaugeScenario(currentId, currentTitle,
                                List.copyOf(currentSteps), currentMissingId));
                    }

                    if (withIdMatch) {
                        currentId = withIdMatcher.group(1);
                        currentTitle = withIdMatcher.group(2).trim();
                        currentMissingId = false;
                    } else {
                        Matcher noIdMatcher = SCENARIO_NO_ID.matcher(line);
                        noIdMatcher.matches();
                        currentId = null;
                        currentTitle = noIdMatcher.group(1).trim();
                        currentMissingId = true;
                    }
                    currentSteps = new ArrayList<>();
                } else {
                    Matcher stepMatcher = STEP_LINE.matcher(line);
                    if (stepMatcher.matches() && currentTitle != null) {
                        currentSteps.add(stepMatcher.group(1).trim());
                    }
                }
            }

            // Flush last scenario
            if (currentTitle != null) {
                scenarios.add(new GaugeScenario(currentId, currentTitle,
                        List.copyOf(currentSteps), currentMissingId));
            }
        }
        return scenarios;
    }
}
