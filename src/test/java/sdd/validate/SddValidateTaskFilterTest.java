package sdd.validate;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Functional tests for include/exclude domain filtering in SddValidateTask.
 * Uses GradleRunner to execute the task inside a real Gradle build.
 */
class SddValidateTaskFilterTest {

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private void writeSettings(Path dir) throws IOException {
        Files.writeString(dir.resolve("settings.gradle"), "rootProject.name = 'test-project'\n");
    }

    private void writeDomainJson(Path modelsDir, String domain, String... behaviorIds) throws IOException {
        StringBuilder behaviors = new StringBuilder();
        for (int i = 0; i < behaviorIds.length; i++) {
            if (i > 0) behaviors.append(",\n");
            behaviors.append(String.format(
                    "{ \"id\": \"%s\", \"given\": \"g\", \"when\": \"w\", \"then\": \"t\" }",
                    behaviorIds[i]));
        }
        Files.writeString(
                modelsDir.resolve(domain + ".domain.json"),
                "{ \"behaviors\": [" + behaviors + "] }\n");
    }

    private void writeSpec(Path gaugeDir, String domain, String behaviorId) throws IOException {
        Path specDir = Files.createDirectories(gaugeDir.resolve(domain));
        Files.writeString(specDir.resolve(domain + ".spec"),
                "## " + behaviorId + " — test scenario\n* some step\n");
    }

    private void writeStepImpl(Path stepsDir, String content) throws IOException {
        Files.writeString(stepsDir.resolve("Steps.java"), content);
    }

    /**
     * Sets up a minimal project with two domains: 'orders' and 'payments'.
     * Both have a single aligned behavior + scenario.
     */
    private Path buildProjectWithTwoDomains(Path root, String buildGradleExtra) throws IOException {
        writeSettings(root);

        Path specsDir = Files.createDirectories(root.resolve("specs"));
        Path modelsDir = Files.createDirectories(specsDir.resolve("models"));
        Path gaugeDir = specsDir.resolve("gauge");
        Path stepsDir = Files.createDirectories(root.resolve("src/test/java"));

        writeDomainJson(modelsDir, "orders", "ORD-001");
        writeDomainJson(modelsDir, "payments", "PAY-001");

        writeSpec(gaugeDir, "orders", "ORD-001");
        writeSpec(gaugeDir, "payments", "PAY-001");

        writeStepImpl(stepsDir, """
                public class Steps {
                    @com.thoughtworks.gauge.Step("some step")
                    public void someStep() {}
                }
                """);

        Files.writeString(root.resolve("build.gradle"), """
                plugins { id 'sdd.validate' }
                sddValidation {
                    specsDir  = 'specs'
                    stepsDir  = 'src/test/java'
                    failOnDivergence = false
                """ + buildGradleExtra + """
                }
                """);

        return root;
    }

    private GradleRunner runner(Path projectDir) {
        return GradleRunner.create()
                .withProjectDir(projectDir.toFile())
                .withPluginClasspath()
                .withArguments("sddValidate", "--stacktrace");
    }

    // ------------------------------------------------------------------
    // Tests
    // ------------------------------------------------------------------

    @Test
    void autoDiscoversBothDomainsWhenNoFilterSet(@TempDir Path root) throws IOException {
        buildProjectWithTwoDomains(root, "");
        BuildResult result = runner(root).build();

        assertEquals(SUCCESS, result.task(":sddValidate").getOutcome());
        assertTrue(result.getOutput().contains("orders"), "should report on orders domain");
        assertTrue(result.getOutput().contains("payments"), "should report on payments domain");
    }

    @Test
    void includeFilterRestrictsToSpecifiedDomains(@TempDir Path root) throws IOException {
        buildProjectWithTwoDomains(root, "includeDomains = ['orders']");
        BuildResult result = runner(root).build();

        assertEquals(SUCCESS, result.task(":sddValidate").getOutcome());
        assertTrue(result.getOutput().contains("orders"), "should report on orders domain");
        assertFalse(result.getOutput().contains("payments"), "should skip payments domain");
    }

    @Test
    void excludeFilterSkipsSpecifiedDomains(@TempDir Path root) throws IOException {
        buildProjectWithTwoDomains(root, "excludeDomains = ['payments']");
        BuildResult result = runner(root).build();

        assertEquals(SUCCESS, result.task(":sddValidate").getOutcome());
        assertTrue(result.getOutput().contains("orders"), "should report on orders domain");
        assertFalse(result.getOutput().contains("payments"), "should skip payments domain");
    }

    @Test
    void includeAndExcludeCanBeUsedTogether(@TempDir Path root) throws IOException {
        // include both, but exclude payments → effectively only orders
        buildProjectWithTwoDomains(root, "includeDomains = ['orders', 'payments']\nexcludeDomains = ['payments']");
        BuildResult result = runner(root).build();

        assertEquals(SUCCESS, result.task(":sddValidate").getOutcome());
        assertTrue(result.getOutput().contains("orders"));
        assertFalse(result.getOutput().contains("payments"));
    }
}
