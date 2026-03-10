/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.context.annotation;

import org.springframework.core.type.AnnotationMetadata;
import org.springframework.lang.Nullable;

/**
 * A variation of {@link ImportSelector} that runs after all {@code @Configuration} beans
 * have been processed. This type of selector can be particularly useful when the selected
 * imports are {@code @Conditional}.
 *
 * <p>Implementations can also extend the {@link org.springframework.core.Ordered}
 * interface or use the {@link org.springframework.core.annotation.Order} annotation to
 * indicate a precedence against other {@link DeferredImportSelector DeferredImportSelectors}.
 *
 * <p>Implementations may also provide an {@link #getImportGroup() import group} which
 * can provide additional sorting and filtering logic across different selectors.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @since 4.0
 */
// DeferredImportSelector 是 ImportSelector 的子接口。正如其名（Deferred 意为“延迟”），它是 Spring 实现 “最后决议” 导入逻辑的核心组件。
// DeferredImportSelector 的核心作用是延迟执行导入逻辑，直到所有的 @Configuration 配置类都被扫描和解析完成。
// 为什么需要延迟？
// 配合条件注解（@Conditional）：这是它最主要的应用场景。普通的 ImportSelector 在解析某个配置类时会立即触发，此时容器中其他配置类可能还没被扫描到。
// 如果你的导入逻辑依赖于“容器中是否缺失某个 Bean”（如 @ConditionalOnMissingBean），立即执行会导致判断不准
// Spring Boot 自动配置的基础：Spring Boot 的 AutoConfigurationImportSelector 就实现了这个接口。这保证了用户定义的 Bean 总是优先被注册，而自动配置类最后才根据用户定义的 Bean 状态来决定是否加载。
// 支持分组（Grouping）：由于它是延迟执行的，Spring 可以在最后阶段将来自不同选择器的导入请求进行归类、排序和统一去重。
public interface DeferredImportSelector extends ImportSelector {

	/**
	 * Return a specific import group.
	 * <p>The default implementations return {@code null} for no grouping required.
	 * @return the import group class, or {@code null} if none
	 * @since 5.0
	 */
	// 作用：返回一个用于处理该选择器结果的“分组”类。
	// 返回值：返回一个实现了 DeferredImportSelector.Group 接口的类。如果返回 null（默认行为），则该选择器不参与分组逻辑，直接执行其继承自父接口的 selectImports 方法。
	// 意义：通过指定相同的 Group 类，多个不同的 DeferredImportSelector 实例可以将它们的导入项交由同一个 Group 实例统一管理。
	@Nullable
	default Class<? extends Group> getImportGroup() {
		return null;
	}


	/**
	 * Interface used to group results from different import selectors.
	 * @since 5.0
	 */
	// 用于在所有配置类解析完成后，聚合多个选择器的结果。
	interface Group {

		/**
		 * Process the {@link AnnotationMetadata} of the importing @{@link Configuration}
		 * class using the specified {@link DeferredImportSelector}.
		 */
		// 作用：由 Spring 容器调用，将具体的选择器及其所属的导入类元数据交给 Group 处理。
		// metadata：标注了 @Import 的类的元数据。
		// selector：当前的 DeferredImportSelector 实例。
		// 典型实现：实现类通常会在这里调用 selector.selectImports(metadata)，然后将结果暂时存储在内部集合中。
		void process(AnnotationMetadata metadata, DeferredImportSelector selector);

		/**
		 * Return the {@link Entry entries} of which class(es) should be imported
		 * for this group.
		 */
		// 作用：在所有的 process 调用完成后触发。Group 可以在这里执行最终的排序（例如按 Spring Boot 自动配置的顺序）和过滤逻辑。
		Iterable<Entry> selectImports();


		/**
		 * An entry that holds the {@link AnnotationMetadata} of the importing
		 * {@link Configuration} class and the class name to import.
		 */
		// 简单的 POJO，用于封装最终的导入信息。
		class Entry {
			// metadata：触发导入动作的源配置类的元数据。
			private final AnnotationMetadata metadata;
			// importClassName：准备导入的目标配置类的全限定类名。
			private final String importClassName;

			public Entry(AnnotationMetadata metadata, String importClassName) {
				this.metadata = metadata;
				this.importClassName = importClassName;
			}

			/**
			 * Return the {@link AnnotationMetadata} of the importing
			 * {@link Configuration} class.
			 */
			public AnnotationMetadata getMetadata() {
				return this.metadata;
			}

			/**
			 * Return the fully qualified name of the class to import.
			 */
			public String getImportClassName() {
				return this.importClassName;
			}

			@Override
			public boolean equals(@Nullable Object other) {
				if (this == other) {
					return true;
				}
				if (other == null || getClass() != other.getClass()) {
					return false;
				}
				Entry entry = (Entry) other;
				return (this.metadata.equals(entry.metadata) && this.importClassName.equals(entry.importClassName));
			}

			@Override
			public int hashCode() {
				return (this.metadata.hashCode() * 31 + this.importClassName.hashCode());
			}

			@Override
			public String toString() {
				return this.importClassName;
			}
		}
	}

}
