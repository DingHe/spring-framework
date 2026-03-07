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

package org.springframework.core.convert.converter;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * A converter converts a source object of type {@code S} to a target of type {@code T}.
 *
 * <p>Implementations of this interface are thread-safe and can be shared.
 *
 * <p>Implementations may additionally implement {@link ConditionalConverter}.
 *
 * @author Keith Donald
 * @author Josh Cummings
 * @since 3.0
 * @param <S> the source type
 * @param <T> the target type
 */
// 在 Spring 3.0 之后，Converter 接口被引入以提供一种强类型、线程安全的方式来处理数据转换：
// 原子转换逻辑：它定义了从一种类型 $S$（Source）到另一种类型 $T$（Target）的单一转换路径。
// 解耦：开发者只需实现简单的转换逻辑（如 String 转 User），Spring 的 ConversionService 会自动将其整合，实现链式调用（如 String -> Integer -> Enum）。
// 线程安全：接口设计要求实现类必须是线程安全的，因此一个 Converter 实例可以在整个 Spring 上下文中共享。
@FunctionalInterface
public interface Converter<S, T> {

	/**
	 * Convert the source object of type {@code S} to target type {@code T}.
	 * @param source the source object to convert, which must be an instance of {@code S} (never {@code null})
	 * @return the converted object, which must be an instance of {@code T} (potentially {@code null})
	 * @throws IllegalArgumentException if the source cannot be converted to the desired target type
	 */
	// 作用：执行具体的转换逻辑，将类型为 $S$ 的源对象转换为类型为 $T$ 的目标对象。
	@Nullable
	T convert(S source);

	/**
	 * Construct a composed {@link Converter} that first applies this {@link Converter}
	 * to its input, and then applies the {@code after} {@link Converter} to the
	 * result.
	 * @param after the {@link Converter} to apply after this {@link Converter}
	 * is applied
	 * @param <U> the type of output of both the {@code after} {@link Converter}
	 * and the composed {@link Converter}
	 * @return a composed {@link Converter} that first applies this {@link Converter}
	 * and then applies the {@code after} {@link Converter}
	 * @since 5.3
	 */
	// 作用：构建一个“组合转换器”。它类似于 Java 8 Function 接口的 andThen 方法。
	default <U> Converter<S, U> andThen(Converter<? super T, ? extends U> after) {
		Assert.notNull(after, "'after' Converter must not be null");
		return (S s) -> {
			T initialResult = convert(s);
			return (initialResult != null ? after.convert(initialResult) : null);
		};
	}

}
