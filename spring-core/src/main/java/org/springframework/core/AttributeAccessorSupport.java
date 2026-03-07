/*
 * Copyright 2002-2023 the original author or authors.
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

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Support class for {@link AttributeAccessor AttributeAccessors}, providing
 * a base implementation of all methods. To be extended by subclasses.
 *
 * <p>{@link Serializable} if subclasses and all attribute values are {@link Serializable}.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 2.0
 */
// 主要用于为那些需要动态附加额外属性的对象提供统一的存储和访问支持。它是 AttributeAccessor 接口的默认实现。
// 在 Spring 的设计中，很多核心对象（如 BeanDefinition 或 TestContext）除了拥有固定的成员变量外，往往还需要在运行时临时挂载一些元数据（Metadata）。
// 元数据容器：它本质上是一个“挂件箱”，允许开发者在不修改类结构的情况下，以键值对（Key-Value）的形式向对象添加自定义属性。
// 统一实现：它通过内部封装一个 Map，实现了 AttributeAccessor 接口定义的所有方法，避免了子类重复编写通用的属性操作逻辑。
// 序列化支持：如果属性值（Value）本身是可序列化的，那么整个支持类及其子类也是可序列化的。
@SuppressWarnings("serial")
public abstract class AttributeAccessorSupport implements AttributeAccessor, Serializable {

	/** Map with String keys and Object values. */
	// 存放所有动态属性的底层容器。
	private final Map<String, Object> attributes = new LinkedHashMap<>();


	@Override
	public void setAttribute(String name, @Nullable Object value) {
		Assert.notNull(name, "Name must not be null");
		if (value != null) {
			this.attributes.put(name, value);
		}
		else {
			removeAttribute(name);
		}
	}

	@Override
	@Nullable
	public Object getAttribute(String name) {
		Assert.notNull(name, "Name must not be null");
		return this.attributes.get(name);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T computeAttribute(String name, Function<String, T> computeFunction) {
		Assert.notNull(name, "Name must not be null");
		Assert.notNull(computeFunction, "Compute function must not be null");
		Object value = this.attributes.computeIfAbsent(name, computeFunction);
		Assert.state(value != null,
				() -> String.format("Compute function must not return null for attribute named '%s'", name));
		return (T) value;
	}

	@Override
	@Nullable
	public Object removeAttribute(String name) {
		Assert.notNull(name, "Name must not be null");
		return this.attributes.remove(name);
	}

	@Override
	public boolean hasAttribute(String name) {
		Assert.notNull(name, "Name must not be null");
		return this.attributes.containsKey(name);
	}

	@Override
	public String[] attributeNames() {
		return StringUtils.toStringArray(this.attributes.keySet());
	}


	/**
	 * Copy the attributes from the supplied AttributeAccessor to this accessor.
	 * @param source the AttributeAccessor to copy from
	 */
	protected void copyAttributesFrom(AttributeAccessor source) {
		Assert.notNull(source, "Source must not be null");
		String[] attributeNames = source.attributeNames();
		for (String attributeName : attributeNames) {
			setAttribute(attributeName, source.getAttribute(attributeName));
		}
	}


	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof AttributeAccessorSupport that &&
				this.attributes.equals(that.attributes)));
	}

	@Override
	public int hashCode() {
		return this.attributes.hashCode();
	}

}
