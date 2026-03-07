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

package org.springframework.util;

import org.springframework.lang.Nullable;

/**
 * Simple strategy interface for resolving a String value.
 * Used by {@link org.springframework.beans.factory.config.ConfigurableBeanFactory}.
 *
 * @author Juergen Hoeller
 * @since 2.5
 * @see org.springframework.beans.factory.config.ConfigurableBeanFactory#resolveAliases
 * @see org.springframework.beans.factory.config.BeanDefinitionVisitor#BeanDefinitionVisitor(StringValueResolver)
 * @see org.springframework.beans.factory.config.PropertyPlaceholderConfigurer
 */
// StringValueResolver 是 Spring 框架中一个简单但功能极其强大的策略接口。它主要用于处理字符串的“解析”工作，最常见的应用场景就是处理占位符（Placeholders）和别名（Aliases）。
// 该接口的核心作用是将一个原始字符串转换为另一个处理后的字符串。
// 占位符解析：这是它最著名的用途。例如，将 ${db.url} 解析为配置文件中实际的 jdbc:mysql://...。
// 别名转换：在 BeanFactory 中，用于解析 Bean 的别名。
// 注解属性解析：解析注解（如 @Value 或 @Scheduled）中硬编码的字符串值。
@FunctionalInterface
public interface StringValueResolver {

	/**
	 * Resolve the given String value, for example parsing placeholders.
	 * @param strVal the original String value (never {@code null})
	 * @return the resolved String value (may be {@code null} when resolved to a null
	 * value), possibly the original String value itself (in case of no placeholders
	 * to resolve or when ignoring unresolvable placeholders)
	 * @throws IllegalArgumentException in case of an unresolvable String value
	 */
	// 对传入的字符串 strVal 执行解析逻辑。
	// strVal：输入的原始字符串。根据文档注释，调用者保证此值绝不为 null。
	@Nullable
	String resolveStringValue(String strVal);

}
