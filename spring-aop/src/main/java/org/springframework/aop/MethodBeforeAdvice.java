/*
 * Copyright 2002-2018 the original author or authors.
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

import java.lang.reflect.Method;

import org.springframework.lang.Nullable;

/**
 * Advice invoked before a method is invoked. Such advices cannot
 * prevent the method call proceeding, unless they throw a Throwable.
 *
 * @author Rod Johnson
 * @see AfterReturningAdvice
 * @see ThrowsAdvice
 */
// MethodBeforeAdvice 是 Spring AOP 中最常用的通知类型之一，它代表了强类型的前置通知。相比于功能最全的环绕通知（Around Advice），它更简单、更安全。
// 前置切入：专门用于在目标方法执行之前插入逻辑。
// 非侵入性增强：它主要用于执行一些辅助操作，如日志记录、权限检查、参数校验或资源预加载。
public interface MethodBeforeAdvice extends BeforeAdvice {

	/**
	 * Callback before a given method is invoked.
	 * @param method the method being invoked
	 * @param args the arguments to the method
	 * @param target the target of the method invocation. May be {@code null}.
	 * @throws Throwable if this object wishes to abort the call.
	 * Any exception thrown will be returned to the caller if it's
	 * allowed by the method signature. Otherwise the exception
	 * will be wrapped as a runtime exception.
	 */
	// Method method 作用：当前正在被调用的目标方法对象。
	void before(Method method, Object[] args, @Nullable Object target) throws Throwable;

}
