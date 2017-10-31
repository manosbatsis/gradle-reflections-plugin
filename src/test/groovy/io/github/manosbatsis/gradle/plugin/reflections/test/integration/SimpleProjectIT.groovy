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
package io.github.manosbatsis.gradle.plugin.reflections.test.integration

import io.github.manosbatsis.gradle.plugin.reflections.ReflectionsMetadataEmbeddingTask
import org.gradle.testkit.runner.GradleRunner

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

/**
 * Integration test executed on a gradle project located in <code>src/test/projects/simple</code>
 */
class SimpleProjectIT extends GroovyTestCase {

	def projectDir = new File(System.getProperty("user.dir") + "/src//test/projects/simple")
	def pluginClasspathResource = getClass().classLoader.findResource("plugin-classpath.txt")
	def pluginClasspath = pluginClasspathResource.readLines().collect { new File(it) }

	void testEmbed() {
		def result = GradleRunner.create()
				.withProjectDir(projectDir)
				.withPluginClasspath(pluginClasspath)
				.withArguments("clean", "classes", ReflectionsMetadataEmbeddingTask.TASK_NAME, "test", "jar")
				.build()

		// check for acceptable tasks outcome
		assertTrue(SUCCESS.equals(result.task(":" + ReflectionsMetadataEmbeddingTask.TASK_NAME).getOutcome()))
		// the tests in the simple project check for successful Reflections.collect()
		assertTrue(SUCCESS.equals(result.task(":test").getOutcome()))

	}
}