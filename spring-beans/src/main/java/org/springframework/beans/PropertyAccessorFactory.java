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

package org.springframework.beans;

/**
 * Simple factory facade for obtaining {@link PropertyAccessor} instances,
 * in particular for {@link BeanWrapper} instances. Conceals the actual
 * target implementation classes and their extended public signature.
 *
 * @author Juergen Hoeller
 * @since 2.5.2
 */
// PropertyAccessorFactory 的主要作用是为开发者提供获取属性访问器（PropertyAccessor）的便捷入口，并隐藏底层具体实现类的细节。
// 解耦：开发者只需要知道 BeanWrapper 或 ConfigurablePropertyAccessor 接口，而不需要直接引用具体的实现类（如 BeanWrapperImpl 或 DirectFieldAccessor）。
// 策略选择：它提供了两种截然不同的对象操作策略：
// JavaBeans 风格：通过 Getter/Setter 方法访问属性（符合标准规范）。
// 直接字段风格：无视方法，直接通过反射操作类的私有或公有成员变量（Field）。
public final class PropertyAccessorFactory {

	private PropertyAccessorFactory() {
	}


	/**
	 * Obtain a BeanWrapper for the given target object,
	 * accessing properties in JavaBeans style.
	 * @param target the target object to wrap
	 * @return the property accessor
	 * @see BeanWrapperImpl
	 */
	// 为目标对象创建一个 BeanWrapper（它是 PropertyAccessor 的子接口）。
	// 访问风格：JavaBeans 风格。它会查找符合 getFoo() 和 setFoo(value) 规范的方法来读写属性。
	public static BeanWrapper forBeanPropertyAccess(Object target) {
		return new BeanWrapperImpl(target);
	}

	/**
	 * Obtain a PropertyAccessor for the given target object,
	 * accessing properties in direct field style.
	 * @param target the target object to wrap
	 * @return the property accessor
	 * @see DirectFieldAccessor
	 */
	// 作用：为目标对象创建一个可以直接访问字段的访问器。
	// 访问风格：直接字段访问（Direct Field Access）。它会绕过 Getter/Setter 方法，利用反射直接读写对象上的变量。
	public static ConfigurablePropertyAccessor forDirectFieldAccess(Object target) {
		return new DirectFieldAccessor(target);
	}

}
