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

package org.springframework.web.servlet;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.PropertyValues;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.EnvironmentCapable;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceEditor;
import org.springframework.core.io.ResourceLoader;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.context.support.ServletContextResourceLoader;
import org.springframework.web.context.support.StandardServletEnvironment;

/**
 * Simple extension of {@link jakarta.servlet.http.HttpServlet} which treats
 * its config parameters ({@code init-param} entries within the
 * {@code servlet} tag in {@code web.xml}) as bean properties.
 *
 * <p>A handy superclass for any type of servlet. Type conversion of config
 * parameters is automatic, with the corresponding setter method getting
 * invoked with the converted value. It is also possible for subclasses to
 * specify required properties. Parameters without matching bean property
 * setter will simply be ignored.
 *
 * <p>This servlet leaves request handling to subclasses, inheriting the default
 * behavior of HttpServlet ({@code doGet}, {@code doPost}, etc).
 *
 * <p>This generic servlet base class has no dependency on the Spring
 * {@link org.springframework.context.ApplicationContext} concept. Simple
 * servlets usually don't load their own context but rather access service
 * beans from the Spring root application context, accessible via the
 * filter's {@link #getServletContext() ServletContext} (see
 * {@link org.springframework.web.context.support.WebApplicationContextUtils}).
 *
 * <p>The {@link FrameworkServlet} class is a more specific servlet base
 * class which loads its own application context. FrameworkServlet serves
 * as direct base class of Spring's full-fledged {@link DispatcherServlet}.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see #addRequiredProperty
 * @see #initServletBean
 * @see #doGet
 * @see #doPost
 */
// 在 Spring MVC 的继承体系中，HttpServletBean 是连接 Servlet API 与 Spring Bean 容器 的重要基石。它是 DispatcherServlet 最顶层的父类。
// HttpServletBean 的核心职责是将传统的 Servlet 配置（web.xml 或 ServletConfig 中的 init-param）转化并注入到当前 Servlet 实例的属性中。
// 配置属性化：它利用 Spring 的 BeanWrapper 机制，将 Servlet 初始化参数当作 JavaBean 的属性来处理。例如，如果在 web.xml 中配置了参数 contextConfigLocation，它会自动调用当前类中的 setContextConfigLocation 方法。
// 类型转换：它支持自动类型转换。如果 Servlet 定义了一个 int 类型的属性，而初始化参数是字符串 "10"，它会自动完成转换。
// 环境感知：它实现了 EnvironmentAware，使得 Servlet 能够感知 Spring 的 Environment 环境抽象。
// 解耦初始化：它重写了标准 Servlet 的 init() 方法，在完成属性注入后提供了一个 initServletBean() 钩子方法供子类扩展，从而实现了“配置”与“业务初始化”的分离。
@SuppressWarnings("serial")
public abstract class HttpServletBean extends HttpServlet implements EnvironmentCapable, EnvironmentAware {

	/** Logger available to subclasses. */
	protected final Log logger = LogFactory.getLog(getClass());
	// 存储当前 Servlet 运行的环境配置信息（Profiles, Properties）。
	@Nullable
	private ConfigurableEnvironment environment;
	// 存储必须提供的初始化参数名集合。如果在初始化时缺失这些参数，将抛出异常。
	private final Set<String> requiredProperties = new HashSet<>(4);


	/**
	 * Subclasses can invoke this method to specify that this property
	 * (which must match a JavaBean property they expose) is mandatory,
	 * and must be supplied as a config parameter. This should be called
	 * from the constructor of a subclass.
	 * <p>This method is only relevant in case of traditional initialization
	 * driven by a ServletConfig instance.
	 * @param property name of the required property
	 */
	// 向 requiredProperties 集合中添加一个必需的属性名。
	protected final void addRequiredProperty(String property) {
		this.requiredProperties.add(property);
	}

	/**
	 * Set the {@code Environment} that this servlet runs in.
	 * <p>Any environment set here overrides the {@link StandardServletEnvironment}
	 * provided by default.
	 * @throws IllegalArgumentException if environment is not assignable to
	 * {@code ConfigurableEnvironment}
	 */
	// 作用：注入环境对象。
	@Override
	public void setEnvironment(Environment environment) {
		Assert.isInstanceOf(ConfigurableEnvironment.class, environment, "ConfigurableEnvironment required");
		this.environment = (ConfigurableEnvironment) environment;
	}

	/**
	 * Return the {@link Environment} associated with this servlet.
	 * <p>If none specified, a default environment will be initialized via
	 * {@link #createEnvironment()}.
	 */
	@Override
	public ConfigurableEnvironment getEnvironment() {
		if (this.environment == null) {
			this.environment = createEnvironment();
		}
		return this.environment;
	}

	/**
	 * Create and return a new {@link StandardServletEnvironment}.
	 * <p>Subclasses may override this in order to configure the environment or
	 * specialize the environment type returned.
	 */
	protected ConfigurableEnvironment createEnvironment() {
		return new StandardServletEnvironment();
	}

