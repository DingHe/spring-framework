/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.context.annotation;

/**
 * Common interface for annotation config application contexts,
 * defining {@link #register} and {@link #scan} methods.
 *
 * @author Juergen Hoeller
 * @since 4.1
 */
// 定义了如何通过“注解配置”的方式向 Spring 容器注册 Bean。
// AnnotationConfigRegistry 的主要作用是为**基于注解的应用上下文（Application Context）**提供统一的注册和扫描规范。
// 统一入口：它定义了两种最常见的引入配置方式：手动注册指定的类，或者自动扫描指定的包。
// 支持编程式配置：它允许开发者在不使用 XML 的情况下，通过 Java 代码动态地向容器添加配置类（@Configuration）或组件类（@Component）。
// 核心实现：我们常用的 AnnotationConfigApplicationContext 和 AnnotationConfigWebApplicationContext 都实现了这个接口。
// 该接口只包含两个核心方法，它们是 Spring 注解驱动开发的基石：
public interface AnnotationConfigRegistry {

	/**
	 * Register one or more component classes to be processed.
	 * <p>Calls to {@code register} are idempotent; adding the same
	 * component class more than once has no additional effect.
	 * @param componentClasses one or more component classes,
	 * e.g. {@link Configuration @Configuration} classes
	 */
	// 手动注册一个或多个组件类（Component Classes）到容器中。
	void register(Class<?>... componentClasses);

	/**
	 * Perform a scan within the specified base packages.
	 * @param basePackages the packages to scan for component classes
	 */
	// 在指定的包路径下执行类扫描。
	// basePackages 是一个或多个包名的字符串（例如 "com.example.services"）。
	// 自动探测：Spring 会递归扫描这些包及其子包，寻找所有带有 @Component、@Service、@Controller、@Repository 或其他自定义复合注解的类。
	// 注册 BeanDefinition：扫描到的类会自动转换为 BeanDefinition 并注册到 Bean 容器中。
	// 当你希望 Spring 自动发现项目中的组件时使用。例如：context.scan("com.mycompany.project");。
	void scan(String... basePackages);

}
