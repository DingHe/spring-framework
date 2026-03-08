/*
 * Copyright 2002-2016 the original author or authors.
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

import java.lang.reflect.Method;

import javax.annotation.Nonnull;

/**
 * Description of an invocation to a method, given to an interceptor
 * upon method-call.
 *
 * <p>A method invocation is a joinpoint and can be intercepted by a
 * method interceptor.
 *
 * @author Rod Johnson
 * @see MethodInterceptor
 */
// MethodInvocation 接口是 AOP Alliance 标准中最为常用的接口之一。在 Spring AOP 的世界里，几乎所有的拦截逻辑（Around Advice）都是围绕这个接口展开的。它继承自 Invocation，专门用于描述对方法的调用。
// 具体化的连接点：将通用的 Joinpoint 细化为“方法执行”这一具体点。
// 语义友好化：相比于父接口返回通用的 AccessibleObject 或 Object，它直接返回 Java 反射中的 Method 类型，消除了强制类型转换的麻烦。
// 拦截器核心入参：它是 MethodInterceptor#invoke(MethodInvocation) 方法的唯一参数，承载了方法调用的全部上下文（目标对象、方法、参数、执行流）。
// 作用：获取正在被调用的目标方法对象。
// 作用：获取调用该方法时传入的参数数组。
public interface MethodInvocation extends Invocation {

	/**
	 * Get the method being called.
	 * <p>This method is a friendly implementation of the
	 * {@link Joinpoint#getStaticPart()} method (same result).
	 * @return the method being called
	 */
	@Nonnull
	Method getMethod();

}
