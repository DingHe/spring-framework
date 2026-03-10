/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.core.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.ReflectionUtils;

/**
 * Provides a quick way to access the attribute methods of an {@link Annotation}
 * with consistent ordering as well as a few useful utility methods.
 *
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 5.2
 */
// AttributeMethods 是 Spring Framework 注解处理模型中的一个内部工具类。它专门用于封装和高效访问 Java 注解的属性方法。
// 在 Java 中，注解的属性实际上是以“无参方法”的形式定义的（例如 public String value() default "";）。该类通过预扫描、排序和缓存，为 Spring 的注解合成（Annotation Synthesis）和别名解析提供了基础。
// 高效访问：通过预先提取注解的方法并进行排序，提供比原生反射更快的索引级（index-based）访问。
// 一致性排序：强制对注解属性按名称进行排序，确保在处理别名映射（Mapping）时，索引位置是恒定不变的。
// 安全检查：检测注解属性是否可能触发 TypeNotPresentException（通常发生在类路径缺少注解引用的 Class 时）。
// 元数据识别：快速判断注解是否包含嵌套注解、默认值等特征。
final class AttributeMethods {
	// 静态常量，表示没有任何属性方法的空对象。
	static final AttributeMethods NONE = new AttributeMethods(null, new Method[0]);
	// 线程安全的缓存，存储已解析过的注解类型及其对应的 AttributeMethods。
	static final Map<Class<? extends Annotation>, AttributeMethods> cache = new ConcurrentReferenceHashMap<>();
	// 比较器，用于按方法名称的字符串顺序对属性进行排序。
	private static final Comparator<Method> methodComparator = (m1, m2) -> {
		if (m1 != null && m2 != null) {
			return m1.getName().compareTo(m2.getName());
		}
		return (m1 != null ? -1 : 1);
	};

	// 当前对应的注解类型。
	@Nullable
	private final Class<? extends Annotation> annotationType;
	// 核心数组：存储该注解所有排序后的属性方法。
	private final Method[] attributeMethods;
	// 状态数组：记录对应索引的方法是否返回 Class、Enum 类型（这些类型加载时易报错）。
	private final boolean[] canThrowTypeNotPresentException;
	// 标识位：如果注解中至少有一个属性定义了 default 值，则为 true。
	private final boolean hasDefaultValueMethod;
	// 标识位：如果属性返回值是另一个注解或注解数组，则为 true。
	private final boolean hasNestedAnnotation;


	private AttributeMethods(@Nullable Class<? extends Annotation> annotationType, Method[] attributeMethods) {
		this.annotationType = annotationType;
		this.attributeMethods = attributeMethods;
		this.canThrowTypeNotPresentException = new boolean[attributeMethods.length];
		boolean foundDefaultValueMethod = false;
		boolean foundNestedAnnotation = false;
		for (int i = 0; i < attributeMethods.length; i++) {
			Method method = this.attributeMethods[i];
			Class<?> type = method.getReturnType();
			// 如果方法返回值不为空，则认为有默认值
			if (!foundDefaultValueMethod && (method.getDefaultValue() != null)) {
				foundDefaultValueMethod = true;
			}
			//如果返回值是注解或者注解数组，则认为是嵌套的注解
			if (!foundNestedAnnotation && (type.isAnnotation() || (type.isArray() && type.getComponentType().isAnnotation()))) {
				foundNestedAnnotation = true;
			}
			// 确保方法可以调用
			ReflectionUtils.makeAccessible(method);
			this.canThrowTypeNotPresentException[i] = (type == Class.class || type == Class[].class || type.isEnum());
		}
		this.hasDefaultValueMethod = foundDefaultValueMethod;
		this.hasNestedAnnotation = foundNestedAnnotation;
	}


