/*
 * Copyright 2002-2016 the original author or authors.
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

import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.lang.Nullable;

/**
 * Configuration interface to be implemented by most if not all {@link PropertyResolver}
 * types. Provides facilities for accessing and customizing the
 * {@link org.springframework.core.convert.ConversionService ConversionService}
 * used when converting property values from one type to another.
 *
 * <p>
 * {@link PropertyResolver} 的扩展，提供了配置和自定义属性解析行为的能力，
 *
 * <ul>
 *     <li>支持访问和自定义类型转换服务{@link ConfigurableConversionService} ，用于将 property value 从一种类型转换为另一种类型；</li>
 *     <li>支持设置和验证 required properties</li>
 * </ul>
 *
 * @author Chris Beams
 * @since 3.1
 */
public interface ConfigurablePropertyResolver extends PropertyResolver {

	/**
	 * Return the {@link ConfigurableConversionService} used when performing type
	 * conversions on properties.
	 * <p>The configurable nature of the returned conversion service allows for
	 * the convenient addition and removal of individual {@code Converter} instances:
	 * <pre class="code">
	 * ConfigurableConversionService cs = env.getConversionService();
	 * cs.addConverter(new FooConverter());
	 * </pre>
	 * @see PropertyResolver#getProperty(String, Class)
	 * @see org.springframework.core.convert.converter.ConverterRegistry#addConverter
	 *
	 * <p>
	 * ConversionService 管理 - 获取 ConversionService
	 * <ul>
	 *     <li>返回执行 property value 类型转换时使用的 ConfigurableConversionService</li>
	 *     <li>ConfigurableConversionServic 允许方便地添加和移除单个 Converter，用于支持自定义类型转换逻辑</li>
	 * </ul>
	 */
	ConfigurableConversionService getConversionService();

	/**
	 * Set the {@link ConfigurableConversionService} to be used when performing type
	 * conversions on properties.
	 * <p><strong>Note:</strong> as an alternative to fully replacing the
	 * {@code ConversionService}, consider adding or removing individual
	 * {@code Converter} instances by drilling into {@link #getConversionService()}
	 * and calling methods such as {@code #addConverter}.
	 * @see PropertyResolver#getProperty(String, Class)
	 * @see #getConversionService()
	 * @see org.springframework.core.convert.converter.ConverterRegistry#addConverter
	 *
	 * <p>
	 * ConversionService 管理 - 设置 ConversionService
	 * <ul>
	 *     <li>设置执行 property value 类型转换时使用的 ConfigurableConversionService</li>
	 *     <li>建议优先考虑添加或移除单个 Converter，而不是完全替换 ConfigurableConversionService</li>
	 * </ul>
	 */
	void setConversionService(ConfigurableConversionService conversionService);

	/**
	 * Set the prefix that placeholders replaced by this resolver must begin with.
	 *
	 * <p>
	 * 占位符配置 - 占位符前缀配置，默认通常是 "${"
	 */
	void setPlaceholderPrefix(String placeholderPrefix);

	/**
	 * Set the suffix that placeholders replaced by this resolver must end with.
	 * <p>
	 * 占位符配置 - 占位符后缀配置，默认通常是 "}"
	 */
	void setPlaceholderSuffix(String placeholderSuffix);

	/**
	 * Specify the separating character between the placeholders replaced by this
	 * resolver and their associated default value, or {@code null} if no such
	 * special character should be processed as a value separator.
	 *
	 * <p>
	 * 占位符配置 - 设置占位符与默认值之间的分隔符
	 * <p>
	 * 例如 "${property.name:defaultValue}" 中的 ":"。
	 * <p>
	 * 如果设置为 null，则不处理占位符与默认值之间的分隔符
	 */
	void setValueSeparator(@Nullable String valueSeparator);

	/**
	 * Set whether to throw an exception when encountering an unresolvable placeholder
	 * nested within the value of a given property. A {@code false} value indicates strict
	 * resolution, i.e. that an exception will be thrown. A {@code true} value indicates
	 * that unresolvable nested placeholders should be passed through in their unresolved
	 * ${...} form.
	 * <p>Implementations of {@link #getProperty(String)} and its variants must inspect
	 * the value set here to determine correct behavior when property values contain
	 * unresolvable placeholders.
	 * @since 3.2
	 *
	 * <p>
	 * 嵌套占位符处理 - 设置遇到无法解析的嵌套占位符时的处理策略
	 * <ul>
	 *     <li>设置为 false 时，表示遇到无法解析的嵌套占位符时抛出异常
	 *     <li>设置为 true 时，表示遇到无法解析的嵌套占位符时，保留原样
	 * </ul>
	 */
	void setIgnoreUnresolvableNestedPlaceholders(boolean ignoreUnresolvableNestedPlaceholders);

	/**
	 * Specify which properties must be present, to be verified by
	 * {@link #validateRequiredProperties()}.
	 *
	 * <p>
	 * 必需属性验证 - 指定必须存在的属性名称列表
	 * <p>
	 * 这些属性将由 {@link ConfigurablePropertyResolver#validateRequiredProperties()} 验证
	 */
	void setRequiredProperties(String... requiredProperties);

	/**
	 * Validate that each of the properties specified by
	 * {@link #setRequiredProperties} is present and resolves to a
	 * non-{@code null} value.
	 * @throws MissingRequiredPropertiesException if any of the required
	 * properties are not resolvable.
	 *
	 * <p>
	 * 必需属性验证 - 验证必须存在的属性是否存在
	 * <ul>
	 *     <li>验证通过 {@link ConfigurablePropertyResolver#setRequiredProperties} 指定的每个属性都存在且解析为非 null 值</li>
	 * 	   <li>如果任何必需属性无法解析，抛出 MissingRequiredPropertiesException</li>
	 * </ul>
	 */
	void validateRequiredProperties() throws MissingRequiredPropertiesException;

}
