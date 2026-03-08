/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.aop;

/**
 * Core Spring pointcut abstraction.
 *
 * <p>A pointcut is composed of a {@link ClassFilter} and a {@link MethodMatcher}.
 * Both these basic terms and a Pointcut itself can be combined to build up combinations
 * (e.g. through {@link org.springframework.aop.support.ComposablePointcut}).
 *
 * @author Rod Johnson
 * @see ClassFilter
 * @see MethodMatcher
 * @see org.springframework.aop.support.Pointcuts
 * @see org.springframework.aop.support.ClassFilters
 * @see org.springframework.aop.support.MethodMatchers
 */
// Pointcut（切点）是 Spring AOP 中最核心的抽象接口。如果说 Advice 解决了“做什么”的问题，那么 Pointcut 专门解决**“在哪里做”**的问题。
// Pointcut 的本质是一个过滤器集合。
// 多维度过滤：它将“在哪里增强”拆分为两个维度：类（Class）和方法（Method）。
// 性能优化：Spring 在创建代理时，会先通过 ClassFilter 快速筛选类。如果类不匹配，就直接跳过，不再进行昂贵的方法级别检查。
// 可组合性：正如注释中提到的，Pointcut 可以通过 ComposablePointcut 进行逻辑组合（与、或、非），从而构建极其复杂的匹配规则。
// 解耦：它使得拦截逻辑（Advice）与业务代码完全解耦。业务代码不需要知道自己被谁拦截，拦截器也不需要硬编码要拦截谁。
public interface Pointcut {

	/**
	 * Return the ClassFilter for this pointcut.
	 * @return the ClassFilter (never {@code null})
	 */
	// 作用：返回该切面的类过滤器。
	ClassFilter getClassFilter();

	/**
	 * Return the MethodMatcher for this pointcut.
	 * @return the MethodMatcher (never {@code null})
	 */
	// 作用：返回该切面的方法匹配器。
	MethodMatcher getMethodMatcher();


	/**
	 * Canonical Pointcut instance that always matches.
	 */
	// 作用：全匹配切点。
	Pointcut TRUE = TruePointcut.INSTANCE;

}
