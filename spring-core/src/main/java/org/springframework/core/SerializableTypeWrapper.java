/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.core;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;

import org.springframework.lang.Nullable;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;

/**
 * Internal utility class that can be used to obtain wrapped {@link Serializable}
 * variants of {@link java.lang.reflect.Type java.lang.reflect.Types}.
 *
 * <p>{@link #forField(Field) Fields} or {@link #forMethodParameter(MethodParameter)
 * MethodParameters} can be used as the root source for a serializable type.
 * Alternatively, a regular {@link Class} can also be used as source.
 *
 * <p>The returned type will either be a {@link Class} or a serializable proxy of
 * {@link GenericArrayType}, {@link ParameterizedType}, {@link TypeVariable} or
 * {@link WildcardType}. With the exception of {@link Class} (which is final) calls
 * to methods that return further {@link Type Types} (for example
 * {@link GenericArrayType#getGenericComponentType()}) will be automatically wrapped.
 *
 * @author Phillip Webb
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 4.0
 */
final class SerializableTypeWrapper {

	private static final Class<?>[] SUPPORTED_SERIALIZABLE_TYPES = {
			GenericArrayType.class, ParameterizedType.class, TypeVariable.class, WildcardType.class};

	static final ConcurrentReferenceHashMap<Type, Type> cache = new ConcurrentReferenceHashMap<>(256);


	private SerializableTypeWrapper() {
	}


	/**
	 * Return a {@link Serializable} variant of {@link Field#getGenericType()}.
	 */
	@Nullable
	public static Type forField(Field field) {
		return forTypeProvider(new FieldTypeProvider(field));
	}

	/**
	 * Return a {@link Serializable} variant of
	 * {@link MethodParameter#getGenericParameterType()}.
	 */
	@Nullable
	public static Type forMethodParameter(MethodParameter methodParameter) {
		return forTypeProvider(new MethodParameterTypeProvider(methodParameter));
	}

	/**
	 * Unwrap the given type, effectively returning the original non-serializable type.
	 * @param type the type to unwrap
	 * @return the original non-serializable type
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Type> T unwrap(T type) {
		Type unwrapped = null;
		if (type instanceof SerializableTypeProxy proxy) {
			unwrapped = proxy.getTypeProvider().getType();
		}
		return (unwrapped != null ? (T) unwrapped : type);
	}

	/**
	 * Return a {@link Serializable} {@link Type} backed by a {@link TypeProvider} .
	 * <p>If type artifacts are generally not serializable in the current runtime
	 * environment, this delegate will simply return the original {@code Type} as-is.
	 */
	@Nullable
	static Type forTypeProvider(TypeProvider provider) {
		Type providedType = provider.getType();
		if (providedType == null || providedType instanceof Serializable) {
			// No serializable type wrapping necessary (e.g. for java.lang.Class)
			return providedType;
		}
		if (NativeDetector.inNativeImage() || !Serializable.class.isAssignableFrom(Class.class)) {
			// Let's skip any wrapping attempts if types are generally not serializable in
			// the current runtime environment (even java.lang.Class itself, e.g. on GraalVM native images)
			return providedType;
		}

		// Obtain a serializable type proxy for the given provider...
		Type cached = cache.get(providedType);
		if (cached != null) {
			return cached;
		}
		for (Class<?> type : SUPPORTED_SERIALIZABLE_TYPES) {
			if (type.isInstance(providedType)) {
				ClassLoader classLoader = provider.getClass().getClassLoader();
				Class<?>[] interfaces = new Class<?>[] {type, SerializableTypeProxy.class, Serializable.class};
				InvocationHandler handler = new TypeProxyInvocationHandler(provider);
				cached = (Type) Proxy.newProxyInstance(classLoader, interfaces, handler);
				cache.put(providedType, cached);
				return cached;
			}
		}
		throw new IllegalArgumentException("Unsupported Type class: " + providedType.getClass().getName());
	}


	/**
	 * Additional interface implemented by the type proxy.
	 */
	interface SerializableTypeProxy {

		/**
		 * Return the underlying type provider.
		 */
		TypeProvider getTypeProvider();
	}


