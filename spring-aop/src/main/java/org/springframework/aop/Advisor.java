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

package org.springframework.aop;

import org.aopalliance.aop.Advice;

/**
 * Base interface holding AOP <b>advice</b> (action to take at a joinpoint)
 * and a filter determining the applicability of the advice (such as
 * a pointcut). <i>This interface is not for use by Spring users, but to
 * allow for commonality in support for different types of advice.</i>
 *
 * <p>Spring AOP is based around <b>around advice</b> delivered via method
 * <b>interception</b>, compliant with the AOP Alliance interception API.
 * The Advisor interface allows support for different types of advice,
 * such as <b>before</b> and <b>after</b> advice, which need not be
 * implemented using interception.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
// 在 Spring AOP 的体系结构中，Advisor 是一个非常高层且核心的接口。如果说 Advice（通知）解决了“做什么”的问题，那么 Advisor 解决的就是“谁在什么时候做什么”的完整封装。
// Advisor 通常被称为**“切面”的底层持有者**。它的作用主要体现在以下几个方面：
// Advice 的容器：它持有一个 Advice 对象。Advice 是具体的拦截逻辑（如日志、事务），而 Advisor 负责管理这个逻辑。
// 解耦通知与应用范围：Advice 本身通常不包含“在哪里执行”的信息。Advisor 的子接口（如 PointcutAdvisor）会将 Advice 与 Pointcut（切点）结合。这样，同一个 Advice 逻辑可以被不同的 Advisor 包装，应用到不同的类或方法上。
// 统一的抽象接口：Spring 并不直接操作各种零散的 BeforeAdvice 或 AfterAdvice，而是统一将它们包装成 Advisor。在创建代理对象时，Spring 只需遍历 Advisor 链即可。
// 内部基础设施：正如代码注释所言，这个接口主要供 Spring 框架内部使用，用于支持不同类型的通知（包括 AOP 联盟标准的拦截器和 Spring 自定义的通知类型）。
public interface Advisor {

	/**
	 * Common placeholder for an empty {@code Advice} to be returned from
	 * {@link #getAdvice()} if no proper advice has been configured (yet).
	 * @since 5.0
	 */
	// 这是一个空通知占位符。
	Advice EMPTY_ADVICE = new Advice() {};


	/**
	 * Return the advice part of this aspect. An advice may be an
	 * interceptor, a before advice, a throws advice, etc.
	 * @return the advice that should apply if the pointcut matches
	 * @see org.aopalliance.intercept.MethodInterceptor
	 * @see BeforeAdvice
	 * @see ThrowsAdvice
	 * @see AfterReturningAdvice
	 */
	// 作用：获取该切面所持有的 通知（Advice） 部分。
	Advice getAdvice();

	/**
	 * Return whether this advice is associated with a particular instance
	 * (for example, creating a mixin) or shared with all instances of
	 * the advised class obtained from the same Spring bean factory.
	 * <p><b>Note that this method is not currently used by the framework.</b>
	 * Typical Advisor implementations always return {@code true}.
	 * Use singleton/prototype bean definitions or appropriate programmatic
	 * proxy creation to ensure that Advisors have the correct lifecycle model.
	 * <p>As of 6.0.10, the default implementation returns {@code true}.
	 * @return whether this advice is associated with a particular target instance
	 */
	// 作用：判断该通知是与特定的目标对象实例关联，还是被所有实例共享。
	// 如果返回 true（共享模式）：该 Advisor 是单例的，所有被代理的目标对象共享同一个通知实例。
	// 如果返回 false（多例/Mixin 模式）：每个被代理的目标对象实例都会拥有该通知的一个独立副本。
	default boolean isPerInstance() {
		return true;
	}

}
