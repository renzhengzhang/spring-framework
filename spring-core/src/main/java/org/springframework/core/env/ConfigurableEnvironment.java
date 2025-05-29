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

package org.springframework.core.env;

import java.util.Map;

/**
 * Configuration interface to be implemented by most if not all {@link Environment} types.
 * Provides facilities for setting active and default profiles and manipulating underlying
 * property sources. Allows clients to set and validate required properties, customize the
 * conversion service and more through the {@link ConfigurablePropertyResolver}
 * superinterface.
 *
 * <h2>Manipulating property sources</h2>
 * <p>Property sources may be removed, reordered, or replaced; and additional
 * property sources may be added using the {@link MutablePropertySources}
 * instance returned from {@link #getPropertySources()}. The following examples
 * are against the {@link StandardEnvironment} implementation of
 * {@code ConfigurableEnvironment}, but are generally applicable to any implementation,
 * though particular default property sources may differ.
 *
 * <h4>Example: adding a new property source with highest search priority</h4>
 * <pre class="code">
 * ConfigurableEnvironment environment = new StandardEnvironment();
 * MutablePropertySources propertySources = environment.getPropertySources();
 * Map&lt;String, Object&gt; myMap = new HashMap&lt;&gt;();
 * myMap.put("xyz", "myValue");
 * propertySources.addFirst(new MapPropertySource("MY_MAP", myMap));
 * </pre>
 *
 * <h4>Example: removing the default system properties property source</h4>
 * <pre class="code">
 * MutablePropertySources propertySources = environment.getPropertySources();
 * propertySources.remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME)
 * </pre>
 *
 * <h4>Example: mocking the system environment for testing purposes</h4>
 * <pre class="code">
 * MutablePropertySources propertySources = environment.getPropertySources();
 * MockPropertySource mockEnvVars = new MockPropertySource().withProperty("xyz", "myValue");
 * propertySources.replace(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, mockEnvVars);
 * </pre>
 *
 * When an {@link Environment} is being used by an {@code ApplicationContext}, it is
 * important that any such {@code PropertySource} manipulations be performed
 * <em>before</em> the context's {@link
 * org.springframework.context.support.AbstractApplicationContext#refresh() refresh()}
 * method is called. This ensures that all property sources are available during the
 * container bootstrap process, including use by {@linkplain
 * org.springframework.context.support.PropertySourcesPlaceholderConfigurer property
 * placeholder configurers}.
 *
 * <p>
 * 提供设置激活和默认 Profiles、操作底层 MutablePropertySources 的功能，
 * 并允许通过父类接口 ConfigurablePropertyResolver 来设置和验证 required properties、自定义 ConversionService 等等
 *
 * @author Chris Beams
 * @since 3.1
 * @see StandardEnvironment
 * @see org.springframework.context.ConfigurableApplicationContext#getEnvironment
 */
public interface ConfigurableEnvironment extends Environment, ConfigurablePropertyResolver {

	/**
	 * Specify the set of profiles active for this {@code Environment}. Profiles are
	 * evaluated during container bootstrap to determine whether bean definitions
	 * should be registered with the container.
	 * <p>Any existing active profiles will be replaced with the given arguments; call
	 * with zero arguments to clear the current set of active profiles. Use
	 * {@link #addActiveProfile} to add a profile while preserving the existing set.
	 *
	 * <p>
	 * Profile 管理 - 配置激活的 Profiles
	 * <ul>
	 *     <li>指定当前 Environment 激活的 Profiles 集合，决定哪些 BeanDefinition 应该注册到容器中</li>
	 * 	   <li>这个方法会替换现有所有激活的 Profiles，传入空参数可清除当前激活的 Profiles</li>
	 * </ul>
	 *
	 * @throws IllegalArgumentException if any profile is null, empty or whitespace-only
	 * @see #addActiveProfile
	 * @see #setDefaultProfiles
	 * @see org.springframework.context.annotation.Profile
	 * @see AbstractEnvironment#ACTIVE_PROFILES_PROPERTY_NAME
	 */
	void setActiveProfiles(String... profiles);

	/**
	 * Add a profile to the current set of active profiles.
	 * @throws IllegalArgumentException if the profile is null, empty or whitespace-only
	 *
	 * <p>
	 * Profile 管理 - 添加激活的 Profile
	 * <p>
	 * 向当前激活的 Profiles 中添加一个配置文件，与 {@link ConfigurableEnvironment#setActiveProfiles} 不同，
	 * 这个方法会保留现有激活的 Profile
	 *
	 * @see #setActiveProfiles
	 */
	void addActiveProfile(String profile);

