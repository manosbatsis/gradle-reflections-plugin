package io.github.manosbatsis.gradle.plugin.reflections;

import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskProvider;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Locale;
import java.util.function.Function;

/**
 * Configures AGP 8+ Android variants via reflection against the project's AGP {@link ClassLoader}.
 * Using reflection avoids classloader isolation issues when this plugin is loaded outside AGP's own
 * buildscript classloader (e.g. root vs subproject plugins or Gradle TestKit).
 */
final class AndroidReflectionsConfigurator {

    private AndroidReflectionsConfigurator() {
    }

    static void configure(Project project, ReflectionsPluginExtension extension) {
        Object androidComponents = project.getExtensions().findByName("androidComponents");
        if (androidComponents == null) {
            return;
        }
        try {
            ClassLoader agpLoader = androidComponents.getClass().getClassLoader();
            Class<?> variantSelectorClass = agpLoader.loadClass("com.android.build.api.variant.VariantSelector");
            Object selector = androidComponents.getClass().getMethod("selector").invoke(androidComponents);
            Object allVariants = selector.getClass().getMethod("all").invoke(selector);

            Method onVariants = findMethod(androidComponents.getClass(), "onVariants", variantSelectorClass, Action.class);
            Action<Object> callback = variant -> configureVariant(project, extension, agpLoader, variant);
            onVariants.invoke(androidComponents, allVariants, callback);
        } catch (Exception e) {
            throw new GradleException("Failed to configure Reflections indexing for Android variants", e);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void configureVariant(
            Project project,
            ReflectionsPluginExtension extension,
            ClassLoader agpLoader,
            Object variant) {
        try {
            String rawName = (String) variant.getClass().getMethod("getName").invoke(variant);
            String variantName = capitalize(rawName);
            TaskProvider<AndroidReflectionsIndexTask> taskProvider = project.getTasks().register(
                    "generate" + variantName + "ReflectionsIndex",
                    AndroidReflectionsIndexTask.class,
                    task -> {
                        task.setGroup("build");
                        task.setDescription("Pre-scans compiled classes for Reflections metadata index (" + rawName + ")");
                        task.getPackages().set(project.provider(extension::getPackages));
                        task.getIndexFilenames().set(project.provider(() -> extension.resolveIndexFilenames(project)));
                        task.getParams().set(project.provider(extension::getParams));
                    }
            );

            Object artifacts = variant.getClass().getMethod("getArtifacts").invoke(variant);
            Class<?> scopeEnum = agpLoader.loadClass("com.android.build.api.variant.ScopedArtifacts$Scope");
            Object projectScope = Enum.valueOf((Class) scopeEnum, "PROJECT");

            Method forScope = findMethod(artifacts.getClass(), "forScope", scopeEnum);
            Object scopedArtifacts = forScope.invoke(artifacts, projectScope);

            Method useTask = findMethod(scopedArtifacts.getClass(), "use", TaskProvider.class);
            Object operation = useTask.invoke(scopedArtifacts, taskProvider);

            Class<?> scopedArtifactClass = agpLoader.loadClass("com.android.build.api.artifact.ScopedArtifact");
            Class<?> classesArtifactClass = agpLoader.loadClass("com.android.build.api.artifact.ScopedArtifact$CLASSES");
            Object classesArtifact = classesArtifactClass.getField("INSTANCE").get(null);
            Class<?> function1Class = agpLoader.loadClass("kotlin.jvm.functions.Function1");

            Object jarsFn = function1Proxy(agpLoader, function1Class, task -> ((AndroidReflectionsIndexTask) task).getAllJars());
            Object dirsFn = function1Proxy(agpLoader, function1Class, task -> ((AndroidReflectionsIndexTask) task).getAllDirectories());

            Method toGet = findMethod(operation.getClass(), "toGet", scopedArtifactClass, function1Class, function1Class);
            toGet.invoke(operation, classesArtifact, jarsFn, dirsFn);

            Object sources = variant.getClass().getMethod("getSources").invoke(variant);
            Object resources = sources.getClass().getMethod("getResources").invoke(sources);
            if (resources != null) {
                Object outDirFn = function1Proxy(
                        agpLoader,
                        function1Class,
                        task -> ((AndroidReflectionsIndexTask) task).getOutputDirectory()
                );
                Method addGenerated = findMethod(resources.getClass(), "addGeneratedSourceDirectory", TaskProvider.class, function1Class);
                addGenerated.invoke(resources, taskProvider, outDirFn);
            }
        } catch (Exception e) {
            throw new GradleException("Failed to configure Reflections indexing task for Android variant", e);
        }
    }

    private static Object function1Proxy(
            ClassLoader agpLoader,
            Class<?> function1Class,
            Function<Object, Object> delegate) {
        return Proxy.newProxyInstance(
                agpLoader,
                new Class<?>[]{function1Class},
                (proxy, method, args) -> {
                    if ("invoke".equals(method.getName()) && args != null && args.length == 1) {
                        return delegate.apply(args[0]);
                    }
                    return null;
                }
        );
    }

    private static Method findMethod(Class<?> targetClass, String name, Class<?>... paramTypes)
            throws NoSuchMethodException {
        for (Method method : targetClass.getMethods()) {
            if (!method.getName().equals(name)) {
                continue;
            }
            Class<?>[] candidates = method.getParameterTypes();
            if (candidates.length != paramTypes.length) {
                continue;
            }
            boolean compatible = true;
            for (int i = 0; i < candidates.length; i++) {
                if (!candidates[i].isAssignableFrom(paramTypes[i])) {
                    compatible = false;
                    break;
                }
            }
            if (compatible) {
                return method;
            }
        }
        return targetClass.getMethod(name, paramTypes);
    }

    private static String capitalize(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return value.substring(0, 1).toUpperCase(Locale.ROOT) + value.substring(1);
    }
}
