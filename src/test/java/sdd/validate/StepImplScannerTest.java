package sdd.validate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class StepImplScannerTest {

    private final StepImplScanner scanner = new StepImplScanner();

    @Test
    void scansLiteralStepAnnotation(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve("Steps.java"), """
                public class Steps {
                    @Step("the inventory is empty")
                    public void inventoryIsEmpty() {}
                }
                """);

        Set<String> patterns = scanner.scan(dir);

        assertEquals(1, patterns.size());
        assertTrue(patterns.contains("the inventory is empty"));
    }

    @Test
    void scansParameterizedStepAnnotation(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve("Steps.java"), """
                public class Steps {
                    @Step("the inventory contains <count> items")
                    public void inventoryContains(int count) {}
                }
                """);

        Set<String> patterns = scanner.scan(dir);

        assertTrue(patterns.contains("the inventory contains <count> items"));
    }

    @Test
    void scansMultipleStepAnnotationsInSingleFile(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve("Steps.java"), """
                public class Steps {
                    @Step("step one")
                    public void stepOne() {}

                    @Step("step two")
                    public void stepTwo() {}

                    @Step("step with <param>")
                    public void stepWithParam(String param) {}
                }
                """);

        Set<String> patterns = scanner.scan(dir);

        assertEquals(3, patterns.size());
        assertTrue(patterns.contains("step one"));
        assertTrue(patterns.contains("step two"));
        assertTrue(patterns.contains("step with <param>"));
    }

    @Test
    void scansMultipleJavaFilesInDirectory(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve("InventorySteps.java"), """
                public class InventorySteps {
                    @Step("the inventory is empty")
                    public void empty() {}
                }
                """);
        Files.writeString(dir.resolve("OrderSteps.java"), """
                public class OrderSteps {
                    @Step("an order exists with id <id>")
                    public void orderExists(String id) {}
                }
                """);

        Set<String> patterns = scanner.scan(dir);

        assertEquals(2, patterns.size());
        assertTrue(patterns.contains("the inventory is empty"));
        assertTrue(patterns.contains("an order exists with id <id>"));
    }

    @Test
    void returnsEmptySetForEmptyDirectory(@TempDir Path dir) throws IOException {
        Set<String> patterns = scanner.scan(dir);

        assertNotNull(patterns);
        assertTrue(patterns.isEmpty());
    }

    @Test
    void returnsEmptySetWhenNoStepAnnotationsPresent(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve("Plain.java"), """
                public class Plain {
                    public void notAStep() {}
                    // @Step is mentioned in a comment but not as annotation
                }
                """);

        Set<String> patterns = scanner.scan(dir);

        assertTrue(patterns.isEmpty());
    }

    @Test
    void walksSubdirectories(@TempDir Path dir) throws IOException {
        Path sub = dir.resolve("sub");
        Files.createDirectories(sub);
        Files.writeString(sub.resolve("DeepSteps.java"), """
                public class DeepSteps {
                    @Step("deep step")
                    public void deepStep() {}
                }
                """);

        Set<String> patterns = scanner.scan(dir);

        assertTrue(patterns.contains("deep step"));
    }

    @Test
    void ignoresNonJavaFiles(@TempDir Path dir) throws IOException {
        // A .txt file that happens to contain @Step — should be ignored
        Files.writeString(dir.resolve("notes.txt"), "@Step(\"should be ignored\")\n");

        Set<String> patterns = scanner.scan(dir);

        assertTrue(patterns.isEmpty());
    }
}
