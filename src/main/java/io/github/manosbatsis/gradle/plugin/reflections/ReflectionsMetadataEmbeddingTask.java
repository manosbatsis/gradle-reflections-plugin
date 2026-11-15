package io.github.manosbatsis.gradle.plugin.reflections;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Gradle task that pre-scans compiled classes for JVM or custom builds and writes Reflections index files.
 */
public class ReflectionsMetadataEmbeddingTask extends DefaultTask {

    public static final String TASK_NAME = "reflections";

    @TaskAction
    public void generateMetadataIndex() {
        Project project = getProject();
        ReflectionsPluginExtension extension = project.getExtensions().findByType(ReflectionsPluginExtension.class);
        if (extension == null) {
            extension = new ReflectionsPluginExtension();
        }

        Collection<File> classRoots = resolveInputDirectories(project, extension);
        File outputDir = resolveOutputDirectory(project, extension);
        List<String> indexFilenames = extension.resolveIndexFilenames(project);

        ReflectionsIndexGenerator.generateIndex(
                classRoots,
                extension.getPackages(),
                extension.getParams(),
                outputDir,
                indexFilenames
        );
    }

    private Collection<File> resolveInputDirectories(Project project, ReflectionsPluginExtension extension) {
        if (extension.getInputFiles() != null && !extension.getInputFiles().isEmpty()) {
            return extension.getInputFiles();
        }
        SourceSetContainer sourceSets = project.getExtensions().findByType(SourceSetContainer.class);
        if (sourceSets != null) {
            SourceSet main = sourceSets.findByName(SourceSet.MAIN_SOURCE_SET_NAME);
            if (main != null) {
                return new ArrayList<>(main.getOutput().getClassesDirs().getFiles());
            }
        }
        throw new GradleException(
                "No Java sourceSets or reflectionsConfig.inputFiles found. Configure inputFiles/outputDirectory or apply java/android plugin.");
    }

    private File resolveOutputDirectory(Project project, ReflectionsPluginExtension extension) {
        if (extension.getOutputDirectory() != null) {
            return extension.getOutputDirectory();
        }
        SourceSetContainer sourceSets = project.getExtensions().findByType(SourceSetContainer.class);
        if (sourceSets != null) {
            SourceSet main = sourceSets.findByName(SourceSet.MAIN_SOURCE_SET_NAME);
            if (main != null && main.getOutput().getResourcesDir() != null) {
                return main.getOutput().getResourcesDir();
            }
        }
        return project.getLayout().getBuildDirectory().dir("generated/resources/reflections/main").get().getAsFile();
    }
}
