package sdd.validate;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.tasks.Input;

public abstract class SddValidateTask extends DefaultTask {

    @InputDirectory
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract DirectoryProperty getSpecsDirInput();

    @Optional
    @InputDirectory
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract DirectoryProperty getStepsDirInput();

    @Input
    public abstract Property<Boolean> getFailOnDivergence();

    @Input
    public abstract ListProperty<String> getIncludeDomains();

    @Input
    public abstract ListProperty<String> getExcludeDomains();

    @TaskAction
    public void validate() {
        Path specsRoot = getSpecsDirInput().get().getAsFile().toPath();
        Path stepsRoot = getStepsDirInput().isPresent()
            ? getStepsDirInput().get().getAsFile().toPath()
            : specsRoot;
        Path modelsDir = specsRoot.resolve("models");

        if (!Files.isDirectory(modelsDir)) {
            throw new GradleException("Models directory not found: " + modelsDir);
        }

        List<String> include = getIncludeDomains().get();
        List<String> exclude = getExcludeDomains().get();

        List<Path> domainFiles;
        try (Stream<Path> stream = Files.list(modelsDir)) {
            domainFiles = stream
                .filter(p -> p.getFileName().toString().endsWith(".domain.json"))
                .filter(p -> {
                    String name = p.getFileName().toString().replaceFirst("\\.domain\\.json$", "");
                    if (!include.isEmpty() && !include.contains(name)) return false;
                    return !exclude.contains(name);
                })
                .sorted()
                .collect(Collectors.toList());
        } catch (IOException e) {
            throw new GradleException("Failed to scan models directory: " + e.getMessage(), e);
        }

        if (domainFiles.isEmpty()) {
            throw new GradleException("No *.domain.json files found in: " + modelsDir);
        }

        DivergenceDetector detector = new DivergenceDetector();
        List<DivergenceReport> reports = new ArrayList<>();

        for (Path domainJson : domainFiles) {
            String domain = domainJson.getFileName().toString().replaceFirst("\\.domain\\.json$", "");
            Path gaugeSpecDir = specsRoot.resolve("gauge/" + domain);
            Path reportJson = specsRoot.resolve("reports/" + domain + "-divergence.json");

            if (!Files.isDirectory(gaugeSpecDir)) {
                getLogger().warn("Skipping domain '{}': gauge spec directory not found: {}", domain, gaugeSpecDir);
                continue;
            }

            try {
                DivergenceReport report = detector.detect(domainJson, gaugeSpecDir, stepsRoot);
                reports.add(report);
                report.writeJson(reportJson);
                getLogger().lifecycle("Report written to: {}", reportJson);
            } catch (IOException e) {
                throw new GradleException("Failed to validate domain '" + domain + "': " + e.getMessage(), e);
            }
        }

        // Print unified console report
        DivergenceReport.printSummary(reports, getLogger());

        if (getFailOnDivergence().get() && reports.stream().anyMatch(DivergenceReport::hasErrors)) {
            throw new GradleException("SDD validation failed: one or more domains have divergences");
        }
    }
}
