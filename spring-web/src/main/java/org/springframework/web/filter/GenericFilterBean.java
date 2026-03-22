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

package org.springframework.web.filter;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.core.env.EnvironmentCapable;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceEditor;
import org.springframework.core.io.ResourceLoader;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.context.support.ServletContextResourceLoader;
import org.springframework.web.context.support.StandardServletEnvironment;

/**
 * Simple base implementation of {@link jakarta.servlet.Filter} which treats
 * its config parameters ({@code init-param} entries within the
 * {@code filter} tag in {@code web.xml}) as bean properties.
 *
 * <p>A handy superclass for any type of filter. Type conversion of config
 * parameters is automatic, with the corresponding setter method getting
 * invoked with the converted value. It is also possible for subclasses to
 * specify required properties. Parameters without matching bean property
 * setter will simply be ignored.
 *
 * <p>This filter leaves actual filtering to subclasses, which have to
 * implement the {@link jakarta.servlet.Filter#doFilter} method.
 *
 * <p>This generic filter base class has no dependency on the Spring
 * {@link org.springframework.context.ApplicationContext} concept.
 * Filters usually don't load their own context but rather access service
 * beans from the Spring root application context, accessible via the
 * filter's {@link #getServletContext() ServletContext} (see
 * {@link org.springframework.web.context.support.WebApplicationContextUtils}).
 *
 * @author Juergen Hoeller
 * @since 06.12.2003
 * @see #addRequiredProperty
 * @see #initFilterBean
 * @see #doFilter
 */
