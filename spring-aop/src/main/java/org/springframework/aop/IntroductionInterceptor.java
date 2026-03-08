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

import org.aopalliance.intercept.MethodInterceptor;

/**
 * Subinterface of AOP Alliance MethodInterceptor that allows additional interfaces
 * to be implemented by the interceptor, and available via a proxy using that
 * interceptor. This is a fundamental AOP concept called <b>introduction</b>.
 *
 * <p>Introductions are often <b>mixins</b>, enabling the building of composite
 * objects that can achieve many of the goals of multiple inheritance in Java.
 *
 * @author Rod Johnson
 * @see DynamicIntroductionAdvice
 */
// IntroductionInterceptor 的主要作用是实现 Mixin（混入） 模式。
// 功能合并：它同时继承了 MethodInterceptor（用于拦截现有方法）和 DynamicIntroductionAdvice（用于动态引入新接口）。
// 状态与行为的扩展：普通的拦截器只能修改现有方法的行为，而引介拦截器可以为目标对象添加全新的状态和方法。
// 多重继承的替代方案：在 Java 不支持多重继承的情况下，引介允许你将多个接口的实现“混入”到一个代理对象中，使该对象在逻辑上成为多个类型的组合体。
public interface IntroductionInterceptor extends MethodInterceptor, DynamicIntroductionAdvice {

}
