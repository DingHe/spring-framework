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

package org.springframework.aop.framework;

/**
 * Marker interface that indicates a bean that is part of Spring's
 * AOP infrastructure. In particular, this implies that any such bean
 * is not subject to auto-proxying, even if a pointcut would match.
 *
 * @author Juergen Hoeller
 * @since 2.0.3
 * @see org.springframework.aop.framework.autoproxy.AbstractAutoProxyCreator
 * @see org.springframework.aop.scope.ScopedProxyFactoryBean
 */
// AopInfrastructureBean 是 Spring AOP 体系中一个非常特殊且重要的标记接口（Marker Interface）。它不包含任何方法或属性，但它的存在对 Spring 容器的 自动代理（Auto-proxying） 逻辑具有决定性的影响。
// 身份标识：它告诉 Spring 容器：“实现这个接口的 Bean 是 Spring AOP 基础设施的一部分（例如拦截器、切点、顾问等）”。
// 防止死循环与递归：Spring 在启动时会使用 AnnotationAwareAspectJAutoProxyCreator 等后置处理器来扫描所有的 Bean，并判断是否需要为它们创建代理。如果 AOP 基础设施类本身也被代理了，那么在执行代理逻辑时又会触发 AOP 逻辑，极易导致无限递归或复杂的启动错误。
// 性能优化：标记为基础设施 Bean 后，Spring 会直接跳过对这些 Bean 的切点匹配检查。
public interface AopInfrastructureBean {

}
