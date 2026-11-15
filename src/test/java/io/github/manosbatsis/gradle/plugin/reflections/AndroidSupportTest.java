package io.github.manosbatsis.gradle.plugin.reflections;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Verifies custom directory indexing, JSON + XML metadata output, and AGP 8+ Android support.
 */
public class AndroidSupportTest {

    @Rule
    public final TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testGeneratesJsonAndXmlIndexesIntoCustomOutputDirectory() throws IOException {
        File projectDir = tempFolder.newFolder("custom-jvm");
        writeFile(new File(projectDir, "settings.gradle"), "rootProject.name = 'custom-jvm'\n");
        writeFile(new File(projectDir, "build.gradle"),
                "plugins {\n"
                        + "    id 'io.github.manosbatsis.gradle.plugin.reflections'\n"
                        + "    id 'java'\n"
                        + "}\n"
                        + "repositories {\n"
                        + "    mavenCentral()\n"
                        + "}\n"
                        + "reflectionsConfig {\n"
                        + "    indexFilename = 'META-INF/reflections/custom-reflections.json'\n"
                        + "    additionalIndexFilenames = ['META-INF/reflections/custom-reflections.xml']\n"
                        + "    packages = ['example.demo']\n"
                        + "}\n");

        File srcDir = new File(projectDir, "src/main/java/example/demo");
        srcDir.mkdirs();
        writeFile(new File(srcDir, "DemoMarker.java"),
                "package example.demo;\n"
                        + "import java.lang.annotation.Retention;\n"
                        + "import java.lang.annotation.RetentionPolicy;\n"
                        + "@Retention(RetentionPolicy.RUNTIME)\n"
                        + "public @interface DemoMarker {}\n");
        writeFile(new File(srcDir, "SampleService.java"),
                "package example.demo;\n"
                        + "@DemoMarker\n"
                        + "public class SampleService {}\n");

        BuildResult result = GradleRunner.create()
                .withProjectDir(projectDir)
                .withArguments("clean", "reflections", "--stacktrace")
                .withPluginClasspath()
                .build();

        assertEquals(TaskOutcome.SUCCESS, result.task(":reflections").getOutcome());

        File jsonFile = new File(projectDir, "build/resources/main/META-INF/reflections/custom-reflections.json");
        File xmlFile = new File(projectDir, "build/resources/main/META-INF/reflections/custom-reflections.xml");
        assertTrue("Expected JSON index file to be created", jsonFile.isFile());
        assertTrue("Expected XML index file to be created", xmlFile.isFile());

        String jsonContent = Files.readString(jsonFile.toPath(), StandardCharsets.UTF_8);
        String xmlContent = Files.readString(xmlFile.toPath(), StandardCharsets.UTF_8);
        assertTrue(jsonContent.contains("example.demo.SampleService"));
        assertTrue(xmlContent.contains("example.demo.SampleService"));
    }

