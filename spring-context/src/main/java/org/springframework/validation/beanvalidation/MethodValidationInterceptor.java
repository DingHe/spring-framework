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

package org.springframework.validation.beanvalidation;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.function.Supplier;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.executable.ExecutableValidator;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import org.springframework.aop.ProxyMethodInvocation;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.SmartFactoryBean;
import org.springframework.core.BridgeMethodResolver;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.function.SingletonSupplier;
import org.springframework.validation.annotation.Validated;

/**
 * An AOP Alliance {@link MethodInterceptor} implementation that delegates to a
 * JSR-303 provider for performing method-level validation on annotated methods.
 *
 * <p>Applicable methods have JSR-303 constraint annotations on their parameters
 * and/or on their return value (in the latter case specified at the method level,
 * typically as inline annotation).
 *
 * <p>E.g.: {@code public @NotNull Object myValidMethod(@NotNull String arg1, @Max(10) int arg2)}
 *
 * <p>Validation groups can be specified through Spring's {@link Validated} annotation
 * at the type level of the containing target class, applying to all public service methods
 * of that class. By default, JSR-303 will validate against its default group only.
 *
 * <p>As of Spring 5.0, this functionality requires a Bean Validation 1.1+ provider.
 *
 * @author Juergen Hoeller
 * @since 3.1
 * @see MethodValidationPostProcessor
 * @see jakarta.validation.executable.ExecutableValidator
 */
// MethodValidationInterceptor 是 Spring 框架中实现方法级别校验（Method-level Validation）的核心组件。
// 它将 Java Bean Validation（JSR-303/JSR-380，如 Hibernate Validator）与 Spring AOP 结合，使得我们可以在方法的参数或返回值上直接使用校验注解。
// 自动化校验：当被拦截的方法被调用时，自动触发参数校验。
// 返回值验证：在目标方法执行完毕后，自动验证其返回结果是否符合约束。
// 分组校验支持：支持通过 Spring 的 @Validated 注解指定校验分组（Groups）。
// 异常转换：如果校验失败，它会收集所有的违反约束信息（Constraint Violations）并抛出 ConstraintViolationException。
public class MethodValidationInterceptor implements MethodInterceptor {
	// 作用：持有并提供 JSR-303/JSR-380 的 Validator 实例。
	// 详细说明：这是一个 Supplier 接口，支持延迟初始化。它负责调用具体的校验引擎（如 Hibernate Validator）来执行实际的逻辑。
	private final Supplier<Validator> validator;


	/**
	 * Create a new MethodValidationInterceptor using a default JSR-303 validator underneath.
	 */
	public MethodValidationInterceptor() {
		this.validator = SingletonSupplier.of(() -> Validation.buildDefaultValidatorFactory().getValidator());
	}

	/**
	 * Create a new MethodValidationInterceptor using the given JSR-303 ValidatorFactory.
	 * @param validatorFactory the JSR-303 ValidatorFactory to use
	 */
	public MethodValidationInterceptor(ValidatorFactory validatorFactory) {
		this.validator = SingletonSupplier.of(validatorFactory::getValidator);
	}

	/**
	 * Create a new MethodValidationInterceptor using the given JSR-303 Validator.
	 * @param validator the JSR-303 Validator to use
	 */
	public MethodValidationInterceptor(Validator validator) {
		this.validator = () -> validator;
	}

	/**
	 * Create a new MethodValidationInterceptor for the supplied
	 * (potentially lazily initialized) Validator.
	 * @param validator a Supplier for the Validator to use
	 * @since 6.0
	 */
	public MethodValidationInterceptor(Supplier<Validator> validator) {
		this.validator = validator;
	}

