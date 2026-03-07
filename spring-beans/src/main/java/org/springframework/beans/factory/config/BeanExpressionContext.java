/*
 * Copyright 2002-2023 the original author or authors.
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

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Context object for evaluating an expression within a bean definition.
 *
 * @author Juergen Hoeller
 * @since 3.0
 */
// BeanExpressionContext 是 Spring 表达式语言（SpEL）在 Bean 工厂环境下的“上下文容器”。
// 当 Spring 需要解析 Bean 定义中的表达式（例如 @Value("#{beanName.property}")）时，它会使用这个类来桥接表达式引擎与 Spring 容器。
// 在 SpEL 表达式求值时，表达式引擎需要知道去哪里寻找变量或对象。BeanExpressionContext 的核心作用是：
// 资源整合：它将 ConfigurableBeanFactory（Bean 工厂）和 Scope（作用域，如 request, session）封装在一起。
// 变量路由：当表达式中引用了一个对象名时，该上下文负责决定是从 Bean 工厂中获取一个标准的 Bean，还是从当前作用域中获取一个上下文对象（如 HTTP 请求对象）。
// 屏蔽细节：它为 BeanExpressionResolver（表达式解析器）提供了一个统一的访问接口，使其不需要关心 Bean 是存储在单例池中还是特定的作用域中。
public class BeanExpressionContext {
	// 引用当前的 Bean 工厂。
	// 重要性：它是获取 Spring 容器中绝大多数 Bean 的入口。
	private final ConfigurableBeanFactory beanFactory;
	// 作用：引用当前执行环境的作用域。
	// 重要性：如果表达式在特定的 Web 作用域（如 Request 作用域）下执行，该属性允许表达式访问该作用域特有的对象（如 request, session）。
	@Nullable
	private final Scope scope;


	public BeanExpressionContext(ConfigurableBeanFactory beanFactory, @Nullable Scope scope) {
		Assert.notNull(beanFactory, "BeanFactory must not be null");
		this.beanFactory = beanFactory;
		this.scope = scope;
	}

	public final ConfigurableBeanFactory getBeanFactory() {
		return this.beanFactory;
	}

	@Nullable
	public final Scope getScope() {
		return this.scope;
	}

	// 作用：检查是否存在指定的对象或变量。
	public boolean containsObject(String key) {
		return (this.beanFactory.containsBean(key) ||
				(this.scope != null && this.scope.resolveContextualObject(key) != null));
	}
	// 作用：根据名称获取具体的对象实例。
	@Nullable
	public Object getObject(String key) {
		if (this.beanFactory.containsBean(key)) {
			return this.beanFactory.getBean(key);
		}
		else if (this.scope != null) {
			return this.scope.resolveContextualObject(key);
		}
		else {
			return null;
		}
	}


	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof BeanExpressionContext that &&
				this.beanFactory == that.beanFactory && this.scope == that.scope));
	}

	@Override
	public int hashCode() {
		return this.beanFactory.hashCode();
	}

}
