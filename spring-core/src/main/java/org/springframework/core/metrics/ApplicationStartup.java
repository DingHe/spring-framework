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

package org.springframework.core.metrics;

/**
 * Instruments the application startup phase using {@link StartupStep steps}.
 * <p>The core container and its infrastructure components can use the {@code ApplicationStartup}
 * to mark steps during the application startup and collect data about the execution context
 * or their processing time.
 *
 * @author Brian Clozel
 * @since 5.3
 */
// ApplicationStartup 是 Spring 5.3 引入的一个核心接口，旨在监控和记录应用程序启动阶段的性能指标。它是 Spring 框架数字化转型的一环，帮助开发者精细化分析启动过程中的耗时点。
// 在大型 Spring Boot 应用中，启动慢往往是由于 Bean 数量过多、某些第三方组件初始化耗时长或配置加载复杂导致的。
// 性能打点：它提供了一种标准化的方式，在 Spring 容器启动的各个阶段（如扫描路径、解析配置、实例化 Bean）插入“步骤（Step）”标记。
// 诊断工具：通过收集这些数据，开发者可以使用监控工具（如 JMX 或 JSON 输出）查看启动过程的完整树状图。
public interface ApplicationStartup {

	/**
	 * Default "no op" {@code ApplicationStartup} implementation.
	 * <p>This variant is designed for minimal overhead and does not record data.
	 */
	ApplicationStartup DEFAULT = new DefaultApplicationStartup();

	/**
	 * Create a new step and marks its beginning.
	 * <p>A step name describes the current action or phase. This technical
	 * name should be "." namespaced and can be reused to describe other instances of
	 * the same step during application startup.
	 * @param name the step name
	 */
	// 作用：创建一个新的“启动步骤（Step）”并标记其开始时间。
	StartupStep start(String name);

}
