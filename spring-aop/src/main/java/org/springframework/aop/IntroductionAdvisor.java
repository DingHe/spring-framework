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
 * Superinterface for advisors that perform one or more AOP <b>introductions</b>.
 *
 * <p>This interface cannot be implemented directly; subinterfaces must
 * provide the advice type implementing the introduction.
 *
 * <p>Introduction is the implementation of additional interfaces
 * (not implemented by a target) via AOP advice.
 *
 * @author Rod Johnson
 * @since 04.04.2003
 * @see IntroductionInterceptor
 */
// 在 Spring AOP 的体系中，IntroductionAdvisor 是一个非常特殊的接口。如果说普通的 Advisor 是在现有方法上“加料”（增强），那么 IntroductionAdvisor 就是在给类“换骨”——它允许你为现有的类动态地添加新的接口实现。
// 动态扩展能力：通过引介，你可以让一个原本没有实现某个接口的类，在运行期间“变”成实现了该接口。例如，你可以让所有的 Service 类动态实现一个 Auditable 接口，而无需修改源码。
// 类级别的增强：普通的 AOP（如 MethodInterceptor）关注的是方法执行前后的拦截，而引介关注的是类结构的改变。
// ClassFilter 驱动：由于引介是针对整个类的，所以它不需要方法级别的匹配（Pointcut 中的 MethodMatcher），只需要 ClassFilter 来决定哪些类需要被“注入”新接口。
public interface IntroductionAdvisor extends Advisor, IntroductionInfo {

	/**
	 * Return the filter determining which target classes this introduction
	 * should apply to.
	 * <p>This represents the class part of a pointcut. Note that method
	 * matching doesn't make sense to introductions.
	 * @return the class filter
	 */
	// 作用：返回一个类过滤器，用于确定哪些目标类应该应用这个引介。
	ClassFilter getClassFilter();

	/**
	 * Can the advised interfaces be implemented by the introduction advice?
	 * Invoked before adding an IntroductionAdvisor.
	 * @throws IllegalArgumentException if the advised interfaces can't be
	 * implemented by the introduction advice
	 */
	void validateInterfaces() throws IllegalArgumentException;

}
