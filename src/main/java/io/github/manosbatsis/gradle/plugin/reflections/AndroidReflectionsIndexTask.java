package io.github.manosbatsis.gradle.plugin.reflections;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * AGP 8+ variant task that reads {@code ScopedArtifact.CLASSES} directories and jars and emits
 * Reflections metadata files into the variant's generated Java resources directory.
 */
public abstract class AndroidReflectionsIndexTask extends DefaultTask {

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ListProperty<Directory> getAllDirectories();

    @InputFiles
    @PathSensitive(PathSensitivity.NONE)
    public abstract ListProperty<RegularFile> getAllJars();

    @Input
    @Optional
    public abstract ListProperty<String> getPackages();

    @Input
    public abstract ListProperty<String> getIndexFilenames();

    @Internal
    public abstract Property<Object[]> getParams();

    @OutputDirectory
    public abstract DirectoryProperty getOutputDirectory();

    @TaskAction
    public void generateAndroidIndex() {
        File outputDir = getOutputDirectory().get().getAsFile();
        deleteDirectoryContents(outputDir);

        List<File> classRoots = new ArrayList<>();
        for (Directory dir : getAllDirectories().get()) {
            classRoots.add(dir.getAsFile());
        }
        for (RegularFile jar : getAllJars().get()) {
            classRoots.add(jar.getAsFile());
        }

        List<String> packages = getPackages().getOrElse(new ArrayList<>());
        Object[] params = getParams().getOrElse(new Object[0]);
        List<String> indexFilenames = getIndexFilenames().get();

        ReflectionsIndexGenerator.generateIndex(
                classRoots,
                packages,
                params,
                outputDir,
                indexFilenames
        );
    }

    private void deleteDirectoryContents(File dir) {
        if (!dir.exists()) {
            return;
        }
        File[] children = dir.listFiles();
        if (children == null) {
            return;
        }
        for (File child : children) {
            if (child.isDirectory()) {
                deleteDirectoryContents(child);
            }
            child.delete();
        }
    }
}

