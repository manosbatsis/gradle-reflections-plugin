package io.github.manosbatsis.gradle.plugin.reflections;

import org.gradle.api.GradleException;
import org.reflections.Reflections;
import org.reflections.scanners.Scanner;
import org.reflections.serializers.JsonSerializer;
import org.reflections.serializers.Serializer;
import org.reflections.serializers.XmlSerializer;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

/**
 * Shared Reflections scanning and index serialization helper used by both JVM and Android tasks.
 */
final class ReflectionsIndexGenerator {

    private ReflectionsIndexGenerator() {
    }

    @SuppressWarnings("unchecked")
    static void generateIndex(
            Collection<File> classRoots,
            List<String> packages,
            Object[] params,
            File outputDirectory,
            List<String> relativeIndexFilenames) {

        List<URL> urls = new ArrayList<>();
        if (classRoots != null) {
            for (File root : classRoots) {
                if (root != null && root.exists()) {
                    try {
                        urls.add(root.toURI().toURL());
                    } catch (MalformedURLException e) {
                        throw new GradleException("Invalid class root path: " + root, e);
                    }
                }
            }
        }

        List<Object> flattenedParams = flattenParams(params);
        List<ClassLoader> customLoaders = new ArrayList<>();
        for (Object param : flattenedParams) {
            if (param instanceof ClassLoader) {
                customLoaders.add((ClassLoader) param);
            } else if (param instanceof URL) {
                urls.add((URL) param);
            }
        }

        if (urls.isEmpty() && customLoaders.isEmpty() && (packages == null || packages.isEmpty())) {
            throw new GradleException("No compiled class inputs or scan packages were found for Reflections indexing.");
        }

        URLClassLoader scanClassLoader = urls.isEmpty()
                ? null
                : new URLClassLoader(urls.toArray(new URL[0]), ReflectionsIndexGenerator.class.getClassLoader());
        try {
            ConfigurationBuilder config = new ConfigurationBuilder();
            if (!urls.isEmpty()) {
                config.addUrls(urls);
            }
            if (scanClassLoader != null) {
                config.addClassLoaders(scanClassLoader);
            }
            if (!customLoaders.isEmpty()) {
                config.addClassLoaders(customLoaders.toArray(new ClassLoader[0]));
            }

            FilterBuilder packageFilter = new FilterBuilder();
            boolean hasPackageFilter = false;
            if (packages != null) {
                for (String pkg : packages) {
                    if (pkg != null && !pkg.trim().isEmpty()) {
                        String trimmed = pkg.trim();
                        packageFilter.includePackage(trimmed);
                        hasPackageFilter = true;
                        if (urls.isEmpty()) {
                            config.forPackage(trimmed, config.getClassLoaders());
                        }
                    }
                }
            }

            Predicate<String> explicitPredicate = null;
            for (Object param : flattenedParams) {
                if (param instanceof String && !((String) param).trim().isEmpty()) {
                    String pkg = ((String) param).trim();
                    packageFilter.includePackage(pkg);
                    hasPackageFilter = true;
                    if (urls.isEmpty()) {
                        config.forPackage(pkg, config.getClassLoaders());
                    }
                } else if (param instanceof Scanner) {
                    config.addScanners((Scanner) param);
                } else if (param instanceof Class && Scanner.class.isAssignableFrom((Class<?>) param)) {
                    try {
                        config.addScanners(((Class<? extends Scanner>) param).getDeclaredConstructor().newInstance());
                    } catch (Exception e) {
                        throw new GradleException("Unable to instantiate scanner class: " + param, e);
                    }
                } else if (param instanceof Predicate) {
                    explicitPredicate = (Predicate<String>) param;
                }
            }

            if (explicitPredicate != null) {
                config.filterInputsBy(explicitPredicate);
            } else if (hasPackageFilter) {
                config.filterInputsBy(packageFilter);
            }

            Reflections reflections = new Reflections(config);
            if (!outputDirectory.exists() && !outputDirectory.mkdirs()) {
                throw new GradleException("Failed to create Reflections output directory: " + outputDirectory);
            }

            for (String relativeFilename : relativeIndexFilenames) {
                if (relativeFilename == null || relativeFilename.trim().isEmpty()) {
                    continue;
                }
                File targetFile = new File(outputDirectory, relativeFilename.trim());
                Serializer serializer = selectSerializer(relativeFilename);
                reflections.save(targetFile.getAbsolutePath(), serializer);
            }
        } finally {
            if (scanClassLoader != null) {
                try {
                    scanClassLoader.close();
                } catch (IOException ignored) {
                    // Best-effort cleanup of temporary scanning classloader
                }
            }
        }
    }

    static Serializer selectSerializer(String filename) {
        if (filename != null && filename.trim().toLowerCase(Locale.ROOT).endsWith(".json")) {
            return new JsonSerializer();
        }
        return new XmlSerializer();
    }

    private static List<Object> flattenParams(Object[] params) {
        List<Object> flattened = new ArrayList<>();
        if (params == null) {
            return flattened;
        }
        for (Object param : params) {
            flattenSingle(param, flattened);
        }
        return flattened;
    }

    private static void flattenSingle(Object param, List<Object> target) {
        if (param == null) {
            return;
        }
        if (param.getClass().isArray()) {
            for (Object element : (Object[]) param) {
                flattenSingle(element, target);
            }
        } else if (param instanceof Iterable) {
            for (Object element : (Iterable<?>) param) {
                flattenSingle(element, target);
            }
        } else {
            target.add(param);
        }
    }
}

