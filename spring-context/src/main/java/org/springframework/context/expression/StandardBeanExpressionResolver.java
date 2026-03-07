/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.context.expression;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanExpressionException;
import org.springframework.beans.factory.config.BeanExpressionContext;
import org.springframework.beans.factory.config.BeanExpressionResolver;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.ParserContext;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.expression.spel.support.StandardTypeConverter;
import org.springframework.expression.spel.support.StandardTypeLocator;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Standard implementation of the
 * {@link org.springframework.beans.factory.config.BeanExpressionResolver}
 * interface, parsing and evaluating Spring EL using Spring's expression module.
 *
 * <p>All beans in the containing {@code BeanFactory} are made available as
 * predefined variables with their common bean name, including standard context
 * beans such as "environment", "systemProperties" and "systemEnvironment".
 *
 * @author Juergen Hoeller
 * @since 3.0
 * @see BeanExpressionContext#getBeanFactory()
 * @see org.springframework.expression.ExpressionParser
 * @see org.springframework.expression.spel.standard.SpelExpressionParser
 * @see org.springframework.expression.spel.support.StandardEvaluationContext
 */
// StandardBeanExpressionResolver 是 Spring 框架中 BeanExpressionResolver 接口的标准实现。它是使 Spring 能够处理 Bean 定义中 SpEL（Spring Expression Language） 表达式的核心类。
// 该类的主要职责是将包含在 #{...} 符号中的字符串解析并求值为具体的 Java 对象。
// SpEL 集成：它整合了 spring-expression 模块，将普通的 Bean 工厂转变为一个支持动态表达式的智能容器。
// 上下文链接：它将 BeanExpressionContext（包含 BeanFactory 和 Scope）转化为 SpEL 引擎能够理解的 StandardEvaluationContext。
// 性能优化：通过内部缓存机制，避免了对相同表达式字符串的重复解析，提高了运行效率。
// 资源访问：它使得表达式可以直接访问容器中的 Bean、系统属性、环境变量等。
public class StandardBeanExpressionResolver implements BeanExpressionResolver {

	/** Default expression prefix: "#{". */
	// 默认的表达式前缀。
	public static final String DEFAULT_EXPRESSION_PREFIX = "#{";

	/** Default expression suffix: "}". */
	// 默认的表达式后缀。
	public static final String DEFAULT_EXPRESSION_SUFFIX = "}";

	// 表达式的前缀，默认为 #{。
	private String expressionPrefix = DEFAULT_EXPRESSION_PREFIX;
	// 表达式的后缀，默认为 }。
	private String expressionSuffix = DEFAULT_EXPRESSION_SUFFIX;
	// 实际执行解析工作的 SpEL 解析器实例（SpelExpressionParser）
	private ExpressionParser expressionParser;
	// 解析后的 Expression 对象缓存。Key 是原始字符串，Value 是编译后的表达式对象。
	private final Map<String, Expression> expressionCache = new ConcurrentHashMap<>(256);
	// 针对不同上下文的 StandardEvaluationContext 缓存。
	private final Map<BeanExpressionContext, StandardEvaluationContext> evaluationCache = new ConcurrentHashMap<>(8);

	private final ParserContext beanExpressionParserContext = new ParserContext() {
		@Override
		public boolean isTemplate() {
			return true;
		}
		@Override
		public String getExpressionPrefix() {
			return expressionPrefix;
		}
		@Override
		public String getExpressionSuffix() {
			return expressionSuffix;
		}
	};


	/**
	 * Create a new {@code StandardBeanExpressionResolver} with default settings.
	 */
	public StandardBeanExpressionResolver() {
		this.expressionParser = new SpelExpressionParser();
	}

	/**
	 * Create a new {@code StandardBeanExpressionResolver} with the given bean class loader,
	 * using it as the basis for expression compilation.
	 * @param beanClassLoader the factory's bean class loader
	 */
	public StandardBeanExpressionResolver(@Nullable ClassLoader beanClassLoader) {
		this.expressionParser = new SpelExpressionParser(new SpelParserConfiguration(null, beanClassLoader));
	}


	/**
	 * Set the prefix that an expression string starts with.
	 * The default is "#{".
	 * @see #DEFAULT_EXPRESSION_PREFIX
	 */
	public void setExpressionPrefix(String expressionPrefix) {
		Assert.hasText(expressionPrefix, "Expression prefix must not be empty");
		this.expressionPrefix = expressionPrefix;
	}

	/**
	 * Set the suffix that an expression string ends with.
	 * The default is "}".
	 * @see #DEFAULT_EXPRESSION_SUFFIX
	 */
	public void setExpressionSuffix(String expressionSuffix) {
		Assert.hasText(expressionSuffix, "Expression suffix must not be empty");
		this.expressionSuffix = expressionSuffix;
	}

	/**
	 * Specify the EL parser to use for expression parsing.
	 * <p>Default is a {@link org.springframework.expression.spel.standard.SpelExpressionParser},
	 * compatible with standard Unified EL style expression syntax.
	 */
	public void setExpressionParser(ExpressionParser expressionParser) {
		Assert.notNull(expressionParser, "ExpressionParser must not be null");
		this.expressionParser = expressionParser;
	}


	@Override
	@Nullable
	public Object evaluate(@Nullable String value, BeanExpressionContext beanExpressionContext) throws BeansException {
		if (!StringUtils.hasLength(value)) {
			return value;
		}
		try {
			Expression expr = this.expressionCache.get(value);
			if (expr == null) {
				expr = this.expressionParser.parseExpression(value, this.beanExpressionParserContext);
				this.expressionCache.put(value, expr);
			}
			StandardEvaluationContext sec = this.evaluationCache.get(beanExpressionContext);
			if (sec == null) {
				sec = new StandardEvaluationContext(beanExpressionContext);
				sec.addPropertyAccessor(new BeanExpressionContextAccessor());
				sec.addPropertyAccessor(new BeanFactoryAccessor());
				sec.addPropertyAccessor(new MapAccessor());
				sec.addPropertyAccessor(new EnvironmentAccessor());
				sec.setBeanResolver(new BeanFactoryResolver(beanExpressionContext.getBeanFactory()));
				sec.setTypeLocator(new StandardTypeLocator(beanExpressionContext.getBeanFactory().getBeanClassLoader()));
				sec.setTypeConverter(new StandardTypeConverter(() -> {
					ConversionService cs = beanExpressionContext.getBeanFactory().getConversionService();
					return (cs != null ? cs : DefaultConversionService.getSharedInstance());
				}));
				customizeEvaluationContext(sec);
				this.evaluationCache.put(beanExpressionContext, sec);
			}
			return expr.getValue(sec);
		}
		catch (Throwable ex) {
			throw new BeanExpressionException("Expression parsing failed", ex);
		}
	}

	/**
	 * Template method for customizing the expression evaluation context.
	 * <p>The default implementation is empty.
	 */
	protected void customizeEvaluationContext(StandardEvaluationContext evalContext) {
	}

}
