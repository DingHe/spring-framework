/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.aop;

import org.aopalliance.aop.Advice;

/**
 * Common marker interface for before advice, such as {@link MethodBeforeAdvice}.
 *
 * <p>Spring supports only method before advice. Although this is unlikely to change,
 * this API is designed to allow field advice in future if desired.
 *
 * @author Rod Johnson
 * @see AfterAdvice
 */
// BeforeAdvice 是 Spring AOP 体系中的一个标识接口（Marker Interface）。它定义了通知（Advice）发生的时机，即在目标操作执行之前。
// 分类标识：它本身不包含任何方法，其存在的主要目的是为了将各种类型的“前置通知”归类。在 Spring 内部，通过 instanceof BeforeAdvice 就可以快速识别出所有需要在目标方法执行前触发的逻辑。
// 设计扩展性：如注释所述，虽然目前 Spring 仅支持方法前置通知（MethodBeforeAdvice），但该接口的设计预留了未来支持字段前置通知（Field Advice）的可能性。
public interface BeforeAdvice extends Advice {

}
