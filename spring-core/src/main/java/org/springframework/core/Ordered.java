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

package org.springframework.core;

/**
 * {@code Ordered} is an interface that can be implemented by objects that
 * should be <em>orderable</em>, for example in a {@code Collection}.
 *
 * <p>The actual {@link #getOrder() order} can be interpreted as prioritization,
 * with the first object (with the lowest order value) having the highest
 * priority.
 *
 * <p>Note that there is also a <em>priority</em> marker for this interface:
 * {@link PriorityOrdered}. Consult the Javadoc for {@code PriorityOrdered} for
 * details on how {@code PriorityOrdered} objects are ordered relative to
 * <em>plain</em> {@link Ordered} objects.
 *
 * <p>Consult the Javadoc for {@link OrderComparator} for details on the
 * sort semantics for non-ordered objects.
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 07.04.2003
 * @see PriorityOrdered
 * @see OrderComparator
 * @see org.springframework.core.annotation.Order
 * @see org.springframework.core.annotation.AnnotationAwareOrderComparator
 */
// Ordered 接口的核心作用是为一组同类型的 Bean 提供一个排序权值。
// 执行顺序控制：在 Spring 中，很多组件是以列表（List）或数组形式存在的，比如 BeanPostProcessor（后置处理器链）、Interceptor（拦截器链）或 Advice（通知）。当有多个组件同时起作用时，必须有一个标准来决定谁先运行，谁后运行。
// 优先级定义：权值越小，优先级越高。这类似于现实生活中的“一等奖”比“二等奖”排在前面。

public interface Ordered {

	/**
	 * Useful constant for the highest precedence value.
	 * @see java.lang.Integer#MIN_VALUE
	 */
	// 作用：代表最高优先级。
	int HIGHEST_PRECEDENCE = Integer.MIN_VALUE;

	/**
	 * Useful constant for the lowest precedence value.
	 * @see java.lang.Integer#MAX_VALUE
	 */
	// 作用：代表最低优先级。
	int LOWEST_PRECEDENCE = Integer.MAX_VALUE;


	/**
	 * Get the order value of this object.
	 * <p>Higher values are interpreted as lower priority. As a consequence,
	 * the object with the lowest value has the highest priority (somewhat
	 * analogous to Servlet {@code load-on-startup} values).
	 * <p>Same order values will result in arbitrary sort positions for the
	 * affected objects.
	 * @return the order value
	 * @see #HIGHEST_PRECEDENCE
	 * @see #LOWEST_PRECEDENCE
	 */
	// 获取当前对象的排序值（Order Value）。
	// 数值越小，优先级越高（Highest Priority）。
	// 数值越大，优先级越低（Lowest Priority）。
	// 相等时：如果两个对象的 getOrder() 返回值相同，则它们的相对顺序是不确定的（通常取决于它们被注册到容器中的自然顺序）。
	int getOrder();

}
