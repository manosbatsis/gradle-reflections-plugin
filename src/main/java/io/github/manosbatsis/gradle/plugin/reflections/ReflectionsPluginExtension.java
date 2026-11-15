package io.github.manosbatsis.gradle.plugin.reflections;

import org.gradle.api.Project;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Configuration extension for {@link ReflectionsPlugin}, exposed as {@code reflectionsConfig}.
 */
public class ReflectionsPluginExtension {

    public static final String EXTENSION_NAME = "reflectionsConfig";

    private String indexFilename;
    private List<String> additionalIndexFilenames = new ArrayList<>();
    private Object[] params = new Object[0];
    private List<String> packages = new ArrayList<>();
    private List<File> inputFiles = new ArrayList<>();
    private File outputDirectory;

    public ReflectionsPluginExtension() {
    }

    public String getIndexFilename() {
        return indexFilename;
    }

    public void setIndexFilename(String indexFilename) {
        this.indexFilename = indexFilename;
    }

    public List<String> getAdditionalIndexFilenames() {
        return additionalIndexFilenames;
    }

    public void setAdditionalIndexFilenames(Collection<String> additionalIndexFilenames) {
        this.additionalIndexFilenames = additionalIndexFilenames == null
                ? new ArrayList<>()
                : new ArrayList<>(additionalIndexFilenames);
    }

    public void additionalIndexFilenames(String... filenames) {
        if (filenames != null) {
            Collections.addAll(this.additionalIndexFilenames, filenames);
        }
    }

    public Object[] getParams() {
        return params == null ? new Object[0] : params;
    }

    public void setParams(Object... params) {
        this.params = params == null ? new Object[0] : params;
    }

    public List<String> getPackages() {
        return packages;
    }

    public void setPackages(Collection<String> packages) {
        this.packages = packages == null ? new ArrayList<>() : new ArrayList<>(packages);
    }

    public void packages(String... packages) {
        if (packages != null) {
            Collections.addAll(this.packages, packages);
        }
    }

    public List<File> getInputFiles() {
        return inputFiles;
    }

    public void setInputFiles(Collection<File> inputFiles) {
        this.inputFiles = inputFiles == null ? new ArrayList<>() : new ArrayList<>(inputFiles);
    }

    public File getOutputDirectory() {
        return outputDirectory;
    }

    public void setOutputDirectory(File outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    public List<String> resolveIndexFilenames(Project project) {
        List<String> resolved = new ArrayList<>();
        String primary = (indexFilename != null && !indexFilename.trim().isEmpty())
                ? indexFilename.trim()
                : "META-INF/reflections/" + project.getName() + "-reflections.xml";
        resolved.add(primary);
        for (String extra : additionalIndexFilenames) {
            if (extra != null && !extra.trim().isEmpty() && !resolved.contains(extra.trim())) {
                resolved.add(extra.trim());
            }
        }
        return resolved;
    }

    @Override
    public String toString() {
        return "ReflectionsPluginExtension{"
                + "indexFilename='" + indexFilename + '\''
                + ", additionalIndexFilenames=" + additionalIndexFilenames
                + ", params=" + Arrays.toString(params)
                + ", packages=" + packages
                + ", inputFiles=" + inputFiles
                + ", outputDirectory=" + outputDirectory
                + '}';
    }
}