    @Test
    public void testAndroidLibraryVariantGeneratesReflectionsIndex() throws IOException {
        File androidSdk = findAndroidSdk();
        Assume.assumeTrue("Android SDK not found on host; skipping AGP integration test", androidSdk != null);

        File projectDir = tempFolder.newFolder("android-lib");
        writeFile(new File(projectDir, "local.properties"),
                "sdk.dir=" + androidSdk.getAbsolutePath().replace("\\", "\\\\") + "\n");
        writeFile(new File(projectDir, "settings.gradle"),
                "pluginManagement {\n"
                        + "    repositories {\n"
                        + "        google()\n"
                        + "        mavenCentral()\n"
                        + "        gradlePluginPortal()\n"
                        + "    }\n"
                        + "}\n"
                        + "rootProject.name = 'android-lib'\n");
        writeFile(new File(projectDir, "build.gradle"),
                "plugins {\n"
                        + "    id 'com.android.library' version '8.5.2'\n"
                        + "    id 'io.github.manosbatsis.gradle.plugin.reflections'\n"
                        + "}\n"
                        + "repositories {\n"
                        + "    google()\n"
                        + "    mavenCentral()\n"
                        + "}\n"
                        + "android {\n"
                        + "    namespace 'example.androidlib'\n"
                        + "    compileSdk 34\n"
                        + "    defaultConfig {\n"
                        + "        minSdk 24\n"
                        + "    }\n"
                        + "    compileOptions {\n"
                        + "        sourceCompatibility JavaVersion.VERSION_17\n"
                        + "        targetCompatibility JavaVersion.VERSION_17\n"
                        + "    }\n"
                        + "}\n"
                        + "reflectionsConfig {\n"
                        + "    packages = ['example.androidlib']\n"
                        + "    indexFilename = 'META-INF/reflections/android-lib-reflections.xml'\n"
                        + "    additionalIndexFilenames = ['META-INF/reflections/android-lib-reflections.json']\n"
                        + "}\n");

        File srcDir = new File(projectDir, "src/main/java/example/androidlib");
        srcDir.mkdirs();
        writeFile(new File(projectDir, "src/main/AndroidManifest.xml"), "<manifest/>\n");
        writeFile(new File(srcDir, "Entity.java"),
                "package example.androidlib;\n"
                        + "import java.lang.annotation.Retention;\n"
                        + "import java.lang.annotation.RetentionPolicy;\n"
                        + "@Retention(RetentionPolicy.RUNTIME)\n"
                        + "public @interface Entity {}\n");
        writeFile(new File(srcDir, "DemoEntity.java"),
                "package example.androidlib;\n"
                        + "@Entity\n"
                        + "public class DemoEntity {}\n");

        BuildResult result = GradleRunner.create()
                .withProjectDir(projectDir)
                .withArguments("generateDebugReflectionsIndex", "bundleDebugAar", "--stacktrace")
                .withPluginClasspath()
                .build();

        assertEquals(TaskOutcome.SUCCESS, result.task(":generateDebugReflectionsIndex").getOutcome());

        File aarFile = new File(projectDir, "build/outputs/aar/android-lib-debug.aar");
        assertTrue("Expected debug AAR to be produced", aarFile.isFile());

        boolean hasClassesJarWithXml = false;
        boolean hasClassesJarWithJson = false;
        try (JarFile aar = new JarFile(aarFile)) {
            Enumeration<JarEntry> entries = aar.entries();
            while (entries.hasMoreElements()) {
                String name = entries.nextElement().getName();
                if (name.endsWith("META-INF/reflections/android-lib-reflections.xml")) {
                    hasClassesJarWithXml = true;
                }
                if (name.endsWith("META-INF/reflections/android-lib-reflections.json")) {
                    hasClassesJarWithJson = true;
                }
            }
        }
        assertTrue("Expected XML index inside AAR or resources (" + aarFile + ")",
                hasClassesJarWithXml || hasEntryInClassesJar(aarFile, "META-INF/reflections/android-lib-reflections.xml"));
        assertTrue("Expected JSON index inside AAR or resources (" + aarFile + ")",
                hasClassesJarWithJson || hasEntryInClassesJar(aarFile, "META-INF/reflections/android-lib-reflections.json"));
    }

    private boolean hasEntryInClassesJar(File aarFile, String expectedEntry) throws IOException {
        File tempClassesJar = tempFolder.newFile("classes-" + System.nanoTime() + ".jar");
        try (JarFile aar = new JarFile(aarFile)) {
            JarEntry classesEntry = aar.getJarEntry("classes.jar");
            if (classesEntry == null) {
                return false;
            }
            Files.copy(aar.getInputStream(classesEntry), tempClassesJar.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
        try (JarFile classesJar = new JarFile(tempClassesJar)) {
            return classesJar.getJarEntry(expectedEntry) != null;
        }
    }

    private File findAndroidSdk() {
        String env = System.getenv("ANDROID_HOME");
        if (env != null && !env.isEmpty() && new File(env).isDirectory()) {
            return new File(env);
        }
        String sdkRoot = System.getenv("ANDROID_SDK_ROOT");
        if (sdkRoot != null && !sdkRoot.isEmpty() && new File(sdkRoot).isDirectory()) {
            return new File(sdkRoot);
        }
        String localAppData = System.getenv("LOCALAPPDATA");
        if (localAppData != null) {
            File candidate = new File(localAppData, "Android/Sdk");
            if (candidate.isDirectory()) {
                return candidate;
            }
        }
        return null;
    }

    private void writeFile(File target, String content) throws IOException {
        if (target.getParentFile() != null) {
            target.getParentFile().mkdirs();
        }
        Files.writeString(target.toPath(), content, StandardCharsets.UTF_8);
    }
}

