/*
 * Copyright 2002-2019 the original author or authors.
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

import org.aopalliance.aop.Advice;

/**
 * This interface represents a generic interceptor.
 *
 * <p>A generic interceptor can intercept runtime events that occur
 * within a base program. Those events are materialized by (reified
 * in) joinpoints. Runtime joinpoints can be invocations, field
 * access, exceptions...
 *
 * <p>This interface is not used directly. Use the sub-interfaces
 * to intercept specific events. For instance, the following class
 * implements some specific interceptors in order to implement a
 * debugger:
 *
 * <pre class=code>
 * class DebuggingInterceptor implements MethodInterceptor,
 *     ConstructorInterceptor {
 *
 *   Object invoke(MethodInvocation i) throws Throwable {
 *     debug(i.getMethod(), i.getThis(), i.getArgs());
 *     return i.proceed();
 *   }
 *
 *   Object construct(ConstructorInvocation i) throws Throwable {
 *     debug(i.getConstructor(), i.getThis(), i.getArgs());
 *     return i.proceed();
 *   }
 *
 *   void debug(AccessibleObject ao, Object this, Object value) {
 *     ...
 *   }
 * }
 * </pre>
 *
 * @author Rod Johnson
 * @see Joinpoint
 */
// Interceptor 的核心作用可以概括为以下几点：
// 通用的拦截抽象：它是所有拦截器的顶层基接口。在 AOP 术语中，拦截器是一种特殊的 Advice（增强），它能够**捕获（拦截）**程序运行时的特定事件。
// 连接点（Joinpoint）的处理器：当程序运行到某个特定的位置（如方法调用、构造函数执行、字段访问等）时，这些位置被物化为 Joinpoint 对象。Interceptor 的任务就是处理这些 Joinpoint。
// 分层的职责链基础：虽然 Interceptor 接口本身依然是一个标记接口（不定义方法），但它定义了一个关键的层级：所有的拦截逻辑都必须支持对事件的“拦截”操作，这为后续形成拦截器链（Interceptor Chain）奠定了基础。
// 解耦具体事件：它并不限制拦截什么。具体的拦截行为由其子接口定义。例如，它不关心是拦截方法还是拦截属性访问，它只是在语义上把“增强（Advice）”具体化为了“拦截（Interceptor）”。


public interface Interceptor extends Advice {

}