	/**
	 * Specify the set of profiles to be made active by default if no other profiles
	 * are explicitly made active through {@link #setActiveProfiles}.
	 * @throws IllegalArgumentException if any profile is null, empty or whitespace-only
	 * @see AbstractEnvironment#DEFAULT_PROFILES_PROPERTY_NAME
	 *
	 * <p>
	 * Profile 管理 - 配置默认的 Profiles
	 * <ul>
	 *     <li>指定在没有显式激活 Profiles 时默认激活的 Profiles</li>
	 *     <li>当没有通过 {@link ConfigurableEnvironment#setActiveProfiles} 显式激活 Profiles 时，这些默认 Profiles 会自动激活</li>
	 * </ul>
	 */
	void setDefaultProfiles(String... profiles);

	/**
	 * Return the {@link PropertySources} for this {@code Environment} in mutable form,
	 * allowing for manipulation of the set of {@link PropertySource} objects that should
	 * be searched when resolving properties against this {@code Environment} object.
	 * The various {@link MutablePropertySources} methods such as
	 * {@link MutablePropertySources#addFirst addFirst},
	 * {@link MutablePropertySources#addLast addLast},
	 * {@link MutablePropertySources#addBefore addBefore} and
	 * {@link MutablePropertySources#addAfter addAfter} allow for fine-grained control
	 * over property source ordering. This is useful, for example, in ensuring that
	 * certain user-defined property sources have search precedence over default property
	 * sources such as the set of system properties or the set of system environment
	 * variables.
	 *
	 * <p>
	 * PropertySources 管理 - 获取 MutablePropertySources，允许配置在 resolve property 时应该搜索的 PropertySource 集合，
	 * 并提供细粒度的 PropertySources 排序控制，包括：
	 * <ul>
	 *     <li>{@link MutablePropertySources#addFirst}：添加到最高优先级</li>
	 *     <li>{@link MutablePropertySources#addLast}：添加到最低优先级</li>
	 *     <li>{@link MutablePropertySources#addBefore}：在指定 PropertySource 之前添加</li>
	 *     <li>{@link MutablePropertySources#addAfter}：在指定 PropertySource 之后添加</li>
	 * </ul>
	 *
	 * @see AbstractEnvironment#customizePropertySources
	 */
	MutablePropertySources getPropertySources();

	/**
	 * Return the value of {@link System#getProperties()}.
	 * <p>Note that most {@code Environment} implementations will include this system
	 * properties map as a default {@link PropertySource} to be searched. Therefore, it is
	 * recommended that this method not be used directly unless bypassing other property
	 * sources is expressly intended.
	 *
	 * <p>
	 * PropertySources 管理 - 获取 System Properties，例如 java.class.path、user.dir
	 * <p>
	 * 注意：大多数 Environment 实现会将 System Properties 作为默认 PropertySource 包含在内
	 * 因此，除非明确要绕过其他 PropertySource，建议不要直接使用此方法
	 *
	 */
	Map<String, Object> getSystemProperties();

	/**
	 * Return the value of {@link System#getenv()}.
	 * <p>Note that most {@link Environment} implementations will include this system
	 * environment map as a default {@link PropertySource} to be searched. Therefore, it
	 * is recommended that this method not be used directly unless bypassing other
	 * property sources is expressly intended.
	 *
	 * <p>
	 * PropertySources 管理 - 获取系统环境变量
	 * <p>
	 * 注意：大多数 Environment 实现会将 System Environment 作为默认 PropertySource 包含在内
	 * 因此，除非明确要绕过其他 PropertySource，建议不要直接使用此方法
	 *
	 */
	Map<String, Object> getSystemEnvironment();

	/**
	 * Append the given parent environment's active profiles, default profiles and
	 * property sources to this (child) environment's respective collections of each.
	 * <p>For any identically-named {@code PropertySource} instance existing in both
	 * parent and child, the child instance is to be preserved and the parent instance
	 * discarded. This has the effect of allowing overriding of property sources by the
	 * child as well as avoiding redundant searches through common property source types,
	 * e.g. system environment and system properties.
	 * <p>Active and default profile names are also filtered for duplicates, to avoid
	 * confusion and redundant storage.
	 * <p>The parent environment remains unmodified in any case. Note that any changes to
	 * the parent environment occurring after the call to {@code merge} will not be
	 * reflected in the child. Therefore, care should be taken to configure parent
	 * property sources and profile information prior to calling {@code merge}
	 *
	 * <p>
	 * 合并 Parent Environment
	 * <p>
	 * 将给定的 Parent Environment 的 Active Profiles、Default Profiles 和 PropertySources 追加到当前 Environment 中
	 * <ul>
	 *     <li>PropertySources 合并规则：对于相同名称的 PropertySource，当前 Environment 中的会保留，Parent Environment 中的会被丢弃</li>
	 *     <li>Profiles 合并规则：Active Profiles、Default Profiles 合并后会进行去重</li>
	 *     <li>合并之后任何对 Parent Environment 的修改都不会影响当前 Environment。因此，需要在合并之前配置好 Parent Environment</li>
	 * </ul>
	 *
	 * @param parent the environment to merge with
	 * @since 3.1.2
	 * @see org.springframework.context.support.AbstractApplicationContext#setParent
	 */
	void merge(ConfigurableEnvironment parent);

}
