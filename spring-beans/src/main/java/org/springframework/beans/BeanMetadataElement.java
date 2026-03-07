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

package org.springframework.beans;

import org.springframework.lang.Nullable;

/**
 * Interface to be implemented by bean metadata elements
 * that carry a configuration source object.
 *
 * @author Juergen Hoeller
 * @since 2.0
 */
// 在 Spring 容器启动过程中，一个 Bean 的定义（BeanDefinition）可能来源于多种渠道：XML 配置文件、Java 配置类（@Bean）、注解扫描（@Component）或者是 Groovy 脚本。
// BeanMetadataElement 的核心作用是：提供一个统一的接口，用于获取定义该元数据的“源对象”（Source Object）。
// 调试与诊断：当 Bean 初始化出错时，Spring 可以通过这个接口告诉开发者，这个 Bean 是在哪个 XML 文件的第几行定义的，或者是哪个配置类触发的。
// 工具支持：IDE（如 IntelliJ IDEA 或 Eclipse）可以利用这个接口实现“跳转到定义”的功能。
// 层次结构：它是 Spring Bean 定义体系的基础接口之一。许多核心接口（如 BeanDefinition）都继承了它。
public interface BeanMetadataElement {

	/**
	 * Return the configuration source {@code Object} for this metadata element
	 * (may be {@code null}).
	 */
	// 如果是从 XML 加载的：返回通常是一个 org.springframework.core.io.Resource 对象（指向具体的 .xml 文件），有时甚至会包含具体的行号信息（通过 org.springframework.beans.factory.parsing.Location）。
	// 如果是通过注解扫描的：返回通常是该类的 Class 对象。
	// 如果是手动注册的：如果没有显式指定源，通常返回 null。
	@Nullable
	default Object getSource() {
		return null;
	}

}
