/*
 * Copyright 2002-2022 the original author or authors.
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

import org.aopalliance.intercept.MethodInvocation;

import org.springframework.aop.DynamicIntroductionAdvice;
import org.springframework.aop.IntroductionInterceptor;
import org.springframework.aop.ProxyMethodInvocation;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Convenient implementation of the
 * {@link org.springframework.aop.IntroductionInterceptor} interface.
 *
 * <p>Subclasses merely need to extend this class and implement the interfaces
 * to be introduced themselves. In this case the delegate is the subclass
 * instance itself. Alternatively a separate delegate may implement the
 * interface, and be set via the delegate bean property.
 *
 * <p>Delegates or subclasses may implement any number of interfaces.
 * All interfaces except IntroductionInterceptor are picked up from
 * the subclass or delegate by default.
 *
 * <p>The {@code suppressInterface} method can be used to suppress interfaces
 * implemented by the delegate but which should not be introduced to the owning
 * AOP proxy.
 *
 * <p>An instance of this class is serializable if the delegate is.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 16.11.2003
 * @see #suppressInterface
 * @see DelegatePerTargetObjectIntroductionInterceptor
 */
// DelegatingIntroductionInterceptor 是 Spring AOP 中最常用的**引介（Introduction）**实现类。它通过“委派”模式，允许开发者轻松地为现有对象动态添加新的接口实现。
// 实现 Mixin（混入）：它能将一个对象的功能“混入”到另一个对象中。例如，你可以让所有的 Service 类都动态实现一个 Auditable 接口，而无需修改源代码。
// 方法分发（Dispatcher）：它充当一个交通警察的角色。当代理对象的方法被调用时，它判断该方法是属于“原始对象”的，还是属于“新引入接口”的。如果是后者，它就将调用转发给内部的 delegate 对象。
// 简化引介开发：开发者只需继承此类并实现目标接口，或者直接传入一个实现类对象，剩下的接口注册和调用分发逻辑都由该类自动完成。
@SuppressWarnings("serial")
public class DelegatingIntroductionInterceptor extends IntroductionInfoSupport
		implements IntroductionInterceptor {

	/**
	 * Object that actually implements the interfaces.
	 * May be "this" if a subclass implements the introduced interfaces.
	 */
	// 作用：真正实现那些“新引入接口”的实例对象。
	@Nullable
	private Object delegate;


	/**
	 * Construct a new DelegatingIntroductionInterceptor, providing
	 * a delegate that implements the interfaces to be introduced.
	 * @param delegate the delegate that implements the introduced interfaces
	 */
	public DelegatingIntroductionInterceptor(Object delegate) {
		init(delegate);
	}

	/**
	 * Construct a new DelegatingIntroductionInterceptor.
	 * The delegate will be the subclass, which must implement
	 * additional interfaces.
	 */
	protected DelegatingIntroductionInterceptor() {
		init(this);
	}


	/**
	 * Both constructors use this init method, as it is impossible to pass
	 * a "this" reference from one constructor to another.
	 * @param delegate the delegate object
	 */
	// 私有初始化核心逻辑。
	// 验证 delegate 非空并赋值。
	// 调用父类 implementInterfacesOnObject 自动扫描 delegate 实现的所有接口并注册。
	private void init(Object delegate) {
		Assert.notNull(delegate, "Delegate must not be null");
		this.delegate = delegate;
		implementInterfacesOnObject(delegate);

		// We don't want to expose the control interface
		// 安全性过滤：调用 suppressInterface 压制 IntroductionInterceptor 和 DynamicIntroductionAdvice。这是为了防止代理对象对外暴露这些 AOP 框架内部的控制接口。
		suppressInterface(IntroductionInterceptor.class);
		suppressInterface(DynamicIntroductionAdvice.class);
	}


	/**
	 * Subclasses may need to override this if they want to perform custom
	 * behaviour in around advice. However, subclasses should invoke this
	 * method, which handles introduced interfaces and forwarding to the target.
	 */
	// 作用：拦截代理对象的所有方法调用。
	@Override
	@Nullable
	public Object invoke(MethodInvocation mi) throws Throwable {
		if (isMethodOnIntroducedInterface(mi)) {
			// 分支一：引介方法：如果是新引入接口的方法，则使用 AopUtils.invokeJoinpointUsingReflection 直接在 delegate 对象上执行该方法
			// Using the following method rather than direct reflection, we
			// get correct handling of InvocationTargetException
			// if the introduced method throws an exception.
			Object retVal = AopUtils.invokeJoinpointUsingReflection(this.delegate, mi.getMethod(), mi.getArguments());

			// Massage return value if possible: if the delegate returned itself,
			// we really want to return the proxy.
			if (retVal == this.delegate && mi instanceof ProxyMethodInvocation pmi) {
				Object proxy = pmi.getProxy();
				if (mi.getMethod().getReturnType().isInstance(proxy)) {
					retVal = proxy;
				}
			}
			return retVal;
		}
		// 分支二：原始方法：如果不是引介方法，则调用 doProceed(mi)。
		return doProceed(mi);
	}

	/**
	 * Proceed with the supplied {@link org.aopalliance.intercept.MethodInterceptor}.
	 * Subclasses can override this method to intercept method invocations on the
	 * target object which is useful when an introduction needs to monitor the object
	 * that it is introduced into. This method is <strong>never</strong> called for
	 * {@link MethodInvocation MethodInvocations} on the introduced interfaces.
	 */
	@Nullable
	protected Object doProceed(MethodInvocation mi) throws Throwable {
		// If we get here, just pass the invocation on.
		return mi.proceed();
	}

}