	/**
	 * Determine if values from the given annotation can be safely accessed without
	 * causing any {@link TypeNotPresentException TypeNotPresentExceptions}.
	 * <p>This method is designed to cover Google App Engine's late arrival of such
	 * exceptions for {@code Class} values (instead of the more typical early
	 * {@code Class.getAnnotations() failure} on a regular JVM).
	 * @param annotation the annotation to check
	 * @return {@code true} if all values are present
	 * @see #validate(Annotation)
	 */
	// 尝试模拟加载注解的所有属性值。如果遇到类找不到等硬错误返回 false。
	boolean canLoad(Annotation annotation) {
		assertAnnotation(annotation);
		for (int i = 0; i < size(); i++) {
			if (canThrowTypeNotPresentException(i)) {
				try {
					AnnotationUtils.invokeAnnotationMethod(get(i), annotation);
				}
				catch (IllegalStateException ex) {
					// Plain invocation failure to expose -> leave up to attribute retrieval
					// (if any) where such invocation failure will be logged eventually.
				}
				catch (Throwable ex) {
					// TypeNotPresentException etc. -> annotation type not actually loadable.
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * Check if values from the given annotation can be safely accessed without causing
	 * any {@link TypeNotPresentException TypeNotPresentExceptions}.
	 * <p>This method is designed to cover Google App Engine's late arrival of such
	 * exceptions for {@code Class} values (instead of the more typical early
	 * {@code Class.getAnnotations() failure} on a regular JVM).
	 * @param annotation the annotation to validate
	 * @throws IllegalStateException if a declared {@code Class} attribute could not be read
	 * @see #canLoad(Annotation)
	 */
	// 校验注解属性。
	// 如果某个属性（如 Class 类型）指向的类不存在，则抛出 IllegalStateException。
	void validate(Annotation annotation) {
		assertAnnotation(annotation);
		for (int i = 0; i < size(); i++) {
			if (canThrowTypeNotPresentException(i)) {
				try {
					AnnotationUtils.invokeAnnotationMethod(get(i), annotation);
				}
				catch (IllegalStateException ex) {
					throw ex;
				}
				catch (Throwable ex) {
					throw new IllegalStateException("Could not obtain annotation attribute value for " +
							get(i).getName() + " declared on " + annotation.annotationType(), ex);
				}
			}
		}
	}
	// 断言传入的注解实例是否非空且类型匹配。
	private void assertAnnotation(Annotation annotation) {
		Assert.notNull(annotation, "Annotation must not be null");
		if (this.annotationType != null) {
			Assert.isInstanceOf(this.annotationType, annotation);
		}
	}

	/**
	 * Get the attribute with the specified name or {@code null} if no
	 * matching attribute exists.
	 * @param name the attribute name to find
	 * @return the attribute method or {@code null}
	 */
	// 根据方法名查找注解的方法
	@Nullable
	Method get(String name) {
		int index = indexOf(name);
		return (index != -1 ? this.attributeMethods[index] : null);
	}

	/**
	 * Get the attribute at the specified index.
	 * @param index the index of the attribute to return
	 * @return the attribute method
	 * @throws IndexOutOfBoundsException if the index is out of range
	 * ({@code index < 0 || index >= size()})
	 */
	// 返回index位置的注解
	Method get(int index) {
		return this.attributeMethods[index];
	}

	/**
	 * Determine if the attribute at the specified index could throw a
	 * {@link TypeNotPresentException} when accessed.
	 * @param index the index of the attribute to check
	 * @return {@code true} if the attribute can throw a
	 * {@link TypeNotPresentException}
	 */
	// 检查特定索引的方法在反射调用时是否可能因为类缺失而抛出异常。
	boolean canThrowTypeNotPresentException(int index) {
		return this.canThrowTypeNotPresentException[index];
	}

	/**
	 * Get the index of the attribute with the specified name, or {@code -1}
	 * if there is no attribute with the name.
	 * @param name the name to find
	 * @return the index of the attribute, or {@code -1}
	 */
	// 根据方法名，返回对应位置的索引
	int indexOf(String name) {
		for (int i = 0; i < this.attributeMethods.length; i++) {
			if (this.attributeMethods[i].getName().equals(name)) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Get the index of the specified attribute, or {@code -1} if the
	 * attribute is not in this collection.
	 * @param attribute the attribute to find
	 * @return the index of the attribute, or {@code -1}
	 */
	// 查找特定方法对象在内部数组中的位置索引。
	int indexOf(Method attribute) {
		for (int i = 0; i < this.attributeMethods.length; i++) {
			if (this.attributeMethods[i].equals(attribute)) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Get the number of attributes in this collection.
	 * @return the number of attributes
	 */
	// 返回注解属性的总数。
	int size() {
		return this.attributeMethods.length;
	}

	/**
	 * Determine if at least one of the attribute methods has a default value.
	 * @return {@code true} if there is at least one attribute method with a default value
	 */
	boolean hasDefaultValueMethod() {
		return this.hasDefaultValueMethod;
	}

	/**
	 * Determine if at least one of the attribute methods is a nested annotation.
	 * @return {@code true} if there is at least one attribute method with a nested
	 * annotation type
	 */
	boolean hasNestedAnnotation() {
		return this.hasNestedAnnotation;
	}


	/**
	 * Get the attribute methods for the given annotation type.
	 * @param annotationType the annotation type
	 * @return the attribute methods for the annotation type
	 */
	// 入口工厂方法。
	// 优先从缓存获取，若无则调用 compute 生成。
	static AttributeMethods forAnnotationType(@Nullable Class<? extends Annotation> annotationType) {
		if (annotationType == null) {
			return NONE;
		}
		return cache.computeIfAbsent(annotationType, AttributeMethods::compute);
	}

	private static AttributeMethods compute(Class<? extends Annotation> annotationType) {
		Method[] methods = annotationType.getDeclaredMethods();
		int size = methods.length;
		for (int i = 0; i < methods.length; i++) {
			if (!isAttributeMethod(methods[i])) {
				methods[i] = null;
				size--;
			}
		}
		if (size == 0) {
			return NONE;
		}
		Arrays.sort(methods, methodComparator);
		Method[] attributeMethods = Arrays.copyOf(methods, size);
		return new AttributeMethods(annotationType, attributeMethods);
	}

	private static boolean isAttributeMethod(Method method) {
		return (method.getParameterCount() == 0 && method.getReturnType() != void.class);
	}

	/**
	 * Create a description for the given attribute method suitable to use in
	 * exception messages and logs.
	 * @param attribute the attribute to describe
	 * @return a description of the attribute
	 */
	static String describe(@Nullable Method attribute) {
		if (attribute == null) {
			return "(none)";
		}
		return describe(attribute.getDeclaringClass(), attribute.getName());
	}

	/**
	 * Create a description for the given attribute method suitable to use in
	 * exception messages and logs.
	 * @param annotationType the annotation type
	 * @param attributeName the attribute name
	 * @return a description of the attribute
	 */
	static String describe(@Nullable Class<?> annotationType, @Nullable String attributeName) {
		if (attributeName == null) {
			return "(none)";
		}
		String in = (annotationType != null ? " in annotation [" + annotationType.getName() + "]" : "");
		return "attribute '" + attributeName + "'" + in;
	}

}