	/**
	 * Map config parameters onto bean properties of this servlet, and
	 * invoke subclass initialization.
	 * @throws ServletException if bean properties are invalid (or required
	 * properties are missing), or if subclass initialization fails.
	 */
	// 作用：这是 Servlet 容器调用的初始化入口。
	// init() 方法重写了 javax.servlet.GenericServlet 的生命周期方法。其核心作用是：
	// 属性注入（DI）：将 web.xml 或 Servlet 容器中的 init-param 配置，自动注入到当前 Servlet 实例的同名 Setter 方法中。
	// 类型转换：利用 Spring 的 BeanWrapper 将配置中的字符串（String）转换为 Java 对象的实际类型（如 Resource、Integer 等）。
	// 扩展点支持：在完成属性设置后，调用 initServletBean() 钩子方法，允许子类（如 DispatcherServlet）执行自定义初始化。
	@Override
	public final void init() throws ServletException {

		// Set bean properties from init parameters.
		// 作用：调用之前提到的内部类，从 ServletConfig 中抓取所有的初始化参数。
		PropertyValues pvs = new ServletConfigPropertyValues(getServletConfig(), this.requiredProperties);
		if (!pvs.isEmpty()) {
			try {
				// 为当前 Servlet 实例（this）创建一个 BeanWrapper。
				// BeanWrapper 的作用：它是 Spring 处理对象属性的核心组件，能够通过反射调用 Setter 方法，并处理复杂的嵌套属性。
				BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(this);
				// ServletContextResourceLoader：使 Servlet 能够识别以 classpath: 或 WEB-INF/ 开头的资源路径。
				ResourceLoader resourceLoader = new ServletContextResourceLoader(getServletContext());
				bw.registerCustomEditor(Resource.class, new ResourceEditor(resourceLoader, getEnvironment()));
				// 允许子类在注入前对 BeanWrapper 进行额外配置（如添加更多自定义编辑器）。
				// 注入属性
				initBeanWrapper(bw);
				// 将提取到的参数批量设置到当前对象中。
				bw.setPropertyValues(pvs, true);
			}
			catch (BeansException ex) {
				if (logger.isErrorEnabled()) {
					logger.error("Failed to set bean properties on servlet '" + getServletName() + "'", ex);
				}
				throw ex;
			}
		}

		// Let subclasses do whatever initialization they like.
		// 当属性注入全部完成后，此方法被触发。著名的 DispatcherServlet 就是通过重写这个方法来开启 Spring Web 容器（WebApplicationContext）的初始化流程。
		initServletBean();
	}

	/**
	 * Initialize the BeanWrapper for this HttpServletBean,
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
	 * All bean properties of this servlet will have been set before this
	 * method is invoked.
	 * <p>This default implementation is empty.
	 * @throws ServletException if subclass initialization fails
	 */
	protected void initServletBean() throws ServletException {
	}

	/**
	 * Overridden method that simply returns {@code null} when no
	 * ServletConfig set yet.
	 * @see #getServletConfig()
	 */
	@Override
	@Nullable
	public String getServletName() {
		return (getServletConfig() != null ? getServletConfig().getServletName() : null);
	}


	/**
	 * PropertyValues implementation created from ServletConfig init parameters.
	 */
	// ServletConfigPropertyValues 是 Spring Framework 中 HttpServletBean 的一个私有静态内部类。
	// 它继承自 MutablePropertyValues，专门负责将 Servlet 的初始化参数（init-param）转换为 Spring 可识别的属性值对象。
	// 配置桥接：它充当了标准 Servlet 规范（ServletConfig）与 Spring Bean 配置体系（PropertyValues）之间的桥梁。
	// 参数提取：自动遍历 web.xml 或注解中定义的所有 init-param，并将其封装为 PropertyValue。
	// 强制校验：支持“必填属性”校验。如果开发者指定了某些参数是必须的，但部署描述符中未提供，该类会直接抛出异常中断启动。
	private static class ServletConfigPropertyValues extends MutablePropertyValues {

		/**
		 * Create new ServletConfigPropertyValues.
		 * @param config the ServletConfig we'll use to take PropertyValues from
		 * @param requiredProperties set of property names we need, where
		 * we can't accept default values
		 * @throws ServletException if any required properties are missing
		 */
		// ServletConfig config: 代表当前 Servlet 的配置对象，用于获取 init-param。
		// Set<String> requiredProperties: 开发者声明的“必填属性名”集合。
		public ServletConfigPropertyValues(ServletConfig config, Set<String> requiredProperties)
				throws ServletException {
			// 初始化缺失追踪器
			Set<String> missingProps = (!CollectionUtils.isEmpty(requiredProperties) ?
					new HashSet<>(requiredProperties) : null);
			// 遍历并提取参数
			Enumeration<String> paramNames = config.getInitParameterNames();
			while (paramNames.hasMoreElements()) {
				String property = paramNames.nextElement();
				Object value = config.getInitParameter(property);
				addPropertyValue(new PropertyValue(property, value)); // 调用父类方法存入集合
				if (missingProps != null) {
					missingProps.remove(property); // 只要配置了，就从缺失名单中移除
				}
			}

			// Fail if we are still missing properties.
			// 如果遍历结束后 missingProps 依然不为空，说明有些预设的必填参数在 web.xml 或容器配置中没有找到。
			if (!CollectionUtils.isEmpty(missingProps)) {
				throw new ServletException(
						"Initialization from ServletConfig for servlet '" + config.getServletName() +
						"' failed; the following required properties were missing: " +
						StringUtils.collectionToDelimitedString(missingProps, ", "));
			}
		}
	}

}
