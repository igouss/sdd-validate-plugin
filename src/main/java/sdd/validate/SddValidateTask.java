package sdd.validate;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public abstract class SddValidateTask extends DefaultTask {

    @Input
    public abstract Property<String> getDomainInput();

    @InputDirectory
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract DirectoryProperty getSpecsDirInput();

    @Optional
    @InputDirectory
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract DirectoryProperty getStepsDirInput();

    @Input
    public abstract Property<Boolean> getFailOnDivergence();

    @TaskAction
    public void validate() {
        String domain = getDomainInput().get();
        Path specsRoot = getSpecsDirInput().get().getAsFile().toPath();
        Path stepsRoot = getStepsDirInput().isPresent()
            ? getStepsDirInput().get().getAsFile().toPath()
            : specsRoot; // fallback, detector handles missing

        Path domainJson = specsRoot.resolve("models/" + domain + ".domain.json");
        Path gaugeSpecDir = specsRoot.resolve("gauge/" + domain);
        Path reportJson = specsRoot.resolve("reports/" + domain + "-divergence.json");

        if (!Files.exists(domainJson)) {
            throw new GradleException("Domain model not found: " + domainJson + "\nRun sdd-spec-analyzer first.");
        }
        if (!Files.isDirectory(gaugeSpecDir)) {
            throw new GradleException("Gauge spec directory not found: " + gaugeSpecDir + "\nRun sdd-contract-generator first.");
        }

        try {
            DivergenceDetector detector = new DivergenceDetector();
            DivergenceReport report = detector.detect(domainJson, gaugeSpecDir, stepsRoot);
            report.printConsole(getLogger());
            report.writeJson(reportJson);
            getLogger().lifecycle("Report written to: " + reportJson);

            if (getFailOnDivergence().get() && report.hasErrors()) {
                throw new GradleException("SDD validation failed: " + report.overallResult());
            }
        } catch (IOException e) {
            throw new GradleException("Failed to run SDD validation: " + e.getMessage(), e);
        }
    }
}
