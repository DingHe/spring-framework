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

package org.aopalliance.aop;

/**
 * Tag interface for Advice. Implementations can be any type
 * of advice, such as Interceptors.
 *
 * @author Rod Johnson
 * @version $Id: Advice.java,v 1.1 2004/03/19 17:02:16 johnsonr Exp $
 */
// Advice（通知/增强）接口的主要作用如下：
// 标识接口（Tag Interface）：它是一个空的接口，没有任何方法定义。它的存在仅仅是为了作为一个类型标识，告诉 Spring 容器：“这是一个 AOP 增强逻辑”。
// 统一抽象：它是所有 AOP 增强逻辑的顶层父接口。无论是“环绕通知”、“前置通知”还是“异常通知”，最终都属于 Advice 类型。
// 解耦规范：Spring AOP 并没有完全自创一套标准，而是实现了 AOP 联盟的规范。通过这个接口，Spring 能够兼容其他符合该规范的 AOP 实现。
// 在 AOP 的核心概念中，Advice 定义了**“在连接点（Joinpoint）执行什么动作”**。
// MethodInterceptor 最强大的增强，拦截方法调用，支持在方法前后执行逻辑（环绕增强）。  @Around
// BeforeAdvice 在目标方法执行前执行。Spring 具体常用 MethodBeforeAdvice。   @Before
// AfterAdvice 在目标方法执行后（无论成功还是异常）执行。   @After
// ThrowsAdvice  在目标方法抛出异常时执行。  @AfterThrowing
// AfterReturningAdvice  在目标方法成功返回结果后执行。  @AfterReturning
public interface Advice {

}
