/*
 * Copyright 2002-2018 the original author or authors.
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

/**
 * A marker superinterface indicating that a bean is eligible to be notified by the
 * Spring container of a particular framework object through a callback-style method.
 * The actual method signature is determined by individual subinterfaces but should
 * typically consist of just one void-returning method that accepts a single argument.
 *
 * <p>Note that merely implementing {@link Aware} provides no default functionality.
 * Rather, processing must be done explicitly, for example in a
 * {@link org.springframework.beans.factory.config.BeanPostProcessor}.
 * Refer to {@link org.springframework.context.support.ApplicationContextAwareProcessor}
 * for an example of processing specific {@code *Aware} interface callbacks.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.1
 */
// 在 Spring 框架中，Aware 接口的设计极度简洁，但它在整个 IoC 容器的生命周期中扮演着至关重要的“感知器”角色。
// Aware 接口本身是一个标记接口（Marker Interface），内部没有任何方法定义。它的主要作用如下：
// 身份标识：它告诉 Spring 容器，实现此接口的 Bean 希望被注入容器内部的某些特定资源（如 BeanFactory、ApplicationContext 等）。
// 回调约定：虽然 Aware 接口为空，但它约定了所有的子接口（如 BeanNameAware、BeanFactoryAware）都应该遵循一种特定的回调风格：单参数、无返回值（void）的方法。
// 解耦容器与业务：通过实现特定的 Aware 接口，Bean 可以以一种非侵入性的方式获取容器资源，而不需要通过反射或复杂的配置。
public interface Aware {

}
