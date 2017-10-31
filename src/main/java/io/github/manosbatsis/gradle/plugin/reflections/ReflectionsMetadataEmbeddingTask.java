/*
 * Copyright (c) 2017 Manos Batsis <https://github.com/manosbatsis/gradle-reflections-plugin>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.manosbatsis.gradle.plugin.reflections;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import com.google.common.collect.ObjectArrays;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskAction;
import org.reflections.Reflections;

/**
 * Embeds a pre-scanned Reflections metadata index file into your project artifact
 */
public class ReflectionsMetadataEmbeddingTask extends DefaultTask {

	public static final String TASK_NAME = "reflections";

	/**
	 * Generates the Reflections metadata index file
	 */
	@TaskAction
	public void generateMetadataIndex() {
		Project project = this.getProject();

		// tell reflections to use the project logger
		Reflections.log = project.getLogger();

		// get extension config
		ReflectionsPluginExtension extension = getExtension();

		// obtain and initialize output directory
		File outputDir = resolveIndexFileDestinationDir();

		// create the Reflections index file
		Reflections reflections = new Reflections(ObjectArrays.concat(this.parseOutputDirUrl(), extension.getParams()));
		reflections.save(outputDir + File.separator + extension.getIndexFilename());

	}

	/**
	 * Get extension config
	 */
	private ReflectionsPluginExtension getExtension() {
		Project project = this.getProject();

		// get reflectionsConfig
		ReflectionsPluginExtension extension = project.getExtensions().findByType(ReflectionsPluginExtension.class);

		// fallback to defaults if missing
		if (extension == null) {
			extension = new ReflectionsPluginExtension();
		}

		// add index filename if missing
		if (extension.getIndexFilename() == null) {
			extension.setIndexFilename(project.getName() + "-reflections.xml");
		}

		return extension;
	}

	/**
	 * Obtain and initialize Reflections index output directory
	 */
	private File resolveIndexFileDestinationDir() {
		File javaOutputDir = resolveOutputDirectory();
		File outputDir = new File(javaOutputDir.getAbsolutePath() + File.separator + "META-INF" + File.separator + "reflections");
		outputDir.mkdirs();
		return outputDir;
	}

	/**
	 * Obtain the main Java output directory
	 */
	private File resolveOutputDirectory() {
		SourceSetContainer sources = (SourceSetContainer) this.getProject().getProperties().get("sourceSets");
		return sources.getByName("main").getJava().getOutputDir();
	}

	/**
	 * Obtain the main Java output directory as a <code>{@link URL}</code>
	 */
	private URL parseOutputDirUrl() {
		try {
			return resolveOutputDirectory().toURI().toURL();
		}
		catch (MalformedURLException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

}