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

package org.springframework.core.type;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;

/**
 * Interface that defines abstract access to the annotations of a specific
 * class, in a form that does not require that class to be loaded yet.
 *
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 2.5
 * @see StandardAnnotationMetadata
 * @see org.springframework.core.type.classreading.MetadataReader#getAnnotationMetadata()
 * @see AnnotatedTypeMetadata
 */
// AnnotationMetadata 是 Spring Framework 中极其核心的接口，它继承了 ClassMetadata（类元数据）和 AnnotatedTypeMetadata（注解型元数据）。
// 该接口的主要作用是提供对特定类的注解信息的抽象访问，且不需要该类已被 JVM 加载。
// 解耦类加载：在 Spring 的扫描阶段（如 @ComponentScan），Spring 往往只需要知道类上是否有某些注解（如 @Component 或 @Configuration），而不需要真正初始化这个类。AnnotationMetadata 允许 Spring 通过 ASM 字节码技术直接读取 .class 文件来获取信息，从而避免了不必要的类加载开销和潜在的类加载冲突。
// 统一视图：它不仅包含了类本身的基本信息（如类名、是否为接口、是否有内部类等），还集成了该类上所有注解的详细信息，包括元注解（Meta-annotations）。
// 方法探测：它提供了查找该类中被特定注解标记的方法的能力，常用于解析 @Bean 方法。
public interface AnnotationMetadata extends ClassMetadata, AnnotatedTypeMetadata {

	/**
	 * Get the fully qualified class names of all annotation types that
	 * are <em>present</em> on the underlying class.
	 * @return the annotation type names
	 */
	// 作用：获取该类上直接标注的所有注解的全限定类名。
	// 实现逻辑：通过 getAnnotations() 获取合并注解流，过滤出 isDirectlyPresent（直接存在）的注解并提取其类型名称。
	default Set<String> getAnnotationTypes() {
		return getAnnotations().stream()
				.filter(MergedAnnotation::isDirectlyPresent)
				.map(annotation -> annotation.getType().getName())
				.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	/**
	 * Get the fully qualified class names of all meta-annotation types that
	 * are <em>present</em> on the given annotation type on the underlying class.
	 * @param annotationName the fully qualified class name of the meta-annotation
	 * type to look for
	 * @return the meta-annotation type names, or an empty set if none found
	 */
	// 作用：获取指定注解在该类上作为“元注解”存在的所有注解类型名。
	// 参数：annotationName - 目标注解的全限定名。
	// 返回值：如果目标注解存在，返回其背后的元注解集合；否则返回空集。
	default Set<String> getMetaAnnotationTypes(String annotationName) {
		MergedAnnotation<?> annotation = getAnnotations().get(annotationName, MergedAnnotation::isDirectlyPresent);
		if (!annotation.isPresent()) {
			return Collections.emptySet();
		}
		return MergedAnnotations.from(annotation.getType(), SearchStrategy.INHERITED_ANNOTATIONS).stream()
				.map(mergedAnnotation -> mergedAnnotation.getType().getName())
				.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	/**
	 * Determine whether an annotation of the given type is <em>present</em> on
	 * the underlying class.
	 * @param annotationName the fully qualified class name of the annotation
	 * type to look for
	 * @return {@code true} if a matching annotation is present
	 */
	// 作用：判断该类上是否直接标注了指定的注解。
	// 详述：它只检查直接声明的注解，不包含元注解。
	default boolean hasAnnotation(String annotationName) {
		return getAnnotations().isDirectlyPresent(annotationName);
	}

	/**
	 * Determine whether the underlying class has an annotation that is itself
	 * annotated with the meta-annotation of the given type.
	 * @param metaAnnotationName the fully qualified class name of the
	 * meta-annotation type to look for
	 * @return {@code true} if a matching meta-annotation is present
	 */
	// 作用：判断该类上是否存在某个注解，而这个注解本身被 metaAnnotationName 所标注。
	default boolean hasMetaAnnotation(String metaAnnotationName) {
		return getAnnotations().get(metaAnnotationName,
				MergedAnnotation::isMetaPresent).isPresent();
	}

	/**
	 * Determine whether the underlying class has any methods that are
	 * annotated (or meta-annotated) with the given annotation type.
	 * @param annotationName the fully qualified class name of the annotation
	 * type to look for
	 */
	// 作用：判断该类中是否至少有一个方法被指定的注解（或其元注解）所标注。
	// 应用场景：常用于快速判断一个配置类中是否存在 @Bean 方法。
	default boolean hasAnnotatedMethods(String annotationName) {
		return !getAnnotatedMethods(annotationName).isEmpty();
	}

	/**
	 * Retrieve the method metadata for all methods that are annotated
	 * (or meta-annotated) with the given annotation type.
	 * <p>For any returned method, {@link MethodMetadata#isAnnotated} will
	 * return {@code true} for the given annotation type.
	 * @param annotationName the fully qualified class name of the annotation
	 * type to look for
	 * @return a set of {@link MethodMetadata} for methods that have a matching
	 * annotation. The return value will be an empty set if no methods match
	 * the annotation type.
	 */
	// 作用：获取所有被指定注解（或元注解）标注的方法的元数据。
	Set<MethodMetadata> getAnnotatedMethods(String annotationName);

	/**
	 * Retrieve the method metadata for all user-declared methods on the
	 * underlying class, preserving declaration order as far as possible.
	 * @return a set of {@link MethodMetadata}
	 * @since 6.0
	 */
	// 作用：获取该类中所有用户声明的方法的元数据。
	Set<MethodMetadata> getDeclaredMethods();


	/**
	 * Factory method to create a new {@link AnnotationMetadata} instance
	 * for the given class using standard reflection.
	 * @param type the class to introspect
	 * @return a new {@link AnnotationMetadata} instance
	 * @since 5.2
	 */
	// 用于通过 Java 标准反射机制为一个已加载的类创建 AnnotationMetadata 实例。
	static AnnotationMetadata introspect(Class<?> type) {
		return StandardAnnotationMetadata.from(type);
	}

}
