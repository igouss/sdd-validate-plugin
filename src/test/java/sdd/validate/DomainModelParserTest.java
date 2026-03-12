package sdd.validate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DomainModelParserTest {

    private final DomainModelParser parser = new DomainModelParser();

    @Test
    void parsesMultipleBehaviors(@TempDir Path dir) throws IOException {
        Path json = dir.resolve("inventory.domain.json");
        Files.writeString(json, """
                {
                  "behaviors": [
                    { "id": "INV-001", "given": "empty inventory", "when": "addItem(x)", "then": "item is present" },
                    { "id": "INV-002", "given": "item in inventory", "when": "removeItem(x)", "then": "item is absent" }
                  ]
                }
                """);

        List<DomainModelParser.DomainBehavior> result = parser.parse(json);

        assertEquals(2, result.size());

        DomainModelParser.DomainBehavior first = result.get(0);
        assertEquals("INV-001", first.id());
        assertEquals("empty inventory", first.given());
        assertEquals("addItem(x)", first.when());
        assertEquals("item is present", first.then());

        DomainModelParser.DomainBehavior second = result.get(1);
        assertEquals("INV-002", second.id());
        assertEquals("item in inventory", second.given());
        assertEquals("removeItem(x)", second.when());
        assertEquals("item is absent", second.then());
    }

    @Test
    void parsesEmptyBehaviorsArray(@TempDir Path dir) throws IOException {
        Path json = dir.resolve("empty.domain.json");
        Files.writeString(json, """
                { "behaviors": [] }
                """);

        List<DomainModelParser.DomainBehavior> result = parser.parse(json);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void throwsWhenFileDoesNotExist(@TempDir Path dir) {
        Path missing = dir.resolve("nonexistent.domain.json");

        assertThrows(IOException.class, () -> parser.parse(missing));
    }

    @Test
    void parsesMinimalBehaviorWithAllRequiredFields(@TempDir Path dir) throws IOException {
        Path json = dir.resolve("minimal.domain.json");
        Files.writeString(json, """
                {
                  "behaviors": [
                    { "id": "ORD-001", "given": "g", "when": "w", "then": "t" }
                  ]
                }
                """);

        List<DomainModelParser.DomainBehavior> result = parser.parse(json);

        assertEquals(1, result.size());
        DomainModelParser.DomainBehavior b = result.get(0);
        assertEquals("ORD-001", b.id());
        assertEquals("g", b.given());
        assertEquals("w", b.when());
        assertEquals("t", b.then());
    }

    @Test
    void parsesAllFourFieldsCorrectly(@TempDir Path dir) throws IOException {
        Path json = dir.resolve("full.domain.json");
        Files.writeString(json, """
                {
                  "behaviors": [
                    {
                      "id": "PAY-042",
                      "given": "a confirmed order exists",
                      "when": "processPayment(orderId, amount)",
                      "then": "payment record is created and order status is PAID"
                    }
                  ]
                }
                """);

        List<DomainModelParser.DomainBehavior> result = parser.parse(json);

        assertEquals(1, result.size());
        DomainModelParser.DomainBehavior b = result.get(0);
        assertEquals("PAY-042", b.id());
        assertEquals("a confirmed order exists", b.given());
        assertEquals("processPayment(orderId, amount)", b.when());
        assertEquals("payment record is created and order status is PAID", b.then());
    }
}
