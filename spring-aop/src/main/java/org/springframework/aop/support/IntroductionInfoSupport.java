/*
 * Copyright 2002-2018 the original author or authors.
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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.aopalliance.intercept.MethodInvocation;

import org.springframework.aop.IntroductionInfo;
import org.springframework.util.ClassUtils;

/**
 * Support for implementations of {@link org.springframework.aop.IntroductionInfo}.
 *
 * <p>Allows subclasses to conveniently add all interfaces from a given object,
 * and to suppress interfaces that should not be added. Also allows for querying
 * all introduced interfaces.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
// IntroductionInfoSupport 是 Spring AOP 框架中一个基础性的辅助类，专门用于支持 引介（Introduction） 功能。
// 它实现了 IntroductionInfo 接口，主要负责管理和追踪那些被“混入”（Mixin）到代理对象中的额外接口。
// 接口管理中心：它维护了一个集合，记录了哪些接口应该被暴露给 AOP 代理。
// 引介决策支持：它提供了一套逻辑，用于判断某个方法调用是否属于“新引入的接口”，从而帮助拦截器决定是将请求转发给原始目标对象，还是由引介逻辑自己处理。
// 简化开发：它是 DelegatingIntroductionInterceptor 等核心引介类的基类，封装了接口检测、过滤和缓存的通用逻辑，避免子类重复编写繁琐的反射代码。
@SuppressWarnings("serial")
public class IntroductionInfoSupport implements IntroductionInfo, Serializable {
	// 作用：存储所有**已发布（公开）**的引介接口。
	protected final Set<Class<?>> publishedInterfaces = new LinkedHashSet<>();
	// 作用：方法检测的结果缓存。
	private transient Map<Method, Boolean> rememberedMethods = new ConcurrentHashMap<>(32);


	/**
	 * Suppress the specified interface, which may have been autodetected
	 * due to the delegate implementing it. Call this method to exclude
	 * internal interfaces from being visible at the proxy level.
	 * <p>Does nothing if the interface is not implemented by the delegate.
	 * @param ifc the interface to suppress
	 */
	// 作用：**压制（排除）**指定的接口。
	public void suppressInterface(Class<?> ifc) {
		this.publishedInterfaces.remove(ifc);
	}
	// 作用：获取当前所有引介接口的数组。
	@Override
	public Class<?>[] getInterfaces() {
		return ClassUtils.toClassArray(this.publishedInterfaces);
	}

	/**
	 * Check whether the specified interfaces is a published introduction interface.
	 * @param ifc the interface to check
	 * @return whether the interface is part of this introduction
	 */
	// 作用：判断给定的接口（或其父接口）是否在已发布的引介接口列表中。
	public boolean implementsInterface(Class<?> ifc) {
		for (Class<?> pubIfc : this.publishedInterfaces) {
			if (ifc.isInterface() && ifc.isAssignableFrom(pubIfc)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Publish all interfaces that the given delegate implements at the proxy level.
	 * @param delegate the delegate object
	 */
	// 作用：自动扫描并注册给定对象实现的所有接口。
	protected void implementInterfacesOnObject(Object delegate) {
		this.publishedInterfaces.addAll(ClassUtils.getAllInterfacesAsSet(delegate));
	}

	/**
	 * Is this method on an introduced interface?
	 * @param mi the method invocation
	 * @return whether the invoked method is on an introduced interface
	 */
	// 作用：判断当前被调用的方法是否属于引介接口。
	protected final boolean isMethodOnIntroducedInterface(MethodInvocation mi) {
		Boolean rememberedResult = this.rememberedMethods.get(mi.getMethod());
		if (rememberedResult != null) {
			return rememberedResult;
		}
		else {
			// Work it out and cache it.
			boolean result = implementsInterface(mi.getMethod().getDeclaringClass());
			this.rememberedMethods.put(mi.getMethod(), result);
			return result;
		}
	}


	//---------------------------------------------------------------------
	// Serialization support
	//---------------------------------------------------------------------

	/**
	 * This method is implemented only to restore the logger.
	 * We don't make the logger static as that would mean that subclasses
	 * would use this class's log category.
	 */
	// 作用：自定义反序列化逻辑。
	private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		// Rely on default serialization; just initialize state after deserialization.
		ois.defaultReadObject();
		// Initialize transient fields.
		this.rememberedMethods = new ConcurrentHashMap<>(32);
	}

}
