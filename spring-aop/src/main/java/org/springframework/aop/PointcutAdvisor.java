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
 * Superinterface for all Advisors that are driven by a pointcut.
 * This covers nearly all advisors except introduction advisors,
 * for which method-level matching doesn't apply.
 *
 * @author Rod Johnson
 */
// Gemini said
//在 Spring AOP 的体系结构中，PointcutAdvisor 是最常用、最核心的接口。如果说 Advisor 是一个通用的切面抽象，那么 PointcutAdvisor 就是绝大多数业务切面（如事务、日志、缓存）的真正实现标准。
// PointcutAdvisor 的核心作用是将 “做什么”（Advice） 与 “在哪里做”（Pointcut） 完美结合在一起。
// 驱动机制：它是“由切点驱动”的。这意味着它不仅包含增强逻辑，还包含一套复杂的过滤规则，决定了这些逻辑应该应用到哪些类、哪些方法上。
// 方法级匹配：与 IntroductionAdvisor（引介切面）不同，PointcutAdvisor 支持细粒度到方法名的匹配。这是实现 AOP 拦截器链的基础。
// PointcutAdvisor = Pointcut (在哪里匹配) + Advice (执行什么逻辑)
//
public interface PointcutAdvisor extends Advisor {

	/**
	 * Get the Pointcut that drives this advisor.
	 */
	// 作用：获取驱动该 Advisor 的 切点（Pointcut）。
	// 职责：Pointcut 内部包含了 ClassFilter（类过滤器）和 MethodMatcher（方法匹配器）。
	// 运行逻辑：Spring 容器在扫描 Bean 并决定是否创建代理时，会调用此方法获取切点，然后询问切点：“这个 Bean 的这个方法需要被增强吗？”。如果切点回答 true，则应用 getAdvice() 返回的逻辑。
	Pointcut getPointcut();

}