	// 作用：拦截器的主入口，执行环绕通知逻辑。
	@Override
	@Nullable
	public Object invoke(MethodInvocation invocation) throws Throwable {
		// Avoid Validator invocation on FactoryBean.getObjectType/isSingleton
		// 判断当前调用的方法是否为 FactoryBean 的非业务方法（如 getObjectType）。
		// 如果是元数据方法，直接调用 invocation.proceed() 执行目标逻辑并返回，跳过后续的所有校验步骤
		if (isFactoryBeanMetadataMethod(invocation.getMethod())) {
			return invocation.proceed();
		}
		// 作用：解析当前方法应适用的校验组（Groups）。
		// 逻辑：查找类或方法上的 @Validated 注解，获取其 value 属性。这允许你在不同场景下应用不同的校验规则。
		Class<?>[] groups = determineValidationGroups(invocation);

		// Standard Bean Validation 1.1 API
		// execVal：从 JSR-303 Validator 中获取专门用于“执行体”（方法或构造函数）校验的 API。
		ExecutableValidator execVal = this.validator.get().forExecutables();
		// 获取当前准备校验的方法引用。
		Method methodToValidate = invocation.getMethod();
		Set<ConstraintViolation<Object>> result;
		// 通常是 getThis() 返回的目标对象。但在某些特殊 AOP 场景下（如没有目标实例的接口代理），则获取代理对象本身作为校验上下文。
		// 断言：确保 target 不为空，因为 JSR-303 校验需要知道是在哪个对象实例上进行的调用。
		Object target = invocation.getThis();
		if (target == null && invocation instanceof ProxyMethodInvocation methodInvocation) {
			// Allow validation for AOP proxy without a target
			target = methodInvocation.getProxy();
		}
		Assert.state(target != null, "Target must not be null");
		// 执行入参校验（核心步骤一）
		try {
			result = execVal.validateParameters(target, methodToValidate, invocation.getArguments(), groups);
		}
		catch (IllegalArgumentException ex) {
			// Probably a generic type mismatch between interface and impl as reported in SPR-12237 / HV-1011
			// Let's try to find the bridged method on the implementation class...
			methodToValidate = BridgeMethodResolver.findBridgedMethod(
					ClassUtils.getMostSpecificMethod(invocation.getMethod(), target.getClass()));
			result = execVal.validateParameters(target, methodToValidate, invocation.getArguments(), groups);
		}
		// 作用：如果校验结果集合不为空，说明参数不合法。
		if (!result.isEmpty()) {
			throw new ConstraintViolationException(result);
		}
		// 作用：在参数校验通过后，正式触发目标方法的业务逻辑。
		Object returnValue = invocation.proceed();
		// 作用：校验方法的返回结果是否符合约束（如方法头上的 @NotNull）。
		result = execVal.validateReturnValue(target, methodToValidate, returnValue, groups);
		if (!result.isEmpty()) {
			throw new ConstraintViolationException(result);
		}

		return returnValue;
	}
	// 核心逻辑是过滤。
	// 在 Spring 中，如果一个 Bean 实现了 FactoryBean 接口，Spring 容器会频繁调用它的元数据方法（如 getObjectType）
	// 为了性能和逻辑正确性，MethodValidationInterceptor 必须识别出这些方法并跳过校验（因为我们只想校验业务方法，不想校验 Spring 框架内部的查询方法）。

	private boolean isFactoryBeanMetadataMethod(Method method) {
		// 作用：获取当前正在被调用的方法是在哪个类或接口中定义的。
		Class<?> clazz = method.getDeclaringClass();

		// Call from interface-based proxy handle, allowing for an efficient check?
		// 判断声明类是否是接口。如果是 JDK 动态代理，调用的方法通常直接声明在接口上。
		// 检查这个接口是不是 Spring 的 FactoryBean 或其子接口 SmartFactoryBean。
		// 逻辑结论：如果是在 FactoryBean 接口上调用的非 getObject 方法，判定为元数据方法，返回 true。
		if (clazz.isInterface()) {
			return ((clazz == FactoryBean.class || clazz == SmartFactoryBean.class) &&
					!method.getName().equals("getObject"));
		}

		// Call from CGLIB proxy handle, potentially implementing a FactoryBean method?
		Class<?> factoryBeanType = null;
		// 判断 clazz 是否实现了 FactoryBean 或 SmartFactoryBean。
		if (SmartFactoryBean.class.isAssignableFrom(clazz)) {
			factoryBeanType = SmartFactoryBean.class;
		}
		else if (FactoryBean.class.isAssignableFrom(clazz)) {
			factoryBeanType = FactoryBean.class;
		}
		return (factoryBeanType != null && !method.getName().equals("getObject") &&
				ClassUtils.hasMethod(factoryBeanType, method));
	}

	/**
	 * Determine the validation groups to validate against for the given method invocation.
	 * <p>Default are the validation groups as specified in the {@link Validated} annotation
	 * on the method, or on the containing target class of the method, or for an AOP proxy
	 * without a target (with all behavior in advisors), also check on proxied interfaces.
	 * @param invocation the current MethodInvocation
	 * @return the applicable validation groups as a Class array
	 */
	protected Class<?>[] determineValidationGroups(MethodInvocation invocation) {
		Validated validatedAnn = AnnotationUtils.findAnnotation(invocation.getMethod(), Validated.class);
		if (validatedAnn == null) {
			Object target = invocation.getThis();
			if (target != null) {
				validatedAnn = AnnotationUtils.findAnnotation(target.getClass(), Validated.class);
			}
			else if (invocation instanceof ProxyMethodInvocation methodInvocation) {
				Object proxy = methodInvocation.getProxy();
				if (AopUtils.isAopProxy(proxy)) {
					for (Class<?> type : AopProxyUtils.proxiedUserInterfaces(proxy)) {
						validatedAnn = AnnotationUtils.findAnnotation(type, Validated.class);
						if (validatedAnn != null) {
							break;
						}
					}
				}
			}
		}
		return (validatedAnn != null ? validatedAnn.value() : new Class<?>[0]);
	}

}
