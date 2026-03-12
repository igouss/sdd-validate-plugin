package sdd.validate;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BehaviorMappingTest {

    @Test
    void constructorAssignsAllFields() {
        BehaviorMapping m = new BehaviorMapping(
                "INV-001",
                "the inventory is empty",
                "addItem(widget)",
                "the item is present in inventory",
                "item can be added to inventory",
                List.of("step one", "step two"),
                List.of(),
                BehaviorMapping.DivergenceType.NONE
        );

        assertEquals("INV-001", m.id());
        assertEquals("the inventory is empty", m.domainGiven());
        assertEquals("addItem(widget)", m.domainWhen());
        assertEquals("the item is present in inventory", m.domainThen());
        assertEquals("item can be added to inventory", m.gaugeScenarioTitle());
        assertEquals(List.of("step one", "step two"), m.gaugeSteps());
        assertEquals(List.of(), m.unmatchedSteps());
        assertEquals(BehaviorMapping.DivergenceType.NONE, m.divergence());
    }

    @Test
    void divergenceTypeNoneIsDistinct() {
        BehaviorMapping m = new BehaviorMapping("X-001", "g", "w", "t", "title",
                List.of(), List.of(), BehaviorMapping.DivergenceType.NONE);

        assertSame(BehaviorMapping.DivergenceType.NONE, m.divergence());
        assertNotSame(BehaviorMapping.DivergenceType.MISSING_SCENARIO, m.divergence());
        assertNotSame(BehaviorMapping.DivergenceType.MISSING_STEP_IMPL, m.divergence());
    }

    @Test
    void divergenceTypeMissingScenarioAllowsNullScenarioTitle() {
        BehaviorMapping m = new BehaviorMapping("X-001", "g", "w", "t",
                null,               // no matching gauge scenario
                List.of(), List.of(),
                BehaviorMapping.DivergenceType.MISSING_SCENARIO);

        assertNull(m.gaugeScenarioTitle());
        assertEquals(BehaviorMapping.DivergenceType.MISSING_SCENARIO, m.divergence());
    }

    @Test
    void divergenceTypeMissingStepImplTracksUnmatchedSteps() {
        List<String> unmatched = List.of("step with no impl");
        BehaviorMapping m = new BehaviorMapping("X-001", "g", "w", "t",
                "scenario exists",
                List.of("step with no impl"), unmatched,
                BehaviorMapping.DivergenceType.MISSING_STEP_IMPL);

        assertEquals(1, m.unmatchedSteps().size());
        assertEquals("step with no impl", m.unmatchedSteps().get(0));
        assertEquals(BehaviorMapping.DivergenceType.MISSING_STEP_IMPL, m.divergence());
    }

    @Test
    void recordEquality() {
        BehaviorMapping a = new BehaviorMapping("INV-001", "g", "w", "t", "title",
                List.of("s1"), List.of(), BehaviorMapping.DivergenceType.NONE);
        BehaviorMapping b = new BehaviorMapping("INV-001", "g", "w", "t", "title",
                List.of("s1"), List.of(), BehaviorMapping.DivergenceType.NONE);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void recordInequalityOnDifferentId() {
        BehaviorMapping a = new BehaviorMapping("INV-001", "g", "w", "t", "title",
                List.of(), List.of(), BehaviorMapping.DivergenceType.NONE);
        BehaviorMapping b = new BehaviorMapping("INV-002", "g", "w", "t", "title",
                List.of(), List.of(), BehaviorMapping.DivergenceType.NONE);

        assertNotEquals(a, b);
    }

    @Test
    void allThreeDivergenceTypesExist() {
        // Existence check — enum must have exactly NONE, MISSING_SCENARIO, MISSING_STEP_IMPL
        BehaviorMapping.DivergenceType[] values = BehaviorMapping.DivergenceType.values();
        assertEquals(3, values.length);
    }
}
