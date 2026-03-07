/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.core;

/**
 * Common interface for managing aliases. Serves as a super-interface for
 * {@link org.springframework.beans.factory.support.BeanDefinitionRegistry}.
 *
 * @author Juergen Hoeller
 * @since 2.5.2
 */
// AliasRegistry 是 Spring 核心包中的一个基础接口，它定义了管理**别名（Alias）**的标准行为。
// 在 Spring 的体系结构中，它是 BeanDefinitionRegistry 的父接口，这意味着所有的 Bean 注册中心都天然具备管理别名的能力。
// 在 Spring 容器中，一个 Bean 通常有一个唯一的“规范名称（Canonical Name）”。但在实际开发中，我们可能希望通过不同的名称来引用同一个 Bean。
// 解耦命名：允许同一组件在不同的上下文或模块中使用不同的名称。
// 管理映射：它维护了一套从“别名”到“规范名称”的映射关系，确保无论使用哪个别名，最终都能指向唯一的 Bean 实例。
// 作为基石：它为更复杂的容器操作（如 Bean 的注册和获取）提供了最基础的命名解析支持。
public interface AliasRegistry {

	/**
	 * Given a name, register an alias for it.
	 * @param name the canonical name
	 * @param alias the alias to be registered
	 * @throws IllegalStateException if the alias is already in use
	 * and may not be overridden
	 */
	// 作用：为一个规范名称（name）注册一个别名（alias）。
	void registerAlias(String name, String alias);

	/**
	 * Remove the specified alias from this registry.
	 * @param alias the alias to remove
	 * @throws IllegalStateException if no such alias was found
	 */
	// 作用：从注册表中移除指定的别名。
	void removeAlias(String alias);

	/**
	 * Determine whether the given name is defined as an alias
	 * (as opposed to the name of an actually registered component).
	 * @param name the name to check
	 * @return whether the given name is an alias
	 */
	// 作用：判断给定的名称是否是一个已注册的“别名”。
	boolean isAlias(String name);

	/**
	 * Return the aliases for the given name, if defined.
	 * @param name the name to check for aliases
	 * @return the aliases, or an empty array if none
	 */
	String[] getAliases(String name);

}
