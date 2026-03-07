/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.context;

import org.springframework.beans.factory.HierarchicalBeanFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.core.env.EnvironmentCapable;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.lang.Nullable;

/**
 * Central interface to provide configuration for an application.
 * This is read-only while the application is running, but may be
 * reloaded if the implementation supports this.
 *
 * <p>An ApplicationContext provides:
 * <ul>
 * <li>Bean factory methods for accessing application components.
 * Inherited from {@link org.springframework.beans.factory.ListableBeanFactory}.
 * <li>The ability to load file resources in a generic fashion.
 * Inherited from the {@link org.springframework.core.io.ResourceLoader} interface.
 * <li>The ability to publish events to registered listeners.
 * Inherited from the {@link ApplicationEventPublisher} interface.
 * <li>The ability to resolve messages, supporting internationalization.
 * Inherited from the {@link MessageSource} interface.
 * <li>Inheritance from a parent context. Definitions in a descendant context
 * will always take priority. This means, for example, that a single parent
 * context can be used by an entire web application, while each servlet has
 * its own child context that is independent of that of any other servlet.
 * </ul>
 *
 * <p>In addition to standard {@link org.springframework.beans.factory.BeanFactory}
 * lifecycle capabilities, ApplicationContext implementations detect and invoke
 * {@link ApplicationContextAware} beans as well as {@link ResourceLoaderAware},
 * {@link ApplicationEventPublisherAware} and {@link MessageSourceAware} beans.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see ConfigurableApplicationContext
 * @see org.springframework.beans.factory.BeanFactory
 * @see org.springframework.core.io.ResourceLoader
 */
// ApplicationContext 是 Spring Framework 中最核心的接口，它是整个 Spring 容器的“面孔”。如果你把 BeanFactory 理解为生产 Bean 的“工厂”，那么 ApplicationContext 就是一个完整的“中央控制系统”。
// ApplicationContext 意为“应用上下文”。它不仅仅管理 Bean，还整合了企业级开发所需的多种核心功能。它的主要职责可以概括为：
// Bean 管理中心：继承了 ListableBeanFactory 和 HierarchicalBeanFactory，具备按类型检索、父子容器管理等能力。
// 配置抽象层：继承了 EnvironmentCapable，可以统一处理 Profiles 和属性配置（Properties）。
// 国际化支持：继承了 MessageSource，能够处理多语言文本。
// 事件传播机制：继承了 ApplicationEventPublisher，支持发布和监听容器内的事件。
// 资源加载器：继承了 ResourcePatternResolver（本质是 ResourceLoader），能以通配符方式加载各种物理资源。
// 感知接口处理：它能自动检测并执行 ApplicationContextAware 等 Aware 接口，将容器自身注入到 Bean 中。
public interface ApplicationContext extends EnvironmentCapable, ListableBeanFactory, HierarchicalBeanFactory,
		MessageSource, ApplicationEventPublisher, ResourcePatternResolver {

	/**
	 * Return the unique id of this application context.
	 * @return the unique id of the context, or {@code null} if none
	 */
	// 作用：获取当前应用上下文的唯一 ID。
	// 说明：每个容器在运行时都有一个标识符（如 application:8080），通常用于集群监控或区分多个并存的容器。可能返回 null。
	@Nullable
	String getId();

	/**
	 * Return a name for the deployed application that this context belongs to.
	 * @return a name for the deployed application, or the empty String by default
	 */
	// 作用：获取当前部署的应用程序名称。
	// 说明：默认返回空字符串。在 Web 环境（如 Spring Boot）中，它通常对应 spring.application.name 配置的值。
	String getApplicationName();

	/**
	 * Return a friendly name for this context.
	 * @return a display name for this context (never {@code null})
	 */
	// 作用：获取该上下文的一个“友好名称”。
	String getDisplayName();

	/**
	 * Return the timestamp when this context was first loaded.
	 * @return the timestamp (ms) when this context was first loaded
	 */
	// 作用：获取上下文第一次加载（刷新完成）时的时间戳（毫秒）。
	// 说明：可以用来计算应用的运行时间（Uptime）。
	long getStartupDate();

	/**
	 * Return the parent context, or {@code null} if there is no parent
	 * and this is the root of the context hierarchy.
	 * @return the parent context, or {@code null} if there is no parent
	 */
	// 作用：获取父级应用上下文。
	// 说明：如果当前是根容器（Root Context），则返回 null。Spring 支持容器继承，子容器可以访问父容器的 Bean，反之则不行。
	@Nullable
	ApplicationContext getParent();

	/**
	 * Expose AutowireCapableBeanFactory functionality for this context.
	 * <p>This is not typically used by application code, except for the purpose of
	 * initializing bean instances that live outside the application context,
	 * applying the Spring bean lifecycle (fully or partly) to them.
	 * <p>Alternatively, the internal BeanFactory exposed by the
	 * {@link ConfigurableApplicationContext} interface offers access to the
	 * {@link AutowireCapableBeanFactory} interface too. The present method mainly
	 * serves as a convenient, specific facility on the ApplicationContext interface.
	 * <p><b>NOTE: As of 4.2, this method will consistently throw IllegalStateException
	 * after the application context has been closed.</b> In current Spring Framework
	 * versions, only refreshable application contexts behave that way; as of 4.2,
	 * all application context implementations will be required to comply.
	 * @return the AutowireCapableBeanFactory for this context
	 * @throws IllegalStateException if the context does not support the
	 * {@link AutowireCapableBeanFactory} interface, or does not hold an
	 * autowire-capable bean factory yet (e.g. if {@code refresh()} has
	 * never been called), or if the context has been closed already
	 * @see ConfigurableApplicationContext#refresh()
	 * @see ConfigurableApplicationContext#getBeanFactory()
	 */
	// 作用：暴露容器内部的“具备自动装配能力”的 Bean 工厂。
	AutowireCapableBeanFactory getAutowireCapableBeanFactory() throws IllegalStateException;

}
