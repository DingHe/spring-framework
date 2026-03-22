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

package org.springframework.web.context;

import jakarta.servlet.ServletContext;

import org.springframework.beans.factory.Aware;

/**
 * Interface to be implemented by any object that wishes to be notified of the
 * {@link ServletContext} (typically determined by the {@link WebApplicationContext})
 * that it runs in.
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 12.03.2004
 * @see ServletConfigAware
 */
// 在 Spring Web 框架中，ServletContextAware 是一个非常关键的 回调接口。它属于 Spring 的 Aware 接口族（如 BeanNameAware, ApplicationContextAware 等），专门用于 Web 开发环境。
// ServletContextAware 的核心作用是让一个 Bean 能够获取到原生 Servlet 容器的 ServletContext 对象。
// 在 Java Web 开发中，ServletContext 代表了整个 Web 应用的上下文环境。通过它，开发者可以：
// 获取 Web 应用的初始化参数（Context Init Parameters）。
// 访问 Web 资源（如 /WEB-INF/ 下的文件）。
// 获取文件的 MIME 类型。
// 在全局范围内（Application Scope）存储和读取共享属性。
public interface ServletContextAware extends Aware {

	/**
	 * Set the {@link ServletContext} that this object runs in.
	 * <p>Invoked after population of normal bean properties but before an init
	 * callback like InitializingBean's {@code afterPropertiesSet} or a
	 * custom init-method. Invoked after ApplicationContextAware's
	 * {@code setApplicationContext}.
	 * @param servletContext the ServletContext object to be used by this object
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet
	 * @see org.springframework.context.ApplicationContextAware#setApplicationContext
	 */
	// 该方法是容器用来向 Bean 传递 ServletContext 对象的“传送带”。当一个类实现了此接口，它通常会定义一个私有字段来保存这个传入的参数，以便在后续逻辑中使用。
	void setServletContext(ServletContext servletContext);

}
