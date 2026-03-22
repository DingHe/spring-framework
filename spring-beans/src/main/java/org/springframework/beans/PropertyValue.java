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

package org.springframework.beans;

import java.io.Serializable;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Object to hold information and value for an individual bean property.
 * Using an object here, rather than just storing all properties in
 * a map keyed by property name, allows for more flexibility, and the
 * ability to handle indexed properties etc in an optimized way.
 *
 * <p>Note that the value doesn't need to be the final required type:
 * A {@link BeanWrapper} implementation should handle any necessary conversion,
 * as this object doesn't know anything about the objects it will be applied to.
 *
 * @author Rod Johnson
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 13 May 2001
 * @see PropertyValues
 * @see BeanWrapper
 */
// 如果说 BeanDefinition 是一个 Bean 的“蓝图”，那么 PropertyValue 就是蓝图上标注的具体“零件信息”。
// PropertyValue 的核心作用是持有单个 Bean 属性的名称和值。
// 在 Spring 容器初始化 Bean 之前，配置信息（如 XML 中的 <property> 标签或注解中的值）会被解析并封装成一个个 PropertyValue 对象。
// 解耦存储：它不直接修改目标对象，而是作为一个“中间载体”，存储属性名和原始值（可能是字符串、表达式等）。
// 支持转换：它允许存储“原始值”和“转换后的值”。例如，XML 里写的 "10"（String），经过 BeanWrapper 处理后，转换后的值就是 10（Integer），这两个状态都可以在此对象中管理。
// 灵活性：相比于简单的 Map<String, Object>，使用对象可以携带更多的元数据（如是否可选、来源信息、属性路径标记等）。
@SuppressWarnings("serial")
public class PropertyValue extends BeanMetadataAttributeAccessor implements Serializable {
	// 必填。属性的名称（对应 Java Bean 的字段名或 Setter 方法名）。
	private final String name;
	// 属性的原始值。在转换之前，通常是配置中的原始字符串或对象引用。
	@Nullable
	private final Object value;
	// 标记是否为“可选”。如果为 true，且目标类中没有对应的 Setter 方法，Spring 会忽略它而不报错。
	private boolean optional = false;
	// 标记该属性值是否已经过类型转换处理。
	private boolean converted = false;
	// 存储经过类型转换后的最终值（例如从 String 转换成了 User 对象）。
	@Nullable
	private Object convertedValue;
	// 易失性标记。用于内部优化，指示该值是否真的需要执行类型转换。
	/** Package-visible field that indicates whether conversion is necessary. */
	@Nullable
	volatile Boolean conversionNecessary;

	/** Package-visible field for caching the resolved property path tokens. */
	// 缓存标记。用于缓存解析后的属性路径（如 address.city）的内部令牌，提高反射效率。
	@Nullable
	transient volatile Object resolvedTokens;


	/**
	 * Create a new PropertyValue instance.
	 * @param name the name of the property (never {@code null})
	 * @param value the value of the property (possibly before type conversion)
	 */
	public PropertyValue(String name, @Nullable Object value) {
		Assert.notNull(name, "Name must not be null");
		this.name = name;
		this.value = value;
	}

	/**
	 * Copy constructor.
	 * @param original the PropertyValue to copy (never {@code null})
	 */
	public PropertyValue(PropertyValue original) {
		Assert.notNull(original, "Original must not be null");
		this.name = original.getName();
		this.value = original.getValue();
		this.optional = original.isOptional();
		this.converted = original.converted;
		this.convertedValue = original.convertedValue;
		this.conversionNecessary = original.conversionNecessary;
		this.resolvedTokens = original.resolvedTokens;
		setSource(original.getSource());
		copyAttributesFrom(original);
	}

	/**
	 * Constructor that exposes a new value for an original value holder.
	 * The original holder will be exposed as source of the new holder.
	 * @param original the PropertyValue to link to (never {@code null})
	 * @param newValue the new value to apply
	 */
	public PropertyValue(PropertyValue original, @Nullable Object newValue) {
		Assert.notNull(original, "Original must not be null");
		this.name = original.getName();
		this.value = newValue;
		this.optional = original.isOptional();
		this.conversionNecessary = original.conversionNecessary;
		this.resolvedTokens = original.resolvedTokens;
		setSource(original);
		copyAttributesFrom(original);
	}


	/**
	 * Return the name of the property.
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Return the value of the property.
	 * <p>Note that type conversion will <i>not</i> have occurred here.
	 * It is the responsibility of the BeanWrapper implementation to
	 * perform type conversion.
	 */
	@Nullable
	public Object getValue() {
		return this.value;
	}

	/**
	 * Return the original PropertyValue instance for this value holder.
	 * @return the original PropertyValue (either a source of this
	 * value holder or this value holder itself).
	 */
	public PropertyValue getOriginalPropertyValue() {
		PropertyValue original = this;
		Object source = getSource();
		while (source instanceof PropertyValue pv && source != original) {
			original = pv;
			source = original.getSource();
		}
		return original;
	}

	/**
	 * Set whether this is an optional value, that is, to be ignored
	 * when no corresponding property exists on the target class.
	 * @since 3.0
	 */
	public void setOptional(boolean optional) {
		this.optional = optional;
	}

	/**
	 * Return whether this is an optional value, that is, to be ignored
	 * when no corresponding property exists on the target class.
	 * @since 3.0
	 */
	public boolean isOptional() {
		return this.optional;
	}

	/**
	 * Return whether this holder contains a converted value already ({@code true}),
	 * or whether the value still needs to be converted ({@code false}).
	 */
	public synchronized boolean isConverted() {
		return this.converted;
	}

	/**
	 * Set the converted value of this property value,
	 * after processed type conversion.
	 */
	public synchronized void setConvertedValue(@Nullable Object value) {
		this.converted = true;
		this.convertedValue = value;
	}

	/**
	 * Return the converted value of this property value,
	 * after processed type conversion.
	 */
	@Nullable
	public synchronized Object getConvertedValue() {
		return this.convertedValue;
	}


	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof PropertyValue that &&
				this.name.equals(that.name) &&
				ObjectUtils.nullSafeEquals(this.value, that.value) &&
				ObjectUtils.nullSafeEquals(getSource(), that.getSource())));
	}

	@Override
	public int hashCode() {
		return this.name.hashCode() * 29 + ObjectUtils.nullSafeHashCode(this.value);
	}

	@Override
	public String toString() {
		return "bean property '" + this.name + "'";
	}

}
