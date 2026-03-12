package sdd.validate;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class DivergenceDetector {

    private final DomainModelParser domainParser = new DomainModelParser();
    private final GaugeSpecParser gaugeParser = new GaugeSpecParser();
    private final StepImplScanner stepScanner = new StepImplScanner();

    public DivergenceReport detect(Path domainJsonPath, Path gaugeSpecDir, Path stepsDir) throws IOException {
        // 1. Load behaviors from domain.json
        List<DomainModelParser.DomainBehavior> behaviors = domainParser.parse(domainJsonPath);
        Map<String, DomainModelParser.DomainBehavior> behaviorMap = behaviors.stream()
                .collect(Collectors.toMap(DomainModelParser.DomainBehavior::id, b -> b));

        // 2. Load scenarios from .spec files
        List<GaugeSpecParser.GaugeScenario> scenarios = gaugeParser.parse(gaugeSpecDir);
        Map<String, GaugeSpecParser.GaugeScenario> scenarioMap = scenarios.stream()
                .filter(s -> s.id() != null)
                .collect(Collectors.toMap(GaugeSpecParser.GaugeScenario::id, s -> s));

        // 3. Load step patterns from Java files
        Set<String> stepPatterns = stepScanner.scan(stepsDir);

        // 4. Build mappings
        List<BehaviorMapping> mappings = new ArrayList<>();

        for (DomainModelParser.DomainBehavior behavior : behaviors) {
            GaugeSpecParser.GaugeScenario scenario = scenarioMap.get(behavior.id());

            if (scenario == null) {
                mappings.add(new BehaviorMapping(
                        behavior.id(), behavior.given(), behavior.when(), behavior.then(),
                        null, List.of(), List.of(),
                        BehaviorMapping.DivergenceType.MISSING_SCENARIO
                ));
                continue;
            }

            // Check step coverage
            List<String> unmatched = new ArrayList<>();
            for (String stepLine : scenario.steps()) {
                if (!hasMatchingStepImpl(stepLine, stepPatterns)) {
                    unmatched.add(stepLine);
                }
            }

            BehaviorMapping.DivergenceType type = unmatched.isEmpty()
                    ? BehaviorMapping.DivergenceType.NONE
                    : BehaviorMapping.DivergenceType.MISSING_STEP_IMPL;

            mappings.add(new BehaviorMapping(
                    behavior.id(), behavior.given(), behavior.when(), behavior.then(),
                    scenario.title(), scenario.steps(), unmatched, type
            ));
        }

        // 5. Identify orphan scenarios (gauge IDs not in domain) — these are warnings, not errors
        List<String> orphans = scenarios.stream()
                .filter(s -> s.id() != null && !behaviorMap.containsKey(s.id()))
                .map(s -> s.id() + " — " + s.title())
                .toList();

        Path fileNamePath = domainJsonPath.getFileName();
        String domain = (fileNamePath != null ? fileNamePath.toString() : domainJsonPath.toString())
                .replaceFirst("\\.domain\\.json$", "");

        return new DivergenceReport(domain, mappings, orphans,
                behaviors.size(), scenarios.size(), stepPatterns.size());
    }

    private boolean hasMatchingStepImpl(String stepLine, Set<String> stepPatterns) {
        for (String pattern : stepPatterns) {
            // Split on <param> placeholders, quote literal segments, join with .*
            // -1 limit preserves trailing empty string when pattern ends with a param
            String[] parts = pattern.split("<[^>]+>", -1);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < parts.length; i++) {
                if (!parts[i].isEmpty()) sb.append(Pattern.quote(parts[i]));
                if (i < parts.length - 1) sb.append(".*");
            }
            try {
                if (Pattern.matches(sb.toString(), stepLine)) return true;
            } catch (Exception e) {
                if (stepLine.equals(pattern)) return true;
            }
        }
        return false;
    }
}
