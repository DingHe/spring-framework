/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.aop.support;

import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.Set;

import org.aopalliance.aop.Advice;

import org.springframework.aop.ClassFilter;
import org.springframework.aop.DynamicIntroductionAdvice;
import org.springframework.aop.IntroductionAdvisor;
import org.springframework.aop.IntroductionInfo;
import org.springframework.core.Ordered;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Simple {@link org.springframework.aop.IntroductionAdvisor} implementation
 * that by default applies to any class.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 11.11.2003
 */
// 在 Spring AOP 中，大部分增强是针对方法的（拦截方法），而 引介 是针对 类 的。
// 功能： 它允许你向现有的目标对象动态地添加新的接口实现，而无需修改目标类的源代码。
// 范围： 默认情况下，该类实现的 matches(Class<?> clazz) 方法始终返回 true。这意味着只要你把这个 Advisor 配置给一个代理对象，它就会尝试将指定的接口“塞”进该目标类中。
@SuppressWarnings("serial")
public class DefaultIntroductionAdvisor implements IntroductionAdvisor, ClassFilter, Ordered, Serializable {
	// 实际执行逻辑的通知对象。它负责处理新接口方法的调用。
	private final Advice advice;
	// 存储需要动态添加到目标类中的接口列表（使用 LinkedHashSet 保持插入顺序）。
	private final Set<Class<?>> interfaces = new LinkedHashSet<>();
	// 该 Advisor 在拦截器链中的执行顺序，默认是最低优先级（LOWEST_PRECEDENCE）。
	private int order = Ordered.LOWEST_PRECEDENCE;


	/**
	 * Create a DefaultIntroductionAdvisor for the given advice.
	 * @param advice the Advice to apply (may implement the
	 * {@link org.springframework.aop.IntroductionInfo} interface)
	 * @see #addInterface
	 */
	public DefaultIntroductionAdvisor(Advice advice) {
		this(advice, (advice instanceof IntroductionInfo introductionInfo ? introductionInfo : null));
	}

	/**
	 * Create a DefaultIntroductionAdvisor for the given advice.
	 * @param advice the Advice to apply
	 * @param introductionInfo the IntroductionInfo that describes
	 * the interface to introduce (may be {@code null})
	 */
	public DefaultIntroductionAdvisor(Advice advice, @Nullable IntroductionInfo introductionInfo) {
		Assert.notNull(advice, "Advice must not be null");
		this.advice = advice;
		if (introductionInfo != null) {
			Class<?>[] introducedInterfaces = introductionInfo.getInterfaces();
			if (introducedInterfaces.length == 0) {
				throw new IllegalArgumentException(
						"IntroductionInfo defines no interfaces to introduce: " + introductionInfo);
			}
			for (Class<?> ifc : introducedInterfaces) {
				addInterface(ifc);
			}
		}
	}

	/**
	 * Create a DefaultIntroductionAdvisor for the given advice.
	 * @param advice the Advice to apply
	 * @param ifc the interface to introduce
	 */
	public DefaultIntroductionAdvisor(DynamicIntroductionAdvice advice, Class<?> ifc) {
		Assert.notNull(advice, "Advice must not be null");
		this.advice = advice;
		addInterface(ifc);
	}


	/**
	 * Add the specified interface to the list of interfaces to introduce.
	 * @param ifc the interface to introduce
	 */
	public void addInterface(Class<?> ifc) {
		Assert.notNull(ifc, "Interface must not be null");
		if (!ifc.isInterface()) {
			throw new IllegalArgumentException("Specified class [" + ifc.getName() + "] must be an interface");
		}
		this.interfaces.add(ifc);
	}

	@Override
	public Class<?>[] getInterfaces() {
		return ClassUtils.toClassArray(this.interfaces);
	}

	@Override
	public void validateInterfaces() throws IllegalArgumentException {
		for (Class<?> ifc : this.interfaces) {
			if (this.advice instanceof DynamicIntroductionAdvice dynamicIntroductionAdvice &&
					!dynamicIntroductionAdvice.implementsInterface(ifc)) {
				throw new IllegalArgumentException("DynamicIntroductionAdvice [" + this.advice + "] " +
						"does not implement interface [" + ifc.getName() + "] specified for introduction");
			}
		}
	}

	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	@Override
	public Advice getAdvice() {
		return this.advice;
	}

	@Override
	public ClassFilter getClassFilter() {
		return this;
	}

	@Override
	public boolean matches(Class<?> clazz) {
		return true;
	}


	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof DefaultIntroductionAdvisor otherAdvisor &&
				this.advice.equals(otherAdvisor.advice) &&
				this.interfaces.equals(otherAdvisor.interfaces)));
	}

	@Override
	public int hashCode() {
		return this.advice.hashCode() * 13 + this.interfaces.hashCode();
	}

	@Override
	public String toString() {
		return getClass().getName() + ": advice [" + this.advice + "]; interfaces " +
				ClassUtils.classNamesToString(this.interfaces);
	}

}
