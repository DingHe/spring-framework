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

package org.springframework.beans.factory.config;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Holder for a BeanDefinition with name and aliases.
 * Can be registered as a placeholder for an inner bean.
 *
 * <p>Can also be used for programmatic registration of inner bean
 * definitions. If you don't care about BeanNameAware and the like,
 * registering RootBeanDefinition or ChildBeanDefinition is good enough.
 *
 * @author Juergen Hoeller
 * @since 1.0.2
 * @see org.springframework.beans.factory.BeanNameAware
 * @see org.springframework.beans.factory.support.RootBeanDefinition
 * @see org.springframework.beans.factory.support.ChildBeanDefinition
 */
// BeanDefinitionHolder 是 Spring 框架中一个简单但非常实用的包装类（Wrapper）。它将 Bean 的“图纸”与它的“身份信息”绑定在了一起。
// 在 Spring 容器中，BeanDefinition 只负责描述 Bean 如何创建（类名、属性等），它本身通常不包含该 Bean 在容器中的具体名称（Bean Name）或别名（Aliases）。
// BeanDefinitionHolder 的核心作用是：将 Bean 的定义（BeanDefinition）与它的唯一名称和别名数组封装成一个整体
// 内部 Bean（Inner Beans）的占位符：当你在 XML 或配置中定义一个嵌套在其他 Bean 内部的 Bean 时，Spring 会用 BeanDefinitionHolder 来表示它。。
// 传递上下文：在解析配置文件的过程中，Holder 作为一个携带完整信息的对象在各个解析器（Parser）和装饰器（Decorator）之间传递。
public class BeanDefinitionHolder implements BeanMetadataElement {
	// 被封装的核心 Bean 定义对象。包含了 Bean 的配置信息（Class, Scope, PropertyValues 等）。
	private final BeanDefinition beanDefinition;
	// Bean 的主要名称（ID）。在容器中必须是唯一的。
	private final String beanName;
	// 可选属性。存储该 Bean 的所有别名。如果没有别名，则为 null。
	@Nullable
	private final String[] aliases;


	/**
	 * Create a new BeanDefinitionHolder.
	 * @param beanDefinition the BeanDefinition to wrap
	 * @param beanName the name of the bean, as specified for the bean definition
	 */
	public BeanDefinitionHolder(BeanDefinition beanDefinition, String beanName) {
		this(beanDefinition, beanName, null);
	}

	/**
	 * Create a new BeanDefinitionHolder.
	 * @param beanDefinition the BeanDefinition to wrap
	 * @param beanName the name of the bean, as specified for the bean definition
	 * @param aliases alias names for the bean, or {@code null} if none
	 */
	public BeanDefinitionHolder(BeanDefinition beanDefinition, String beanName, @Nullable String[] aliases) {
		Assert.notNull(beanDefinition, "BeanDefinition must not be null");
		Assert.notNull(beanName, "Bean name must not be null");
		this.beanDefinition = beanDefinition;
		this.beanName = beanName;
		this.aliases = aliases;
	}

	/**
	 * Copy constructor: Create a new BeanDefinitionHolder with the
	 * same contents as the given BeanDefinitionHolder instance.
	 * <p>Note: The wrapped BeanDefinition reference is taken as-is;
	 * it is {@code not} deeply copied.
	 * @param beanDefinitionHolder the BeanDefinitionHolder to copy
	 */
	public BeanDefinitionHolder(BeanDefinitionHolder beanDefinitionHolder) {
		Assert.notNull(beanDefinitionHolder, "BeanDefinitionHolder must not be null");
		this.beanDefinition = beanDefinitionHolder.getBeanDefinition();
		this.beanName = beanDefinitionHolder.getBeanName();
		this.aliases = beanDefinitionHolder.getAliases();
	}


	/**
	 * Return the wrapped BeanDefinition.
	 */
	public BeanDefinition getBeanDefinition() {
		return this.beanDefinition;
	}

	/**
	 * Return the primary name of the bean, as specified for the bean definition.
	 */
	public String getBeanName() {
		return this.beanName;
	}

	/**
	 * Return the alias names for the bean, as specified directly for the bean definition.
	 * @return the array of alias names, or {@code null} if none
	 */
	@Nullable
	public String[] getAliases() {
		return this.aliases;
	}

	/**
	 * Expose the bean definition's source object.
	 * @see BeanDefinition#getSource()
	 */
	@Override
	@Nullable
	public Object getSource() {
		return this.beanDefinition.getSource();
	}

	/**
	 * Determine whether the given candidate name matches the bean name
	 * or the aliases stored in this bean definition.
	 */
	public boolean matchesName(@Nullable String candidateName) {
		return (candidateName != null && (candidateName.equals(this.beanName) ||
				candidateName.equals(BeanFactoryUtils.transformedBeanName(this.beanName)) ||
				ObjectUtils.containsElement(this.aliases, candidateName)));
	}


	/**
	 * Return a friendly, short description for the bean, stating name and aliases.
	 * @see #getBeanName()
	 * @see #getAliases()
	 */
	public String getShortDescription() {
		if (this.aliases == null) {
			return "Bean definition with name '" + this.beanName + "'";
		}
		return "Bean definition with name '" + this.beanName + "' and aliases [" + StringUtils.arrayToCommaDelimitedString(this.aliases) + ']';
	}

	/**
	 * Return a long description for the bean, including name and aliases
	 * as well as a description of the contained {@link BeanDefinition}.
	 * @see #getShortDescription()
	 * @see #getBeanDefinition()
	 */
	public String getLongDescription() {
		return getShortDescription() + ": " + this.beanDefinition;
	}

	/**
	 * This implementation returns the long description. Can be overridden
	 * to return the short description or any kind of custom description instead.
	 * @see #getLongDescription()
	 * @see #getShortDescription()
	 */
	@Override
	public String toString() {
		return getLongDescription();
	}


	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof BeanDefinitionHolder that &&
				this.beanDefinition.equals(that.beanDefinition) &&
				this.beanName.equals(that.beanName) &&
				ObjectUtils.nullSafeEquals(this.aliases, that.aliases)));
	}

	@Override
	public int hashCode() {
		int hashCode = this.beanDefinition.hashCode();
		hashCode = 29 * hashCode + this.beanName.hashCode();
		hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(this.aliases);
		return hashCode;
	}

}
