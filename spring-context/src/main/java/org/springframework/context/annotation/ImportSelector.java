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

import java.util.function.Predicate;

import org.springframework.core.type.AnnotationMetadata;
import org.springframework.lang.Nullable;

/**
 * Interface to be implemented by types that determine which @{@link Configuration}
 * class(es) should be imported based on a given selection criteria, usually one or
 * more annotation attributes.
 *
 * <p>An {@link ImportSelector} may implement any of the following
 * {@link org.springframework.beans.factory.Aware Aware} interfaces,
 * and their respective methods will be called prior to {@link #selectImports}:
 * <ul>
 * <li>{@link org.springframework.context.EnvironmentAware EnvironmentAware}</li>
 * <li>{@link org.springframework.beans.factory.BeanFactoryAware BeanFactoryAware}</li>
 * <li>{@link org.springframework.beans.factory.BeanClassLoaderAware BeanClassLoaderAware}</li>
 * <li>{@link org.springframework.context.ResourceLoaderAware ResourceLoaderAware}</li>
 * </ul>
 *
 * <p>Alternatively, the class may provide a single constructor with one or more of
 * the following supported parameter types:
 * <ul>
 * <li>{@link org.springframework.core.env.Environment Environment}</li>
 * <li>{@link org.springframework.beans.factory.BeanFactory BeanFactory}</li>
 * <li>{@link java.lang.ClassLoader ClassLoader}</li>
 * <li>{@link org.springframework.core.io.ResourceLoader ResourceLoader}</li>
 * </ul>
 *
 * <p>{@code ImportSelector} implementations are usually processed in the same way
 * as regular {@code @Import} annotations, however, it is also possible to defer
 * selection of imports until all {@code @Configuration} classes have been processed
 * (see {@link DeferredImportSelector} for details).
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.1
 * @see DeferredImportSelector
 * @see Import
 * @see ImportBeanDefinitionRegistrar
 * @see Configuration
 */
// ImportSelector 是 Spring Framework 中一个极其重要的扩展点，它是实现 Spring Boot 自动配置（Auto-configuration）以及复杂模块化配置的核心机制。
// ImportSelector 的核心作用是根据运行时上下文动态地决定要导入哪些 @Configuration 配置类。
// 与静态的 @Import(MyConfig.class) 不同，ImportSelector 允许你编写 Java 代码来动态选择配置。Spring 会执行这些实现类的方法，将返回的类名数组视为额外的配置类进行加载。
// 场景示例：比如 Spring Boot 的 EnableAutoConfiguration，它正是通过 AutoConfigurationImportSelector（ImportSelector 的实现）读取 spring.factories 或 spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports 文件，根据条件自动加载所需的 Bean 配置。

public interface ImportSelector {

	/**
	 * Select and return the names of which class(es) should be imported based on
	 * the {@link AnnotationMetadata} of the importing @{@link Configuration} class.
	 * @return the class names, or an empty array if none
	 */
	// 实现类的逻辑核心。
	// 它接收导入该选择器的配置类的元数据（AnnotationMetadata），并返回一组全限定类名字符串数组。
	// importingClassMetadata：当前导入该选择器的类（通常带有 @Import 注解的类）的注解元数据。通过它，开发者可以读取该类上的注解属性，从而决定到底该“导入”哪些配置。
	// 返回值是 String[]，即需要被导入的 @Configuration 类（或组件）的全限定类名。如果返回空数组，则表示不导入任何类。
	String[] selectImports(AnnotationMetadata importingClassMetadata);

	/**
	 * Return a predicate for excluding classes from the import candidates, to be
	 * transitively applied to all classes found through this selector's imports.
	 * <p>If this predicate returns {@code true} for a given fully-qualified
	 * class name, said class will not be considered as an imported configuration
	 * class, bypassing class file loading as well as metadata introspection.
	 * @return the filter predicate for fully-qualified candidate class names
	 * of transitively imported configuration classes, or {@code null} if none
	 * @since 5.2.4
	 */
	// 提供一个过滤器，用于在处理导入的类时排除特定的候选者。
	// 返回一个 Predicate<String>。如果该 Predicate 对某个类名返回 true，则该类将被过滤掉，不被考虑为配置类，甚至不会加载其字节码。
	@Nullable
	default Predicate<String> getExclusionFilter() {
		return null;
	}

}
