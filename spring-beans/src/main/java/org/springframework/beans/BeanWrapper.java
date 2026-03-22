/*
 * Copyright 2002-2017 the original author or authors.
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

import java.beans.PropertyDescriptor;

/**
 * The central interface of Spring's low-level JavaBeans infrastructure.
 *
 * <p>Typically not used directly but rather implicitly via a
 * {@link org.springframework.beans.factory.BeanFactory} or a
 * {@link org.springframework.validation.DataBinder}.
 *
 * <p>Provides operations to analyze and manipulate standard JavaBeans:
 * the ability to get and set property values (individually or in bulk),
 * get property descriptors, and query the readability/writability of properties.
 *
 * <p>This interface supports <b>nested properties</b> enabling the setting
 * of properties on subproperties to an unlimited depth.
 *
 * <p>A BeanWrapper's default for the "extractOldValueForEditor" setting
 * is "false", to avoid side effects caused by getter method invocations.
 * Turn this to "true" to expose present property values to custom editors.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 13 April 2001
 * @see PropertyAccessor
 * @see PropertyEditorRegistry
 * @see PropertyAccessorFactory#forBeanPropertyAccess
 * @see org.springframework.beans.factory.BeanFactory
 * @see org.springframework.validation.BeanPropertyBindingResult
 * @see org.springframework.validation.DataBinder#initBeanPropertyAccess()
 */
// BeanWrapper 是最核心的接口之一。如果说 PropertyAccessor 定义了“如何访问属性”，那么 BeanWrapper 就是这个行为的具体执行者和管理者。
// 它是对一个 Java 对象的“包装器”，赋予了普通 POJO 对象强大的“自省”和“动态操作”能力。
// BeanWrapper 是 Spring 低层 JavaBean 基础设施的中央接口。它的主要作用包括：
// 对象包装与代理：它包装了一个具体的 Java 实例，充当该实例的“操作手”。
// JavaBeans 规范实现：它遵循标准 JavaBeans 内省（Introspection）机制，能够分析并操作 Getter/Setter 方法。
// 级联属性操作：支持对无限深度的嵌套属性（如 user.office.address.street）进行读写。
// 自动集合增长：在设置值时，如果目标集合或数组太小，它可以自动扩容以防止越界异常。
// 类型转换集成：通过继承自 PropertyEditorRegistry 和 ConfigurablePropertyAccessor，它能自动将字符串（如从配置文件读入的）转换为目标属性所需的类型（如 Date、URL 等）。
public interface BeanWrapper extends ConfigurablePropertyAccessor {

	/**
	 * Specify a limit for array and collection auto-growing.
	 * <p>Default is unlimited on a plain BeanWrapper.
	 * @since 4.1
	 */
	// 设置数组或集合自动增长的阈值。
	void setAutoGrowCollectionLimit(int autoGrowCollectionLimit);

	/**
	 * Return the limit for array and collection auto-growing.
	 * @since 4.1
	 */
	// 获取当前的自动增长限制值。
	int getAutoGrowCollectionLimit();

	/**
	 * Return the bean instance wrapped by this object.
	 */
	// 获取当前被 BeanWrapper 包装的原始 Java 对象实例。
	Object getWrappedInstance();

	/**
	 * Return the type of the wrapped bean instance.
	 */
	// 获取被包装对象的 Class 类型。
	Class<?> getWrappedClass();

	/**
	 * Obtain the PropertyDescriptors for the wrapped object
	 * (as determined by standard JavaBeans introspection).
	 * @return the PropertyDescriptors for the wrapped object
	 */
	// 获取被包装对象所有属性的 PropertyDescriptor 数组。
	PropertyDescriptor[] getPropertyDescriptors();

	/**
	 * Obtain the property descriptor for a specific property
	 * of the wrapped object.
	 * @param propertyName the property to obtain the descriptor for
	 * (may be a nested path, but not an indexed/mapped property)
	 * @return the property descriptor for the specified property
	 * @throws InvalidPropertyException if there is no such property
	 */
	// 根据属性名获取特定的属性描述符。
	PropertyDescriptor getPropertyDescriptor(String propertyName) throws InvalidPropertyException;

}