	/**
	 * A {@link Serializable} interface providing access to a {@link Type}.
	 */
	// 在 Java 中，标准的 java.lang.reflect.Type（及其子类如 ParameterizedType）通常是不可序列化的。
	// 这在分布式环境或需要将 BeanDefinition 存储到磁盘的 Spring 应用中会产生问题。
	// TypeProvider 的核心作用包括：
	// 屏蔽不可序列化细节：通过实现此接口，Spring 可以将一个 Type 包装起来。在序列化时，只序列化 TypeProvider 的元数据（如字段名、方法名）；
	// 在反序列化后，再通过 getType() 重新获取 Type 对象。
	// 延迟加载（Lazy Loading）：只有在真正需要解析类型时，才调用 getType() 从原始源（如 Field 或 MethodParameter）中提取类型信息。
	// 提供溯源信息：通过 getSource() 告知开发者这个类型是从哪里来的（是哪个类的哪个字段，或者是哪个方法的参数）。
	@SuppressWarnings("serial")
	interface TypeProvider extends Serializable {

		/**
		 * Return the (possibly non {@link Serializable}) {@link Type}.
		 */
		// 作用：返回该提供者所持有的底层 java.lang.reflect.Type 对象。
		// 返回类型：可能是 Class、ParameterizedType（带泛型的类型）、GenericArrayType（泛型数组）等。
		@Nullable
		Type getType();

		/**
		 * Return the source of the type, or {@code null} if not known.
		 * <p>The default implementation returns {@code null}.
		 */
		// 作用：返回该类型的“源头”对象。
		// 如果类型来自类成员变量，返回 java.lang.reflect.Field。
		// 如果类型来自方法参数，返回 org.springframework.core.MethodParameter。
		@Nullable
		default Object getSource() {
			return null;
		}
	}


	/**
	 * {@link Serializable} {@link InvocationHandler} used by the proxied {@link Type}.
	 * Provides serialization support and enhances any methods that return {@code Type}
	 * or {@code Type[]}.
	 */
	@SuppressWarnings("serial")
	private static class TypeProxyInvocationHandler implements InvocationHandler, Serializable {

		private final TypeProvider provider;

		public TypeProxyInvocationHandler(TypeProvider provider) {
			this.provider = provider;
		}

		@Override
		@Nullable
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			switch (method.getName()) {
				case "equals":
					Object other = args[0];
					// Unwrap proxies for speed
					if (other instanceof Type otherType) {
						other = unwrap(otherType);
					}
					return ObjectUtils.nullSafeEquals(this.provider.getType(), other);
				case "hashCode":
					return ObjectUtils.nullSafeHashCode(this.provider.getType());
				case "getTypeProvider":
					return this.provider;
			}

			if (Type.class == method.getReturnType() && ObjectUtils.isEmpty(args)) {
				return forTypeProvider(new MethodInvokeTypeProvider(this.provider, method, -1));
			}
			else if (Type[].class == method.getReturnType() && ObjectUtils.isEmpty(args)) {
				Object returnValue = ReflectionUtils.invokeMethod(method, this.provider.getType());
				if (returnValue == null) {
					return null;
				}
				Type[] result = new Type[((Type[]) returnValue).length];
				for (int i = 0; i < result.length; i++) {
					result[i] = forTypeProvider(new MethodInvokeTypeProvider(this.provider, method, i));
				}
				return result;
			}

