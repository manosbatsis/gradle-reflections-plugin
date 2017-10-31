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
package io.github.manosbatsis.gradle.plugin.reflections.test.unit;

import static org.junit.Assert.assertNotNull;

import io.github.manosbatsis.gradle.plugin.reflections.ReflectionsMetadataEmbeddingTask;
import io.github.manosbatsis.gradle.plugin.reflections.ReflectionsPlugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.Test;

/**
 * Simple test for task discovery
 */
public class ReflectionsPluginTest {

	@Test
	public void testTaskDiscovery() {

		// apply "reflections" plugin
		Project project = ProjectBuilder.builder().build();
		project.getPlugins().apply(ReflectionsPlugin.PLUGIN_NAME);

		// check for "embed" task discovery
		Task embedTask = project.getTasks().findByPath(ReflectionsMetadataEmbeddingTask.TASK_NAME);
		assertNotNull("Task not found: " + ReflectionsMetadataEmbeddingTask.TASK_NAME, embedTask);
	}
}
