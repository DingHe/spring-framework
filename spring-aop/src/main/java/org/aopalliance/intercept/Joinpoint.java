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

package org.aopalliance.intercept;

import java.lang.reflect.AccessibleObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * This interface represents a generic runtime joinpoint (in the AOP
 * terminology).
 *
 * <p>A runtime joinpoint is an <i>event</i> that occurs on a static
 * joinpoint (i.e. a location in a program). For instance, an
 * invocation is the runtime joinpoint on a method (static joinpoint).
 * The static part of a given joinpoint can be generically retrieved
 * using the {@link #getStaticPart()} method.
 *
 * <p>In the context of an interception framework, a runtime joinpoint
 * is then the reification of an access to an accessible object (a
 * method, a constructor, a field), i.e. the static part of the
 * joinpoint. It is passed to the interceptors that are installed on
 * the static joinpoint.
 *
 * @author Rod Johnson
 * @see Interceptor
 */
// 在 AOP（面向切面编程）术语中，Joinpoint（连接点） 代表程序执行过程中的一个特定点。
// 运行时的“事件”：它是一个抽象的概念，代表了“调用一个方法”或“构造一个对象”这个动作本身。
// 拦截器的纽带：在 Spring AOP 中，当一个方法被拦截时，Spring 会创建一个 Joinpoint 实例（通常是 MethodInvocation）并传递给拦截器链。
// 控制执行流：它不仅包含目标方法的信息，还负责控制是否继续执行后续的拦截器或目标方法。
public interface Joinpoint {

	/**
	 * Proceed to the next interceptor in the chain.
	 * <p>The implementation and the semantics of this method depends
	 * on the actual joinpoint type (see the children interfaces).
	 * @return see the children interfaces' proceed definition
	 * @throws Throwable if the joinpoint throws an exception
	 */
	// 作用：推进执行链。这是 AOP 逻辑中最核心的方法。
	// 返回类型：Object（返回目标方法执行后的结果，如果方法返回 void 则为 null）。
	// 在一个拦截器链中，调用 proceed() 会导致执行流跳转到下一个拦截器。
	// 如果当前拦截器已经是链中的最后一个，调用 proceed() 将触发对目标对象真实方法的调用。
	@Nullable
	Object proceed() throws Throwable;

	/**
	 * Return the object that holds the current joinpoint's static part.
	 * <p>For instance, the target object for an invocation.
	 * @return the object (can be null if the accessible object is static)
	 */
	// 作用：获取当前持有连接点的对象（目标对象）。
	// 它返回的是被代理的那个真实目标对象实例。
	// 如果拦截的是一个静态方法（static method），该方法将返回 null。
	// 在拦截器中，如果你需要访问目标对象的其他属性或方法，可以通过此方法获取引用。
	@Nullable
	Object getThis();

	/**
	 * Return the static part of this joinpoint.
	 * <p>The static part is an accessible object on which a chain of
	 * interceptors is installed.
	 */
	// 作用：获取连接点的静态部分。
	// “静态部分”是指在编写代码时就已经确定的元数据。
	// 对于方法拦截，它通常返回一个 java.lang.reflect.Method 对象。
	// 对于构造器拦截，它返回 java.lang.reflect.Constructor 对象。
	@Nonnull
	AccessibleObject getStaticPart();

}
