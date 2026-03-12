package sdd.validate;

import java.util.List;

public record BehaviorMapping(
        String id,
        String domainGiven,
        String domainWhen,
        String domainThen,
        String gaugeScenarioTitle,
        List<String> gaugeSteps,
        List<String> unmatchedSteps,
        DivergenceType divergence
) {
    public enum DivergenceType {
        NONE,
        MISSING_SCENARIO,
        MISSING_STEP_IMPL
    }
}
