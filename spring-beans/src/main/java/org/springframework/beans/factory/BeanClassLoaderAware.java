/*
 * Copyright 2002-2017 the original author or authors.
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
 * Callback that allows a bean to be aware of the bean
 * {@link ClassLoader class loader}; that is, the class loader used by the
 * present bean factory to load bean classes.
 *
 * <p>This is mainly intended to be implemented by framework classes which
 * have to pick up application classes by name despite themselves potentially
 * being loaded from a shared class loader.
 *
 * <p>For a list of all bean lifecycle methods, see the
 * {@link BeanFactory BeanFactory javadocs}.
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 2.0
 * @see BeanNameAware
 * @see BeanFactoryAware
 * @see InitializingBean
 */
// BeanClassLoaderAware 的核心作用是让 Bean 能够访问容器用于加载 Bean 定义的 ClassLoader。
// 解决类可见性问题：在复杂的应用场景中（如 OSGi、热部署框架或共享库环境），框架类可能由“父加载器”加载，而应用类由“子加载器”加载。如果框架类需要通过类名反射创建应用类，直接使用 Class.forName() 可能会失败。通过此接口获取 ClassLoader，可以确保正确加载应用资源。
// 动态代理与反射：许多底层基础设施（如 AOP 代理生成、序列化工具、动态脚本解析）在运行时需要生成新的类或寻找特定的类，这些操作都依赖于正确的 ClassLoader。
public interface BeanClassLoaderAware extends Aware {

	/**
	 * Callback that supplies the bean {@link ClassLoader class loader} to
	 * a bean instance.
	 * <p>Invoked <i>after</i> the population of normal bean properties but
	 * <i>before</i> an initialization callback such as
	 * {@link InitializingBean InitializingBean's}
	 * {@link InitializingBean#afterPropertiesSet()}
	 * method or a custom init-method.
	 * @param classLoader the owning class loader
	 */
	// 作用：由 Spring 容器自动调用，将当前 Bean Factory 使用的 ClassLoader 注入到 Bean 实例中。
	void setBeanClassLoader(ClassLoader classLoader);

}
