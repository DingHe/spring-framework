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

import org.springframework.beans.BeansException;
import org.springframework.lang.Nullable;

/**
 * Strategy interface for resolving a value by evaluating it as an expression,
 * if applicable.
 *
 * <p>A raw {@link org.springframework.beans.factory.BeanFactory} does not
 * contain a default implementation of this strategy. However,
 * {@link org.springframework.context.ApplicationContext} implementations
 * will provide expression support out of the box.
 *
 * @author Juergen Hoeller
 * @since 3.0
 */
// BeanExpressionResolver 是 Spring 框架中专门用于处理表达式解析的策略接口。它是 Spring 表达式语言（SpEL）与 Bean 工厂（BeanFactory）之间的核心纽带。
// 在 Spring 配置中，我们经常会看到类似 #{...} 的语法。该接口的主要职责就是将这些字符串形式的表达式转化为具体的对象或数值。
// 动态求值：允许在运行时根据容器的状态（如其他 Bean 的属性、系统环境变量等）动态计算出某个 Bean 属性的值。
// 解耦表达式引擎：它是一个策略接口，这意味着 Spring 并不强制绑定到特定的表达式引擎。虽然默认实现使用的是 SpEL (Spring Expression Language)，但理论上你可以通过实现此接口来支持其他表达式语言。
// 按需集成：底层的 BeanFactory 默认不包含此功能的实现，只有在 ApplicationContext（如 AnnotationConfigApplicationContext）启动过程中，才会将具体的解析器（通常是 StandardBeanExpressionResolver）注入到工厂中。
public interface BeanExpressionResolver {

	/**
	 * Evaluate the given value as an expression, if applicable;
	 * return the value as-is otherwise.
	 * @param value the value to evaluate as an expression
	 * @param beanExpressionContext the bean expression context to use when
	 * evaluating the expression
	 * @return the resolved value (potentially the given value as-is)
	 * @throws BeansException if evaluation failed
	 */
	// 这是解析器的核心入口。它尝试将传入的字符串 value 作为一个表达式进行计算；如果该字符串不符合表达式的格式（例如不以 #{ 开头），则原样返回。
	@Nullable
	Object evaluate(@Nullable String value, BeanExpressionContext beanExpressionContext) throws BeansException;

}
