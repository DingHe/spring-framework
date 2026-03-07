/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.web.context.request;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.Scope;
import org.springframework.lang.Nullable;

/**
 * Abstract {@link Scope} implementation that reads from a particular scope
 * in the current thread-bound {@link RequestAttributes} object.
 *
 * <p>Subclasses simply need to implement {@link #getScope()} to instruct
 * this class which {@link RequestAttributes} scope to read attributes from.
 *
 * <p>Subclasses may wish to override the {@link #get} and {@link #remove}
 * methods to add synchronization around the call back into this superclass.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @since 2.0
 */
// AbstractRequestAttributesScope 是 Spring Web 模块中的一个抽象类，它是 Scope 接口的基础实现，专门用于处理与当前 HTTP 请求线程绑定的属性。
// 这个类的核心作用是桥接 Spring Bean 工厂的作用域机制与 Web 请求属性（RequestAttributes）。
//
public abstract class AbstractRequestAttributesScope implements Scope {
	// 作用：从 Web 作用域中获取 Bean 实例，不存在则创建。
	@Override
	public Object get(String name, ObjectFactory<?> objectFactory) {
		// 通过 RequestContextHolder 获取当前线程绑定的 RequestAttributes。
		RequestAttributes attributes = RequestContextHolder.currentRequestAttributes();
		// 尝试从对应的 Web 作用域（由 getScope() 决定）获取属性。
		Object scopedObject = attributes.getAttribute(name, getScope());
		if (scopedObject == null) {
			// 如果不存在：
			// 调用 objectFactory.getObject() 创建新实例。
			// 用 setAttribute 将新实例存入 Web 环境。
			scopedObject = objectFactory.getObject();
			attributes.setAttribute(name, scopedObject, getScope());
			// Retrieve object again, registering it for implicit session attribute updates.
			// As a bonus, we also allow for potential decoration at the getAttribute level.
			Object retrievedObject = attributes.getAttribute(name, getScope());
			if (retrievedObject != null) {
				// Only proceed with retrieved object if still present (the expected case).
				// If it disappeared concurrently, we return our locally created instance.
				scopedObject = retrievedObject;
			}
		}
		return scopedObject;
	}
	// 作用：从 Web 作用域中移除指定的 Bean。
	@Override
	@Nullable
	public Object remove(String name) {
		RequestAttributes attributes = RequestContextHolder.currentRequestAttributes();
		Object scopedObject = attributes.getAttribute(name, getScope());
		if (scopedObject != null) {
			attributes.removeAttribute(name, getScope());
			return scopedObject;
		}
		else {
			return null;
		}
	}
	// 作用：注册 Bean 销毁时的回调逻辑。
	@Override
	public void registerDestructionCallback(String name, Runnable callback) {
		RequestAttributes attributes = RequestContextHolder.currentRequestAttributes();
		attributes.registerDestructionCallback(name, callback, getScope());
	}
	// 作用：解析上下文相关的引用对象。
	@Override
	@Nullable
	public Object resolveContextualObject(String key) {
		RequestAttributes attributes = RequestContextHolder.currentRequestAttributes();
		return attributes.resolveReference(key);
	}


	/**
	 * Template method that determines the actual target scope.
	 * @return the target scope, in the form of an appropriate
	 * {@link RequestAttributes} constant
	 * @see RequestAttributes#SCOPE_REQUEST
	 * @see RequestAttributes#SCOPE_SESSION
	 */
	// 返回值：必须返回 RequestAttributes 定义的常量（如 SCOPE_REQUEST 为 0，SCOPE_SESSION 为 1）。
	// 重要性：子类（如 RequestScope 和 SessionScope）通过实现此方法来决定父类操作的是请求还是会话。
	protected abstract int getScope();

}
