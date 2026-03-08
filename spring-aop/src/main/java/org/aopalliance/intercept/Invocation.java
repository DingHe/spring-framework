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

import javax.annotation.Nonnull;

/**
 * This interface represents an invocation in the program.
 *
 * <p>An invocation is a joinpoint and can be intercepted by an
 * interceptor.
 *
 * @author Rod Johnson
 */
// 如果说 Joinpoint 是一个通用的“连接点”，那么 Invocation 则进一步具体化为一次“调用”（如方法调用或构造器调用）。
// 数据载体：它不仅代表一个执行点，还包含了执行该点所需的具体数据（即参数）。
// 参数控制权：通过该接口，拦截器不仅能读取参数，还能修改参数。因为 getArguments() 返回的是数组引用，修改数组内的元素会直接影响后续拦截器和目标方法的接收值。
// 层次化定义：它是 MethodInvocation（方法调用）和 ConstructorInvocation（构造器调用）的共同基类，提取了“调用”行为的共有特征。
public interface Invocation extends Joinpoint {

	/**
	 * Get the arguments as an array object.
	 * It is possible to change element values within this
	 * array to change the arguments.
	 * @return the argument of the invocation
	 */
	// 作用：获取调用目标时传入的参数数组。
	@Nonnull
	Object[] getArguments();

}
