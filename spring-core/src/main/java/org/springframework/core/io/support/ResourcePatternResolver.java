/*
 * Copyright 2002-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.core.io.support;

import java.io.IOException;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

/**
 * Strategy interface for resolving a location pattern (for example,
 * an Ant-style path pattern) into {@link Resource} objects.
 *
 * <p>This is an extension to the {@link org.springframework.core.io.ResourceLoader}
 * interface. A passed-in {@code ResourceLoader} (for example, an
 * {@link org.springframework.context.ApplicationContext} passed in via
 * {@link org.springframework.context.ResourceLoaderAware} when running in a context)
 * can be checked whether it implements this extended interface too.
 *
 * <p>{@link PathMatchingResourcePatternResolver} is a standalone implementation
 * that is usable outside an {@code ApplicationContext}, also used by
 * {@link ResourceArrayPropertyEditor} for populating {@code Resource} array bean
 * properties.
 *
 * <p>Can be used with any sort of location pattern &mdash; for example,
 * {@code "/WEB-INF/*-context.xml"}. However, input patterns have to match the
 * strategy implementation. This interface just specifies the conversion method
 * rather than a specific pattern format.
 *
 * <p>This interface also defines a {@value #CLASSPATH_ALL_URL_PREFIX} resource
 * prefix for all matching resources from the module path and the class path. Note
 * that the resource location may also contain placeholders &mdash; for example
 * {@code "/beans-*.xml"}. JAR files or different directories in the module path
 * or class path can contain multiple files of the same name.
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 1.0.2
 * @see org.springframework.core.io.Resource
 * @see org.springframework.core.io.ResourceLoader
 * @see org.springframework.context.ApplicationContext
 * @see org.springframework.context.ResourceLoaderAware
 */
// ResourcePatternResolver 是 Spring 框架中用于资源加载的高级接口。它继承了ResourceLoader，并将其功能从“加载单个资源”扩展到了“批量解析匹配资源”。
// 如果说 ResourceLoader 是一个精确的“定位器”，那么 ResourcePatternResolver 就是一个强大的“资源搜索引擎”。
// 支持模式匹配（Pattern Matching）：它支持使用通配符（如 Ant 风格的路径 /**/*.xml）来一次性搜索多个资源。
// 支持全路径搜索：通过特殊的协议前缀，它可以跨越多个 JAR 包和文件系统目录查找同名资源。
// ApplicationContext 的核心能力：所有的 ApplicationContext 实例都间接实现了这个接口。这也是为什么你可以在 Spring 中使用 classpath*:bean-*.xml 这种写法来加载配置。
// 独立实现：它拥有一个非常著名的独立实现类 PathMatchingResourcePatternResolver，可以在不启动 Spring 容器的情况下单独使用。
public interface ResourcePatternResolver extends ResourceLoader {

	/**
	 * Pseudo URL prefix for all matching resources from the class path: {@code "classpath*:"}.
	 * <p>This differs from ResourceLoader's {@code "classpath:"} URL prefix in
	 * that it retrieves all matching resources for a given path &mdash; for
	 * example, to locate all "beans.xml" files in the root of all deployed JAR
	 * files you can use the location pattern {@code "classpath*:/beans.xml"}.
	 * <p>As of Spring Framework 6.0, the semantics for the {@code "classpath*:"}
	 * prefix have been expanded to include the module path as well as the class path.
	 * @see org.springframework.core.io.ResourceLoader#CLASSPATH_URL_PREFIX
	 */
	// 作用：定义“加载所有匹配项”的类路径伪 URL 前缀。
	String CLASSPATH_ALL_URL_PREFIX = "classpath*:";

	/**
	 * Resolve the given location pattern into {@code Resource} objects.
	 * <p>Overlapping resource entries that point to the same physical
	 * resource should be avoided, as far as possible. The result should
	 * have set semantics.
	 * @param locationPattern the location pattern to resolve
	 * @return the corresponding {@code Resource} objects
	 * @throws IOException in case of I/O errors
	 */
	// 作用：将给定的位置模式（Location Pattern）解析为一组 Resource 对象。
	Resource[] getResources(String locationPattern) throws IOException;

}
