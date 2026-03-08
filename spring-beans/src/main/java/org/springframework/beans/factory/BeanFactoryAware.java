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

import org.springframework.beans.BeansException;

/**
 * Interface to be implemented by beans that wish to be aware of their
 * owning {@link BeanFactory}.
 *
 * <p>For example, beans can look up collaborating beans via the factory
 * (Dependency Lookup). Note that most beans will choose to receive references
 * to collaborating beans via corresponding bean properties or constructor
 * arguments (Dependency Injection).
 *
 * <p>For a list of all bean lifecycle methods, see the
 * {@link BeanFactory BeanFactory javadocs}.
 *
 * @author Rod Johnson
 * @author Chris Beams
 * @since 11.03.2003
 * @see BeanNameAware
 * @see BeanClassLoaderAware
 * @see InitializingBean
 * @see org.springframework.context.ApplicationContextAware
 */
// BeanFactoryAware 的核心作用是让 Bean 能够“感知”到管理它的容器（BeanFactory）。
// 打破控制反转（IoC）的封闭性：通常情况下，Bean 是被动接受依赖注入（DI）的，它不知道自己身处哪个容器中。实现此接口后，Bean 主动获得了容器的引用。
// 编程式依赖查找（Dependency Lookup）：虽然 Spring 推荐使用自动注入（@Autowired），但在某些特殊场景下（例如：需要根据运行时的参数动态获取不同类型的 Bean），Bean 可以通过持有的 BeanFactory 实例直接调用 getBean()。
// 低级别容器访问：它提供的是对 BeanFactory 的访问，这比 ApplicationContextAware 更底层。它通常用于那些不需要完整的应用上下文功能（如国际化、事件广播），而只需要基础 Bean 管理功能的底层基础设施类。

public interface BeanFactoryAware extends Aware {

	/**
	 * Callback that supplies the owning factory to a bean instance.
	 * <p>Invoked after the population of normal bean properties
	 * but before an initialization callback such as
	 * {@link InitializingBean#afterPropertiesSet()} or a custom init-method.
	 * @param beanFactory owning BeanFactory (never {@code null}).
	 * The bean can immediately call methods on the factory.
	 * @throws BeansException in case of initialization errors
	 * @see BeanInitializationException
	 */
	// 作用：由 Spring 容器自动调用，将当前的容器实例注入到 Bean 中。
	void setBeanFactory(BeanFactory beanFactory) throws BeansException;

}