// GenericFilterBean 是 Spring Web 模块中一个非常重要的基类。它将传统的 Servlet Filter 与 Spring 的 Bean 生命周期管理完美结合在一起。
// GenericFilterBean 的核心作用是将 Servlet 过滤器的配置参数（init-param）自动映射为该过滤器的 Bean 属性。
// 自动属性映射：它利用 Spring 的 BeanWrapper 机制，读取 web.xml 或 FilterConfig 中的初始化参数，并自动调用子类对应的 Setter 方法进行赋值。
// 弥合差异：它实现了多个 Spring 生命周期接口（如 InitializingBean, Aware 系列），使得一个普通的 Filter 可以像标准的 Spring Bean 一样感知容器环境。
// 简化开发：开发者只需继承此类并实现 doFilter 方法，无需手动编写冗长的 filterConfig.getInitParameter(...) 代码。
public abstract class GenericFilterBean implements Filter, BeanNameAware, EnvironmentAware,
		EnvironmentCapable, ServletContextAware, InitializingBean, DisposableBean {

	/** Logger available to subclasses. */
	protected final Log logger = LogFactory.getLog(getClass());
	// 存储该 Bean 在 Spring 容器中的名称（通过 BeanNameAware 获取）。
	@Nullable
	private String beanName;
	// 存储当前运行的环境配置，用于解析资源路径中的占位符。
	@Nullable
	private Environment environment;
	// 存储 Web 应用上下文对象。
	@Nullable
	private ServletContext servletContext;
	// 存储 Servlet 容器传入的原始过滤器配置对象。
	@Nullable
	private FilterConfig filterConfig;
	// 存储必须提供的属性名称集合。如果配置中缺少这些参数，初始化将报错。
	private final Set<String> requiredProperties = new HashSet<>(4);


	/**
	 * Stores the bean name as defined in the Spring bean factory.
	 * <p>Only relevant in case of initialization as bean, to have a name as
	 * fallback to the filter name usually provided by a FilterConfig instance.
	 * @see org.springframework.beans.factory.BeanNameAware
	 * @see #getFilterName()
	 */
	@Override
	public void setBeanName(String beanName) {
		this.beanName = beanName;
	}

	/**
	 * Set the {@code Environment} that this filter runs in.
	 * <p>Any environment set here overrides the {@link StandardServletEnvironment}
	 * provided by default.
	 * <p>This {@code Environment} object is used only for resolving placeholders in
	 * resource paths passed into init-parameters for this filter. If no init-params are
	 * used, this {@code Environment} can be essentially ignored.
	 */
	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	/**
	 * Return the {@link Environment} associated with this filter.
	 * <p>If none specified, a default environment will be initialized via
	 * {@link #createEnvironment()}.
	 * @since 4.3.9
	 */
	@Override
	public Environment getEnvironment() {
		if (this.environment == null) {
			this.environment = createEnvironment();
		}
		return this.environment;
	}

	/**
	 * Create and return a new {@link StandardServletEnvironment}.
	 * <p>Subclasses may override this in order to configure the environment or
	 * specialize the environment type returned.
	 * @since 4.3.9
	 */
	protected Environment createEnvironment() {
		return new StandardServletEnvironment();
	}

	/**
	 * Stores the ServletContext that the bean factory runs in.
	 * <p>Only relevant in case of initialization as bean, to have a ServletContext
	 * as fallback to the context usually provided by a FilterConfig instance.
	 * @see org.springframework.web.context.ServletContextAware
	 * @see #getServletContext()
	 */
	@Override
	public void setServletContext(ServletContext servletContext) {
		this.servletContext = servletContext;
	}

	/**
	 * Calls the {@code initFilterBean()} method that might
	 * contain custom initialization of a subclass.
	 * <p>Only relevant in case of initialization as bean, where the
	 * standard {@code init(FilterConfig)} method won't be called.
	 * @see #initFilterBean()
	 * @see #init(jakarta.servlet.FilterConfig)
	 */
	// 实现 InitializingBean 接口。在 Bean 属性设置完成后调用 initFilterBean()，确保初始化逻辑被触发。
	@Override
	public void afterPropertiesSet() throws ServletException {
		initFilterBean();
	}

	/**
	 * Subclasses may override this to perform custom filter shutdown.
	 * <p>Note: This method will be called from standard filter destruction
	 * as well as filter bean destruction in a Spring application context.
	 * <p>This default implementation is empty.
	 */
	@Override
	public void destroy() {
	}


	/**
	 * Subclasses can invoke this method to specify that this property
	 * (which must match a JavaBean property they expose) is mandatory,
	 * and must be supplied as a config parameter. This should be called
	 * from the constructor of a subclass.
	 * <p>This method is only relevant in case of traditional initialization
	 * driven by a FilterConfig instance.
	 * @param property name of the required property
	 */
	protected final void addRequiredProperty(String property) {
		this.requiredProperties.add(property);
	}

	/**
	 * Standard way of initializing this filter.
	 * Map config parameters onto bean properties of this filter, and
	 * invoke subclass initialization.
	 * @param filterConfig the configuration for this filter
	 * @throws ServletException if bean properties are invalid (or required
	 * properties are missing), or if subclass initialization fails.
	 * @see #initFilterBean
	 */
	// 连接 Servlet 标准接口 与 Spring 属性注入机制 的桥梁
	// 该方法实现了 jakarta.servlet.Filter 接口的初始化方法。它的核心任务是：
	// 捕获配置：接收来自 Servlet 容器（如 Tomcat）的配置信息。
	// 属性转化：将 web.xml 或配置类中定义的 init-param 参数转换成该类的成员变量属性。
	// 类型安全：支持复杂的类型转换（如将字符串路径转为 Resource 对象）。
	// 扩展开放：在完成 Spring 风格的初始化后，触发子类的自定义逻辑。
	@Override
	public final void init(FilterConfig filterConfig) throws ServletException {
		Assert.notNull(filterConfig, "FilterConfig must not be null");
		// 存原始配置
		this.filterConfig = filterConfig;

		// Set bean properties from init parameters.
		// 使用内部类 FilterConfigPropertyValues 将 filterConfig 中的所有键值对提取出来。
		PropertyValues pvs = new FilterConfigPropertyValues(filterConfig, this.requiredProperties);
		if (!pvs.isEmpty()) {
			try {
				// Spring 底层操作对象的工具，它可以利用反射调用 setXxx 方法。
				BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(this);
				// 资源加载器与环境准备
				ResourceLoader resourceLoader = new ServletContextResourceLoader(filterConfig.getServletContext());
				Environment env = this.environment;
				if (env == null) {
					env = new StandardServletEnvironment();
				}
				// 如果你在配置中写了 configFile = "classpath:config.xml"，Spring 会自动将其转换成 Resource 对象并注入给子类的 setConfigFile(Resource res) 方法
				bw.registerCustomEditor(Resource.class, new ResourceEditor(resourceLoader, env));
				// 触发子类初始化
				initBeanWrapper(bw);
				bw.setPropertyValues(pvs, true);
			}
			catch (BeansException ex) {
				String msg = "Failed to set bean properties on filter '" +
						filterConfig.getFilterName() + "': " + ex.getMessage();
				logger.error(msg, ex);
				throw new ServletException(msg, ex);
			}
		}

		// Let subclasses do whatever initialization they like.
		initFilterBean();

		if (logger.isDebugEnabled()) {
			logger.debug("Filter '" + filterConfig.getFilterName() + "' configured for use");
		}
	}

	/**
	 * Initialize the BeanWrapper for this GenericFilterBean,
	 * possibly with custom editors.
	 * <p>This default implementation is empty.
	 * @param bw the BeanWrapper to initialize
	 * @throws BeansException if thrown by BeanWrapper methods
	 * @see org.springframework.beans.BeanWrapper#registerCustomEditor
	 */
	protected void initBeanWrapper(BeanWrapper bw) throws BeansException {
	}

	/**
	 * Subclasses may override this to perform custom initialization.
	 * All bean properties of this filter will have been set before this
	 * method is invoked.
	 * <p>Note: This method will be called from standard filter initialization
	 * as well as filter bean initialization in a Spring application context.
	 * Filter name and ServletContext will be available in both cases.
	 * <p>This default implementation is empty.
	 * @throws ServletException if subclass initialization fails
	 * @see #getFilterName()
	 * @see #getServletContext()
	 */
	protected void initFilterBean() throws ServletException {
	}

	/**
	 * Make the FilterConfig of this filter available, if any.
	 * Analogous to GenericServlet's {@code getServletConfig()}.
	 * <p>Public to resemble the {@code getFilterConfig()} method
	 * of the Servlet Filter version that shipped with WebLogic 6.1.
	 * @return the FilterConfig instance, or {@code null} if none available
	 * @see jakarta.servlet.GenericServlet#getServletConfig()
	 */
	@Nullable
	public FilterConfig getFilterConfig() {
		return this.filterConfig;
	}

	/**
	 * Make the name of this filter available to subclasses.
	 * Analogous to GenericServlet's {@code getServletName()}.
	 * <p>Takes the FilterConfig's filter name by default.
	 * If initialized as bean in a Spring application context,
	 * it falls back to the bean name as defined in the bean factory.
	 * @return the filter name, or {@code null} if none available
	 * @see jakarta.servlet.GenericServlet#getServletName()
	 * @see jakarta.servlet.FilterConfig#getFilterName()
	 * @see #setBeanName
	 */
	@Nullable
	protected String getFilterName() {
		return (this.filterConfig != null ? this.filterConfig.getFilterName() : this.beanName);
	}

	/**
	 * Make the ServletContext of this filter available to subclasses.
	 * Analogous to GenericServlet's {@code getServletContext()}.
	 * <p>Takes the FilterConfig's ServletContext by default.
	 * If initialized as bean in a Spring application context,
	 * it falls back to the ServletContext that the bean factory runs in.
	 * @return the ServletContext instance
	 * @throws IllegalStateException if no ServletContext is available
	 * @see jakarta.servlet.GenericServlet#getServletContext()
	 * @see jakarta.servlet.FilterConfig#getServletContext()
	 * @see #setServletContext
	 */
	protected ServletContext getServletContext() {
		if (this.filterConfig != null) {
			return this.filterConfig.getServletContext();
		}
		else if (this.servletContext != null) {
			return this.servletContext;
		}
		else {
			throw new IllegalStateException("No ServletContext");
		}
	}


	/**
	 * PropertyValues implementation created from FilterConfig init parameters.
	 */
	// 主要任务是扮演一个“搬运工”和“校验员”，将 Servlet 容器提供的原始配置参数转换为 Spring 能够识别的属性值对象。
	// 这个类的核心逻辑在于：实现从 FilterConfig 到 PropertyValues 的转换。
	// 数据桥接：Servlet 标准使用 FilterConfig.getInitParameter() 获取字符串配置，而 Spring 属性注入使用 PropertyValue 对象。该类将两者打通。
	// 强制性校验：它不仅负责搬运数据，还负责检查开发者定义的“必填属性”是否在配置（如 web.xml 或 @WebFilter）中真实存在。如果缺失，它会直接阻止应用启动，确保系统安全。
	// 该类本身没有定义额外的成员变量，但它操作了两个关键的数据源：
	// config (FilterConfig)：来自 Servlet 容器的原始配置。
	// requiredProperties (Set)：子类预先声明的必须存在的属性名集合。
	@SuppressWarnings("serial")
	private static class FilterConfigPropertyValues extends MutablePropertyValues {

		/**
		 * Create new FilterConfigPropertyValues.
		 * @param config the FilterConfig we'll use to take PropertyValues from
		 * @param requiredProperties set of property names we need, where
		 * we can't accept default values
		 * @throws ServletException if any required properties are missing
		 */
		public FilterConfigPropertyValues(FilterConfig config, Set<String> requiredProperties)
				throws ServletException {
			// 如果子类指定了必填属性，就创建一个 missingProps 集合，并把所有必填项丢进去。
			Set<String> missingProps = (!CollectionUtils.isEmpty(requiredProperties) ?
					new HashSet<>(requiredProperties) : null);

			Enumeration<String> paramNames = config.getInitParameterNames();
			while (paramNames.hasMoreElements()) {
				String property = paramNames.nextElement();
				Object value = config.getInitParameter(property);
				// 核心：创建 PropertyValue 并添加到父类容器中
				addPropertyValue(new PropertyValue(property, value));
				if (missingProps != null) {
					missingProps.remove(property);
				}
			}

			// Fail if we are still missing properties.
			// 遍历结束后，如果 missingProps 集合还不为空，说明有的必填项没在配置中找到。
			if (!CollectionUtils.isEmpty(missingProps)) {
				throw new ServletException(
						"Initialization from FilterConfig for filter '" + config.getFilterName() +
						"' failed; the following required properties were missing: " +
						StringUtils.collectionToDelimitedString(missingProps, ", "));
			}
		}
	}

}
