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

package org.springframework.beans.factory.config;

import java.lang.reflect.Constructor;

import org.springframework.beans.BeansException;
import org.springframework.lang.Nullable;

/**
 * Extension of the {@link InstantiationAwareBeanPostProcessor} interface,
 * adding a callback for predicting the eventual type of a processed bean.
 *
 * <p><b>NOTE:</b> This interface is a special purpose interface, mainly for
 * internal use within the framework. In general, application-provided
 * post-processors should simply implement the plain {@link BeanPostProcessor}
 * interface.
 *
 * @author Juergen Hoeller
 * @since 2.0.3
 */
// 继承自 InstantiationAwareBeanPostProcessor，在原本干预“实例化”的基础上，进一步增加了对 Bean 类型预测、构造函数选择以及解决循环依赖的核心能力。
// 智能类型预测：在 Bean 真正被创建之前，提前告诉容器这个 Bean 最终会长成什么样（例如是否会被代理）。这对于某些需要根据类型进行依赖查找（Dependency Lookup）的场景至关重要。
// 构造函数推断：这是 Spring 自动装配（Autowiring）构造函数的底层入口。它允许处理器决定应该调用哪个构造函数来创建 Bean 实例。
// 循环依赖的终极杀手锏：通过 getEarlyBeanReference 方法，它支持在 Bean 尚未完全初始化时就暴露一个早期引用。这是 Spring 三级缓存机制解决循环依赖的关键点，特别是对于需要被 AOP 代理的循环依赖 Bean。
public interface SmartInstantiationAwareBeanPostProcessor extends InstantiationAwareBeanPostProcessor {

	/**
	 * Predict the type of the bean to be eventually returned from this
	 * processor's {@link #postProcessBeforeInstantiation} callback.
	 * <p>The default implementation returns {@code null}.
	 * Specific implementations should try to predict the bean type as
	 * far as known/cached already, without extra processing steps.
	 * @param beanClass the raw class of the bean
	 * @param beanName the name of the bean
	 * @return the type of the bean, or {@code null} if not predictable
	 * @throws org.springframework.beans.BeansException in case of errors
	 */
	// 执行时机：在 Bean 实例创建之前的任意时刻，当容器需要知道 Bean 类型时调用。
	// 作用：尝试预测 Bean 的最终类型。它主要用于快速检查，不应包含复杂的计算。
	@Nullable
	default Class<?> predictBeanType(Class<?> beanClass, String beanName) throws BeansException {
		return null;
	}

	/**
	 * Determine the type of the bean to be eventually returned from this
	 * processor's {@link #postProcessBeforeInstantiation} callback.
	 * <p>The default implementation returns the given bean class as-is.
	 * Specific implementations should fully evaluate their processing steps
	 * in order to create/initialize a potential proxy class upfront.
	 * @param beanClass the raw class of the bean
	 * @param beanName the name of the bean
	 * @return the type of the bean (never {@code null})
	 * @throws org.springframework.beans.BeansException in case of errors
	 * @since 6.0
	 */
	// 执行时机：Spring 6.0 新增方法，作为 predictBeanType 的增强版。
	// 作用：更明确地确定 Bean 的最终类型。
	default Class<?> determineBeanType(Class<?> beanClass, String beanName) throws BeansException {
		return beanClass;
	}

	/**
	 * Determine the candidate constructors to use for the given bean.
	 * <p>The default implementation returns {@code null}.
	 * @param beanClass the raw class of the bean (never {@code null})
	 * @param beanName the name of the bean
	 * @return the candidate constructors, or {@code null} if none specified
	 * @throws org.springframework.beans.BeansException in case of errors
	 */
	// 执行时机：在 postProcessBeforeInstantiation 之后，但在真正调用构造函数 new 对象之前。
	@Nullable
	default Constructor<?>[] determineCandidateConstructors(Class<?> beanClass, String beanName)
			throws BeansException {

		return null;
	}

	/**
	 * Obtain a reference for early access to the specified bean,
	 * typically for the purpose of resolving a circular reference.
	 * <p>This callback gives post-processors a chance to expose a wrapper
	 * early - that is, before the target bean instance is fully initialized.
	 * The exposed object should be equivalent to the what
	 * {@link #postProcessBeforeInitialization} / {@link #postProcessAfterInitialization}
	 * would expose otherwise. Note that the object returned by this method will
	 * be used as bean reference unless the post-processor returns a different
	 * wrapper from said post-process callbacks. In other words: Those post-process
	 * callbacks may either eventually expose the same reference or alternatively
	 * return the raw bean instance from those subsequent callbacks (if the wrapper
	 * for the affected bean has been built for a call to this method already,
	 * it will be exposes as final bean reference by default).
	 * <p>The default implementation returns the given {@code bean} as-is.
	 * @param bean the raw bean instance
	 * @param beanName the name of the bean
	 * @return the object to expose as bean reference
	 * (typically with the passed-in bean instance as default)
	 * @throws org.springframework.beans.BeansException in case of errors
	 */
	// 执行时机：在 Bean 实例化之后，但在填充属性之前，仅当发生循环依赖时被三级缓存中的 ObjectFactory 调用。
	default Object getEarlyBeanReference(Object bean, String beanName) throws BeansException {
		return bean;
	}

}
