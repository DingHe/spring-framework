/*
 * Copyright 2002-2012 the original author or authors.
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

import java.beans.PropertyEditor;

import org.springframework.lang.Nullable;

/**
 * Encapsulates methods for registering JavaBeans {@link PropertyEditor PropertyEditors}.
 * This is the central interface that a {@link PropertyEditorRegistrar} operates on.
 *
 * <p>Extended by {@link BeanWrapper}; implemented by {@link BeanWrapperImpl}
 * and {@link org.springframework.validation.DataBinder}.
 *
 * @author Juergen Hoeller
 * @since 1.2.6
 * @see java.beans.PropertyEditor
 * @see PropertyEditorRegistrar
 * @see BeanWrapper
 * @see org.springframework.validation.DataBinder
 */
// 该接口定义了一个注册表的标准，用于存储“类型/路径”与“编辑器”之间的映射关系。
// 集中管理：它提供了一个统一的场所来存放自定义的 PropertyEditor。
// 按需查找：当 Spring 在进行 Bean 属性填充或数据绑定（Data Binding）时，会查询这个注册表，看是否有匹配的编辑器来处理特定的字符串转对象逻辑。
// 核心支持：它是 BeanWrapper（Bean 操作核心）和 DataBinder（Web 数据绑定核心）的父接口。
public interface PropertyEditorRegistry {

	/**
	 * Register the given custom property editor for all properties of the given type.
	 * @param requiredType the type of the property
	 * @param propertyEditor the editor to register
	 */
	// 作用：为所有属于指定类型的属性注册一个全局自定义编辑器。
	// requiredType：目标类型。例如，如果你传入 java.util.Date.class，那么容器中所有类型为 Date 的属性都会使用这个编辑器。
	// propertyEditor：具体的编辑器实例。
	void registerCustomEditor(Class<?> requiredType, PropertyEditor propertyEditor);

	/**
	 * Register the given custom property editor for the given type and
	 * property, or for all properties of the given type.
	 * <p>If the property path denotes an array or Collection property,
	 * the editor will get applied either to the array/Collection itself
	 * (the {@link PropertyEditor} has to create an array or Collection value) or
	 * to each element (the {@code PropertyEditor} has to create the element type),
	 * depending on the specified required type.
	 * <p>Note: Only one single registered custom editor per property path
	 * is supported. In the case of a Collection/array, do not register an editor
	 * for both the Collection/array and each element on the same property.
	 * <p>For example, if you wanted to register an editor for "items[n].quantity"
	 * (for all values n), you would use "items.quantity" as the value of the
	 * 'propertyPath' argument to this method.
	 * @param requiredType the type of the property. This may be {@code null}
	 * if a property is given but should be specified in any case, in particular in
	 * case of a Collection - making clear whether the editor is supposed to apply
	 * to the entire Collection itself or to each of its entries. So as a general rule:
	 * <b>Do not specify {@code null} here in case of a Collection/array!</b>
	 * @param propertyPath the path of the property (name or nested path), or
	 * {@code null} if registering an editor for all properties of the given type
	 * @param propertyEditor editor to register
	 */
	// 作用：为特定路径或特定类型的属性注册编辑器，提供更细粒度的控制。
	// requiredType：可选的目标类型。
	// propertyPath：属性路径（支持嵌套）。例如 "address.zipCode" 或 "items[0].price"。如果为 null，效果等同于第一个方法。
	void registerCustomEditor(@Nullable Class<?> requiredType, @Nullable String propertyPath, PropertyEditor propertyEditor);

	/**
	 * Find a custom property editor for the given type and property.
	 * @param requiredType the type of the property (can be {@code null} if a property
	 * is given but should be specified in any case for consistency checking)
	 * @param propertyPath the path of the property (name or nested path), or
	 * {@code null} if looking for an editor for all properties of the given type
	 * @return the registered editor, or {@code null} if none
	 */
	// PropertyEditor 的主要作用是实现 字符串（String）与 Java 对象（Object）之间的双向转换。
	// 在 Spring 中，它主要负责以下任务：
	// 配置文件解析：当你 XML 中写 <property name="age" value="25"/> 时，Spring 需要把字符串 "25" 转换成 int 类型。
	// Web 参数绑定：当 URL 参数 ?date=2023-10-01 传给 Controller 时，Spring 需要把它转换成 java.util.Date 对象。
	// UI 显示：将复杂的 Java 对象格式化为字符串显示在 HTML 表单的输入框中。
	// 1) setAsText(String text) —— 反序列化 / 解析
	// 逻辑：接收一个字符串，解析它，然后调用 setValue(Object) 将转换后的结果存入内部属性。
	// 2) getAsText() —— 序列化 / 格式化
	// 逻辑：获取当前持有的对象值（通过 getValue()），并将其格式化为字符串返回。
	// 作用：根据类型和路径查找已注册的自定义编辑器。
	@Nullable
	PropertyEditor findCustomEditor(@Nullable Class<?> requiredType, @Nullable String propertyPath);

}
