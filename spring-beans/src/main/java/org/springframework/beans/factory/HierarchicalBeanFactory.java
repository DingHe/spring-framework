/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.beans.factory;

import org.springframework.lang.Nullable;

/**
 * Sub-interface implemented by bean factories that can be part
 * of a hierarchy.
 *
 * <p>The corresponding {@code setParentBeanFactory} method for bean
 * factories that allow setting the parent in a configurable
 * fashion can be found in the ConfigurableBeanFactory interface.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 07.07.2003
 * @see org.springframework.beans.factory.config.ConfigurableBeanFactory#setParentBeanFactory
 */
// HierarchicalBeanFactory 是 Spring Bean 工厂体系中一个非常关键的子接口。它为容器引入了**父子层级（Hierarchy）**的概念，使 Spring 容器不再是孤立的，而可以像树结构一样进行组织。
// 该接口的主要目的是让 BeanFactory 具备“双亲委派”或“层级检索”的能力。
// 容器分层：允许创建一个 Bean 工厂作为另一个工厂的“父节点”。
// 搜索策略：当你在子容器中请求一个 Bean 时，如果子容器找不到，它会自动向父容器询问。这与 Java 类加载器的机制非常相似。
// 配置隔离与共享：
// 子容器可以看到父容器的 Bean。
// 父容器看不到子容器的 Bean。
public interface HierarchicalBeanFactory extends BeanFactory {

	/**
	 * Return the parent bean factory, or {@code null} if there is none.
	 */
	// 作用：获取当前 Bean 工厂的父级工厂。
	@Nullable
	BeanFactory getParentBeanFactory();

	/**
	 * Return whether the local bean factory contains a bean of the given name,
	 * ignoring beans defined in ancestor contexts.
	 * <p>This is an alternative to {@code containsBean}, ignoring a bean
	 * of the given name from an ancestor bean factory.
	 * @param name the name of the bean to query
	 * @return whether a bean with the given name is defined in the local factory
	 * @see BeanFactory#containsBean
	 */
	// 作用：判断**当前（本地）**工厂中是否包含指定名称的 Bean，忽略父级工厂。
	boolean containsLocalBean(String name);

}
