package io.github.manosbatsis.gradle.plugin.reflections;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.TaskProvider;

/**
 * Gradle plugin that pre-scans compiled project classes and embeds a Reflections metadata index file
 * for both JVM and Android (AGP 8+) builds.
 */
public class ReflectionsPlugin implements Plugin<Project> {

    public static final String PLUGIN_NAME = ReflectionsPlugin.class.getPackage().getName();

    @Override
    public void apply(Project target) {
        ReflectionsPluginExtension extension = target.getExtensions().create(
                ReflectionsPluginExtension.EXTENSION_NAME,
                ReflectionsPluginExtension.class
        );

        TaskProvider<ReflectionsMetadataEmbeddingTask> jvmTask = target.getTasks().register(
                ReflectionsMetadataEmbeddingTask.TASK_NAME,
                ReflectionsMetadataEmbeddingTask.class,
                task -> {
                    task.setGroup("build");
                    task.setDescription("Pre-scan and embed a Reflections metadata index file into the artifact.");
                }
        );

        target.getPlugins().withType(JavaBasePlugin.class, plugin ->
                jvmTask.configure(task -> {
                    if (target.getTasks().findByName(JavaPlugin.CLASSES_TASK_NAME) != null) {
                        task.dependsOn(JavaPlugin.CLASSES_TASK_NAME);
                    }
                })
        );

        target.getPlugins().withType(JavaPlugin.class, plugin -> {
            target.getTasks().named(JavaPlugin.JAR_TASK_NAME, jarTask -> jarTask.dependsOn(jvmTask));
            target.getTasks().named(JavaPlugin.TEST_TASK_NAME, testTask -> testTask.mustRunAfter(jvmTask));
        });

        target.getPlugins().withId("com.android.base", plugin ->
                AndroidReflectionsConfigurator.configure(target, extension)
        );
    }
}
