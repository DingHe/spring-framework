/*
 * Copyright 2002-2021 the original author or authors.
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

import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValues;
import org.springframework.lang.Nullable;

/**
 * Subinterface of {@link BeanPostProcessor} that adds a before-instantiation callback,
 * and a callback after instantiation but before explicit properties are set or
 * autowiring occurs.
 *
 * <p>Typically used to suppress default instantiation for specific target beans,
 * for example to create proxies with special TargetSources (pooling targets,
 * lazily initializing targets, etc), or to implement additional injection strategies
 * such as field injection.
 *
 * <p><b>NOTE:</b> This interface is a special purpose interface, mainly for
 * internal use within the framework. It is recommended to implement the plain
 * {@link BeanPostProcessor} interface as far as possible.
 *
 * @author Juergen Hoeller
 * @author Rod Johnson
 * @since 1.2
 * @see org.springframework.aop.framework.autoproxy.AbstractAutoProxyCreator#setCustomTargetSourceCreators
 * @see org.springframework.aop.framework.autoproxy.target.LazyInitTargetSourceCreator
 */
// InstantiationAwareBeanPostProcessor 是 Spring 容器中一个非常强大且特殊的后置处理器接口。它是 BeanPostProcessor 的子接口。
// 在理解这个类之前，必须先区分两个概念：实例化（Instantiation） 和 初始化（Initialization）。
// 实例化：指在堆内存中创建对象实例（相当于 new 对象）。
// 初始化：指对象已经创建好，开始注入属性、执行各种 init 方法。
// 这个接口的主要作用是干预 Bean 的生命周期中“实例化”阶段的前后：
// 短路实例化（Short-circuiting）：它允许你在 Spring 默认实例化逻辑运行之前，直接返回一个自定义的对象（通常是代理对象）。如果此方法返回了对象，Spring 将不再执行标准实例化过程。
// 属性注入前的最后机会：在对象 new 出来之后，但在 Spring 开始自动装配（Autowiring）或设置显式属性值之前，提供回调。
// 属性值修改：它允许你在属性应用到 Bean 之前，对属性值进行修改、添加或删除。
public interface InstantiationAwareBeanPostProcessor extends BeanPostProcessor {

	/**
	 * Apply this BeanPostProcessor <i>before the target bean gets instantiated</i>.
	 * The returned bean object may be a proxy to use instead of the target bean,
	 * effectively suppressing default instantiation of the target bean.
	 * <p>If a non-null object is returned by this method, the bean creation process
	 * will be short-circuited. The only further processing applied is the
	 * {@link #postProcessAfterInitialization} callback from the configured
	 * {@link BeanPostProcessor BeanPostProcessors}.
	 * <p>This callback will be applied to bean definitions with their bean class,
	 * as well as to factory-method definitions in which case the returned bean type
	 * will be passed in here.
	 * <p>Post-processors may implement the extended
	 * {@link SmartInstantiationAwareBeanPostProcessor} interface in order
	 * to predict the type of the bean object that they are going to return here.
	 * <p>The default implementation returns {@code null}.
	 * @param beanClass the class of the bean to be instantiated
	 * @param beanName the name of the bean
	 * @return the bean object to expose instead of a default instance of the target bean,
	 * or {@code null} to proceed with default instantiation
	 * @throws org.springframework.beans.BeansException in case of errors
	 * @see #postProcessAfterInstantiation
	 * @see org.springframework.beans.factory.support.AbstractBeanDefinition#getBeanClass()
	 * @see org.springframework.beans.factory.support.AbstractBeanDefinition#getFactoryMethodName()
	 */
	// 执行时机：在目标 Bean 被实例化之前调用。
	// 你可以通过这个方法返回一个对象（例如通过 RPC 获取的远程代理，或者通过 AOP 创建的代理）。
	// 返回值：返回替代用的 Bean 实例，或返回 null（默认值）以继续执行 Spring 标准的实例化流程。
	@Nullable
	default Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) throws BeansException {
		return null;
	}

	/**
	 * Perform operations after the bean has been instantiated, via a constructor or factory method,
	 * but before Spring property population (from explicit properties or autowiring) occurs.
	 * <p>This is the ideal callback for performing custom field injection on the given bean
	 * instance, right before Spring's autowiring kicks in.
	 * <p>The default implementation returns {@code true}.
	 * @param bean the bean instance created, with properties not having been set yet
	 * @param beanName the name of the bean
	 * @return {@code true} if properties should be set on the bean; {@code false}
	 * if property population should be skipped. Normal implementations should return {@code true}.
	 * Returning {@code false} will also prevent any subsequent InstantiationAwareBeanPostProcessor
	 * instances being invoked on this bean instance.
	 * @throws org.springframework.beans.BeansException in case of errors
	 * @see #postProcessBeforeInstantiation
	 */
	// 执行时机：在 Bean 实例化之后，但在 Spring 属性填充（Property Population）之前调用。此时 Bean 已经有了实例，但属性还是空的（或默认值）。
	// 用于在自动装配开始前执行自定义的字段注入逻辑。
	// 控制是否需要继续填充属性。
	// true（默认值）：允许 Spring 继续正常的属性注入过程。
	default boolean postProcessAfterInstantiation(Object bean, String beanName) throws BeansException {
		return true;
	}

	/**
	 * Post-process the given property values before the factory applies them
	 * to the given bean.
	 * <p>The default implementation returns the given {@code pvs} as-is.
	 * @param pvs the property values that the factory is about to apply (never {@code null})
	 * @param bean the bean instance created, but whose properties have not yet been set
	 * @param beanName the name of the bean
	 * @return the actual property values to apply to the given bean (can be the passed-in
	 * PropertyValues instance), or {@code null} to skip property population
	 * @throws org.springframework.beans.BeansException in case of errors
	 * @since 5.1
	 */
	// 执行时机：在 Spring 准备将属性值应用到 Bean 之前调用。
	// 这是对属性值进行最后检查或修改的地方。
	// 核心应用：Spring 内部处理 @Autowired、@Resource 和 @Value 的核心逻辑（AutowiredAnnotationBeanPostProcessor）就是在此方法中完成的。它会在此处解析注解并查找依赖，最后返回修改后的属性值。
	@Nullable
	default PropertyValues postProcessProperties(PropertyValues pvs, Object bean, String beanName)
			throws BeansException {

		return pvs;
	}

}
