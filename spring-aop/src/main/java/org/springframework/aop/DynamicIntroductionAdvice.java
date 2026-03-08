/*
 * Copyright 2002-2021 the original author or authors.
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
 * Subinterface of AOP Alliance Advice that allows additional interfaces
 * to be implemented by an Advice, and available via a proxy using that
 * interceptor. This is a fundamental AOP concept called <b>introduction</b>.
 *
 * <p>Introductions are often <b>mixins</b>, enabling the building of composite
 * objects that can achieve many of the goals of multiple inheritance in Java.
 *
 * <p>Compared to {@link IntroductionInfo}, this interface allows an advice to
 * implement a range of interfaces that is not necessarily known in advance.
 * Thus an {@link IntroductionAdvisor} can be used to specify which interfaces
 * will be exposed in an advised object.
 *
 * @author Rod Johnson
 * @since 1.1.1
 * @see IntroductionInfo
 * @see IntroductionAdvisor
 */
// 在标准的 AOP 中，我们通常是拦截现有方法（如在方法前后加日志）。而 引介（Introduction） 允许我们为现有的对象动态地添加新的接口实现。
// 实现“混入”（Mixin）模式：它可以让一个现有的 Java 对象在运行期间看起来像是实现了某些原本没有定义的接口。这在一定程度上实现了类似于“多重继承”的效果。
// 动态性：与静态的 IntroductionInfo 接口不同，DynamicIntroductionAdvice 并不要求 Advice 提前声明它支持哪些接口。它允许在运行时通过逻辑判断来决定是否支持某个特定接口。
// 组合对象：它常用于构建复合对象，使对象能够根据配置动态地具备不同的行为能力（例如：让任何 Service 自动实现 IsModified 接口来追踪状态）。
public interface DynamicIntroductionAdvice extends Advice {

	/**
	 * Does this introduction advice implement the given interface?
	 * @param intf the interface to check
	 * @return whether the advice implements the specified interface
	 */
	// 参数：Class<?> intf —— 需要检查的接口类型。
	// 返回值：boolean —— 如果该 Advice 能够处理或实现了指定的接口，则返回 true；否则返回 false。
	// 接口匹配：当 AOP 代理（Proxy）在接收到属于新添加接口的方法调用时，它会询问这个 Advice：“你是否实现了这个接口？”
	// 动态决策：这是“动态”二字的体现。实现类可以根据运行时的状态、配置或者传入的 intf 类型，灵活决定是否要暴露这个接口。
	// 分发逻辑：如果此方法返回 true，Spring AOP 框架就会将对该接口的方法调用委派（Delegate）给当前的这个 Advice 实例来执行。
	boolean implementsInterface(Class<?> intf);

}
