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

/**
 * Interface for strategies that register custom
 * {@link java.beans.PropertyEditor property editors} with a
 * {@link org.springframework.beans.PropertyEditorRegistry property editor registry}.
 *
 * <p>This is particularly useful when you need to use the same set of
 * property editors in several situations: write a corresponding
 * registrar and reuse that in each case.
 *
 * @author Juergen Hoeller
 * @since 1.2.6
 * @see PropertyEditorRegistry
 * @see java.beans.PropertyEditor
 */
// PropertyEditorRegistrar 是 Spring 框架中用于批量注册自定义属性编辑器（PropertyEditor）的策略接口。它解决了 PropertyEditor 非线程安全带来的复用难题。
// 在 Spring 中，PropertyEditor 是有状态的（Stateful），因此它不是线程安全的。这意味着我们不能定义一个单例的 PropertyEditor 并将其注入到多个地方。
// 集中定义，多次注册：你可以编写一个实现类，在其中定义一套标准的编辑器注册逻辑（例如统一的日期格式、货币格式），然后在不同的场景（如多个 Controller 或多个 BeanFactory）中重复调用。
// 解决线程安全问题：每当 Spring 需要进行数据绑定（如处理一个 Web 请求）时，它会调用 Registrar 的方法。实现类会在方法内部为当前请求 new 出全新的编辑器实例，从而避免了并发竞争。
// 解耦：将具体的“如何注册编辑器”的逻辑从业务代码（如 Controller 的 @InitBinder）或复杂的容器配置中剥离出来。
public interface PropertyEditorRegistrar {

	/**
	 * Register custom {@link java.beans.PropertyEditor PropertyEditors} with
	 * the given {@code PropertyEditorRegistry}.
	 * <p>The passed-in registry will usually be a {@link BeanWrapper} or a
	 * {@link org.springframework.validation.DataBinder DataBinder}.
	 * <p>It is expected that implementations will create brand new
	 * {@code PropertyEditors} instances for each invocation of this
	 * method (since {@code PropertyEditors} are not threadsafe).
	 * @param registry the {@code PropertyEditorRegistry} to register the
	 * custom {@code PropertyEditors} with
	 */
	// 将一组自定义的 PropertyEditor 注册到给定的 PropertyEditorRegistry（注册表）中。
	// registry：这是注册表接口。在实际运行中，传入的通常是 BeanWrapperImpl（用于填充 Bean 属性）或 DataBinder（用于 Web 参数绑定）。
	void registerCustomEditors(PropertyEditorRegistry registry);

}
