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

package org.aopalliance.intercept;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Intercepts calls on an interface on its way to the target. These
 * are nested "on top" of the target.
 *
 * <p>The user should implement the {@link #invoke(MethodInvocation)}
 * method to modify the original behavior. E.g. the following class
 * implements a tracing interceptor (traces all the calls on the
 * intercepted method(s)):
 *
 * <pre class=code>
 * class TracingInterceptor implements MethodInterceptor {
 *   Object invoke(MethodInvocation i) throws Throwable {
 *     System.out.println("method "+i.getMethod()+" is called on "+
 *                        i.getThis()+" with args "+i.getArguments());
 *     Object ret=i.proceed();
 *     System.out.println("method "+i.getMethod()+" returns "+ret);
 *     return ret;
 *   }
 * }
 * </pre>
 *
 * @author Rod Johnson
 */
// MethodInterceptor 的核心作用是实现环绕通知（Around Advice）。
// 全周期控制：与“前置通知”或“后置通知”不同，拦截器持有对目标方法调用的完全控制权。它可以在目标方法执行前、执行后、甚至在抛出异常时执行自定义逻辑。
// 链式调用（Chain of Responsibility）：在 Spring AOP 中，一个目标方法通常被多个拦截器包装。MethodInterceptor 通过调用 invocation.proceed()，将执行权传递给链中的下一个拦截器，直到最后到达目标方法。
// 透明性：对于调用者来说，调用的依然是原接口的方法，但实际上执行的是拦截器提供的增强逻辑。这正是声明式事务（Transaction）、权限校验（Security）和日志监控（Logging）的底层实现基础。
@FunctionalInterface
public interface MethodInterceptor extends Interceptor {

	/**
	 * Implement this method to perform extra treatments before and
	 * after the invocation. Polite implementations would certainly
	 * like to invoke {@link Joinpoint#proceed()}.
	 * @param invocation the method invocation joinpoint
	 * @return the result of the call to {@link Joinpoint#proceed()};
	 * might be intercepted by the interceptor
	 * @throws Throwable if the interceptors or the target object
	 * throws an exception
	 */
	@Nullable
	Object invoke(@Nonnull MethodInvocation invocation) throws Throwable;

}
