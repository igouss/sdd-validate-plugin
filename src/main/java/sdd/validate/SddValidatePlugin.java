package sdd.validate;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class SddValidatePlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        SddValidateExtension ext = project.getExtensions()
                .create("sddValidation", SddValidateExtension.class);

        project.getTasks().register("sddValidate", SddValidateTask.class, task -> {
            task.setGroup("verification");
            task.setDescription("Validates SDD spec-to-implementation alignment");

            task.getSpecsDirInput().set(
                project.getLayout().getProjectDirectory().dir(
                    project.provider(ext::getSpecsDir)
                )
            );
            task.getFailOnDivergence().set(
                project.provider(ext::isFailOnDivergence)
            );
            task.getIncludeDomains().set(ext.getIncludeDomains());
            task.getExcludeDomains().set(ext.getExcludeDomains());

            // stepsDir is optional — only wire if the directory exists at config time
            // We use a lazy provider; the task action handles missing directory gracefully
            task.getStepsDirInput().set(
                project.getLayout().getProjectDirectory().dir(
                    project.provider(ext::getStepsDir)
                )
            );
        });
    }
}
