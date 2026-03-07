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

package org.springframework.core;

import java.util.function.Function;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Interface defining a generic contract for attaching and accessing metadata
 * to/from arbitrary objects.
 *
 * @author Rob Harrop
 * @author Sam Brannen
 * @since 2.0
 */
// 在软件设计中，我们经常需要给某些对象添加额外的临时信息，但又不希望修改这些对象的类定义。AttributeAccessor 就充当了一个**“元数据容器”**的作用：
// 扩展性：允许在运行时动态地为对象添加属性，而不需要继承或修改原有类。
// 统一契约：Spring 中的许多核心组件（如 BeanDefinition、TestContext）都实现了这个接口，从而让开发者可以用统一的 API 来操作这些组件的附加属性。
// 解耦：通过 key-value 的形式存储数据，使得不同的第三方库或插件可以在不感知对方的情况下，共享同一个对象上的上下文信息。

public interface AttributeAccessor {

	/**
	 * Set the attribute defined by {@code name} to the supplied {@code value}.
	 * <p>If {@code value} is {@code null}, the attribute is {@link #removeAttribute removed}.
	 * <p>In general, users should take care to prevent overlaps with other
	 * metadata attributes by using fully-qualified names, perhaps using
	 * class or package names as prefix.
	 * @param name the unique attribute key
	 * @param value the attribute value to be attached
	 */
	// 作用：将指定的 value 绑定到唯一的 name 上。
	// 如果传入的 value 为 null，该方法的行为等同于调用 removeAttribute(name)，即删除该属性。
	// 为了防止与其他组件定义的属性冲突，Spring 官方建议使用全限定名（如类名或包名作为前缀）作为 key。
	void setAttribute(String name, @Nullable Object value);

	/**
	 * Get the value of the attribute identified by {@code name}.
	 * <p>Return {@code null} if the attribute doesn't exist.
	 * @param name the unique attribute key
	 * @return the current value of the attribute, if any
	 */
	// 作用：根据指定的 name 获取绑定的属性值。
	@Nullable
	Object getAttribute(String name);

	/**
	 * Compute a new value for the attribute identified by {@code name} if
	 * necessary and {@linkplain #setAttribute set} the new value in this
	 * {@code AttributeAccessor}.
	 * <p>If a value for the attribute identified by {@code name} already exists
	 * in this {@code AttributeAccessor}, the existing value will be returned
	 * without applying the supplied compute function.
	 * <p>The default implementation of this method is not thread safe but can
	 * be overridden by concrete implementations of this interface.
	 * @param <T> the type of the attribute value
	 * @param name the unique attribute key
	 * @param computeFunction a function that computes a new value for the attribute
	 * name; the function must not return a {@code null} value
	 * @return the existing value or newly computed value for the named attribute
	 * @since 5.3.3
	 * @see #getAttribute(String)
	 * @see #setAttribute(String, Object)
	 */
	// 作用：这是一个**默认实现（default）**方法，用于“计算并设置”属性。它结合了“获取”和“设置”的逻辑，确保属性在不存在时被自动初始化。
	@SuppressWarnings("unchecked")
	default <T> T computeAttribute(String name, Function<String, T> computeFunction) {
		Assert.notNull(name, "Name must not be null");
		Assert.notNull(computeFunction, "Compute function must not be null");
		Object value = getAttribute(name);
		if (value == null) {
			value = computeFunction.apply(name);
			Assert.state(value != null,
					() -> String.format("Compute function must not return null for attribute named '%s'", name));
			setAttribute(name, value);
		}
		return (T) value;
	}

	/**
	 * Remove the attribute identified by {@code name} and return its value.
	 * <p>Return {@code null} if no attribute under {@code name} is found.
	 * @param name the unique attribute key
	 * @return the last value of the attribute, if any
	 */
	// 作用：从对象中移除名为 name 的属性。
	// 返回：被移除的属性值。如果原本就没有该属性，则返回 null。
	@Nullable
	Object removeAttribute(String name);

	/**
	 * Return {@code true} if the attribute identified by {@code name} exists.
	 * <p>Otherwise return {@code false}.
	 * @param name the unique attribute key
	 */
	// 作用：检查是否存在名为 name 的属性。
	boolean hasAttribute(String name);

	/**
	 * Return the names of all attributes.
	 */
	// 作用：获取当前对象中所有已定义属性的名称。
	String[] attributeNames();

}