			Type type = this.provider.getType();
			if (type instanceof TypeVariable<?> tv && method.getName().equals("getName")) {
				// Avoid reflection for common comparison of type variables
				return tv.getName();
			}
			return ReflectionUtils.invokeMethod(method, type, args);
		}
	}


	/**
	 * {@link TypeProvider} for {@link Type Types} obtained from a {@link Field}.
	 */
	// FieldTypeProvider 的核心任务是：将一个不可序列化的 java.lang.reflect.Field 包装成一个可序列化的对象。
	// FieldTypeProvider 通过记录字段的“坐标”（类名 + 字段名），在反序列化时动态地把 Field 找回来，从而保证了 ResolvableType 在网络传输或持久化后的可用性。
	@SuppressWarnings("serial")
	static class FieldTypeProvider implements TypeProvider {

		private final String fieldName;

		private final Class<?> declaringClass;

		private transient Field field;
		// 接收一个原始的 Field 对象。
		// 关键逻辑：它不仅保存了 field 本身，还提取并保存了 fieldName 和 declaringClass。这两个元数据是可序列化的，作为未来重建 field 的“存根”。
		public FieldTypeProvider(Field field) {
			this.fieldName = field.getName();
			this.declaringClass = field.getDeclaringClass();
			this.field = field;
		}

		@Override
		public Type getType() {
			return this.field.getGenericType();
		}

		@Override
		public Object getSource() {
			return this.field;
		}

		private void readObject(ObjectInputStream inputStream) throws IOException, ClassNotFoundException {
			inputStream.defaultReadObject();
			try {
				this.field = this.declaringClass.getDeclaredField(this.fieldName);
			}
			catch (Throwable ex) {
				throw new IllegalStateException("Could not find original class structure", ex);
			}
		}
	}


	/**
	 * {@link TypeProvider} for {@link Type Types} obtained from a {@link MethodParameter}.
	 */
	// MethodParameterTypeProvider 的主要职责是：实现对 MethodParameter 的可序列化包装。
	// 由于 MethodParameter 内部持有 Method 或 Constructor 对象的引用（这些反射对象同样不可序列化），该类通过记录方法的“签名信息”，确保在反序列化后能够精准地重新定位并重建该参数对象。
	@SuppressWarnings("serial")
	static class MethodParameterTypeProvider implements TypeProvider {

		@Nullable
		private final String methodName;

		private final Class<?>[] parameterTypes;

		private final Class<?> declaringClass;

		private final int parameterIndex;

		private transient MethodParameter methodParameter;

		public MethodParameterTypeProvider(MethodParameter methodParameter) {
			this.methodName = (methodParameter.getMethod() != null ? methodParameter.getMethod().getName() : null);
			this.parameterTypes = methodParameter.getExecutable().getParameterTypes();
			this.declaringClass = methodParameter.getDeclaringClass();
			this.parameterIndex = methodParameter.getParameterIndex();
			this.methodParameter = methodParameter;
		}

		@Override
		public Type getType() {
			return this.methodParameter.getGenericParameterType();
		}

		@Override
		public Object getSource() {
			return this.methodParameter;
		}

		private void readObject(ObjectInputStream inputStream) throws IOException, ClassNotFoundException {
			inputStream.defaultReadObject();
			try {
				if (this.methodName != null) {
					this.methodParameter = new MethodParameter(
							this.declaringClass.getDeclaredMethod(this.methodName, this.parameterTypes), this.parameterIndex);
				}
				else {
					this.methodParameter = new MethodParameter(
							this.declaringClass.getDeclaredConstructor(this.parameterTypes), this.parameterIndex);
				}
			}
			catch (Throwable ex) {
				throw new IllegalStateException("Could not find original class structure", ex);
			}
		}
	}


	/**
	 * {@link TypeProvider} for {@link Type Types} obtained by invoking a no-arg method.
	 */
	// MethodInvokeTypeProvider 的作用是：通过执行一个“无参方法”来动态获取类型信息。
	// 在处理复杂的泛型时（例如 WildcardType 的上边界 getUpperBounds() 或 ParameterizedType 的参数列表 getActualTypeArguments()），这些信息本身就是通过调用 JDK 的 Type 接口方法获得的。
	// 该类通过包装一个已有的 TypeProvider（作为调用目标）和一个 Method（要执行的操作），实现了对泛型内部组件的可序列化追踪。
	@SuppressWarnings("serial")
	static class MethodInvokeTypeProvider implements TypeProvider {

		private final TypeProvider provider;

		private final String methodName;

		private final Class<?> declaringClass;

		private final int index;

		private transient Method method;

		@Nullable
		private transient volatile Object result;

		public MethodInvokeTypeProvider(TypeProvider provider, Method method, int index) {
			this.provider = provider;
			this.methodName = method.getName();
			this.declaringClass = method.getDeclaringClass();
			this.index = index;
			this.method = method;
		}

		@Override
		@Nullable
		public Type getType() {
			Object result = this.result;
			if (result == null) {
				// Lazy invocation of the target method on the provided type
				result = ReflectionUtils.invokeMethod(this.method, this.provider.getType());
				// Cache the result for further calls to getType()
				this.result = result;
			}
			return (result instanceof Type[] results ? results[this.index] : (Type) result);
		}

		@Override
		@Nullable
		public Object getSource() {
			return null;
		}

		private void readObject(ObjectInputStream inputStream) throws IOException, ClassNotFoundException {
			inputStream.defaultReadObject();
			Method method = ReflectionUtils.findMethod(this.declaringClass, this.methodName);
			if (method == null) {
				throw new IllegalStateException("Cannot find method on deserialization: " + this.methodName);
			}
			if (method.getReturnType() != Type.class && method.getReturnType() != Type[].class) {
				throw new IllegalStateException(
						"Invalid return type on deserialized method - needs to be Type or Type[]: " + method);
			}
			this.method = method;
		}
	}

}
