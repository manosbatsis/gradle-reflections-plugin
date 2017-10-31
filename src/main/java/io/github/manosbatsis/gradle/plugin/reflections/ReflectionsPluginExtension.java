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

import lombok.Data;
import lombok.NoArgsConstructor;
import org.reflections.Reflections;

/**
 * Gradle configuration model and corresponding default values
 * for the {@link ReflectionsPlugin}
 */
@NoArgsConstructor
@Data
public class ReflectionsPluginExtension {

	/**
	 * Gradle config section name
	 */
	public static final String EXTENSION_NAME = "reflectionsConfig";

	/**
	 * File name for the Reflections metadata index.
	 * If empty, will default to <code>PROJECTNAME-reflections.xml</code>
	 * where PROJECTNAME is the name of the Gradle project executing the plugin.
	 */
	private String indexFilename = null;

	/**
	 * An array of objects for convenient of a {@link Reflections}, where given {@code Object...} parameter types can be either:
	 * <ul>
	 *     <li>{@link String} - would add urls using {@link org.reflections.util.ClasspathHelper#forPackage(String, ClassLoader...)} ()}</li>
	 *     <li>{@link Class} - would add urls using {@link org.reflections.util.ClasspathHelper#forClass(Class, ClassLoader...)} </li>
	 *     <li>{@link ClassLoader} - would use this classloaders in order to find urls in {@link org.reflections.util.ClasspathHelper#forPackage(String, ClassLoader...)} and {@link org.reflections.util.ClasspathHelper#forClass(Class, ClassLoader...)}</li>
	 *     <li>{@link org.reflections.scanners.Scanner} - would use given scanner, overriding the default scanners</li>
	 *     <li>{@link java.net.URL} - would add the given url for scanning</li>
	 *     <li>{@link Object[]} - would use each element as above</li>
	 * </ul>
	 *
	 * Parameter types can be used in any order. <strong>There is no need to add a param for the project java source/class files.</strong>
	 *
	 * @see Reflections#Reflections(java.lang.Object...)
	 */
	private Object[] params = {};

}