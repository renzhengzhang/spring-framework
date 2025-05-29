/*
 * Copyright 2002-2020 the original author or authors.
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

import org.springframework.lang.Nullable;

/**
 * Interface for resolving properties against any underlying source.
 *
 * <p>
 * 定义了一个统一的 property 解析接口，可以从任何底层数据源（如配置文件、系统属性、环境变量等）中解析 property。
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.1
 * @see Environment
 * @see PropertySourcesPropertyResolver
 */
public interface PropertyResolver {

	/**
	 * Return whether the given property key is available for resolution,
	 * i.e. if the value for the given key is not {@code null}.
	 *
	 * <p>
	 * property 存在性检查
	 * <p>
	 * 检查指定的 property key 是否可用于解析，如果该 key 对应的值不为 null，则返回 true
	 */
	boolean containsProperty(String key);

	/**
	 * Return the property value associated with the given key,
	 * or {@code null} if the key cannot be resolved.
	 *
	 * <p>
	 * 基础 property 获取方法
	 * <p>
	 * 返回指定 property key 的 property value，如果无法解析则返回 {@code null}
	 *
	 * @param key the property name to resolve
	 * @see #getProperty(String, String)
	 * @see #getProperty(String, Class)
	 * @see #getRequiredProperty(String)
	 */
	@Nullable
	String getProperty(String key);

	/**
	 * Return the property value associated with the given key, or
	 * {@code defaultValue} if the key cannot be resolved.
	 *
	 * <p>
	 * 基础 property 获取方法
	 * <p>
	 * 返回指定 property key 的 property value，如果无法解析则返回 {@code defaultValue}
	 *
	 * @param key the property name to resolve
	 * @param defaultValue the default value to return if no value is found
	 * @see #getRequiredProperty(String)
	 * @see #getProperty(String, Class)
	 */
	String getProperty(String key, String defaultValue);

	/**
	 * Return the property value associated with the given key,
	 * or {@code null} if the key cannot be resolved.
	 *
	 * <p>
	 * 类型转换支持的属性获取
	 * <ul>
	 *     <li>支持将属性值转换为指定的目标类型</li>
	 *     <li>提供类型安全的属性值获取</li>
	 * </ul>
	 *
	 * @param key the property name to resolve
	 * @param targetType the expected type of the property value
	 * @see #getRequiredProperty(String, Class)
	 */
	@Nullable
	<T> T getProperty(String key, Class<T> targetType);

	/**
	 * Return the property value associated with the given key,
	 * or {@code defaultValue} if the key cannot be resolved.
	 *
	 * <p>
	 * 类型转换支持的属性获取
	 * <ul>
	 *     <li>支持将属性值转换为指定的目标类型</li>
	 *     <li>提供类型安全的属性值获取</li>
	 *     <li>类型化的默认值</li>
	 * </ul>
	 *
	 * @param key the property name to resolve
	 * @param targetType the expected type of the property value
	 * @param defaultValue the default value to return if no value is found
	 * @see #getRequiredProperty(String, Class)
	 */
	<T> T getProperty(String key, Class<T> targetType, T defaultValue);

	/**
	 * Return the property value associated with the given key (never {@code null}).
	 *
	 * <p>
	 * 必需属性获取（强制性）
	 * <ul>
	 *     <li>获取必需的属性值，如果无法解析会抛出 {@link IllegalStateException}</li>
	 *     <li>确保关键配置项的存在性</li>
	 * </ul>
	 *
	 * @throws IllegalStateException if the key cannot be resolved
	 * @see #getRequiredProperty(String, Class)
	 */
	String getRequiredProperty(String key) throws IllegalStateException;

	/**
	 * Return the property value associated with the given key, converted to the given
	 * targetType (never {@code null}).
	 *
	 * <p>
	 * 必需属性获取（强制性）
	 * <ul>
	 *     <li>获取必需的属性值，如果无法解析会抛出 {@link IllegalStateException}</li>
	 *     <li>确保关键配置项的存在性</li>
	 *     <li>提供类型安全的属性值获取</li>
	 * </ul>
	 *
	 * @throws IllegalStateException if the given key cannot be resolved
	 */
	<T> T getRequiredProperty(String key, Class<T> targetType) throws IllegalStateException;

	/**
	 * Resolve ${...} placeholders in the given text, replacing them with corresponding
	 * property values as resolved by {@link #getProperty}. Unresolvable placeholders with
	 * no default value are ignored and passed through unchanged.
	 *
	 * <p>
	 * 占位符解析功能
	 * <p>
	 * 使用 {@link PropertyResolver#getProperty} 替换文本中的 {@code ${...}} 占位符，
	 * 替换为对应的 property 值，如果无法解析且无默认值则跳过不处理
	 *
	 * @param text the String to resolve
	 * @return the resolved String (never {@code null})
	 * @throws IllegalArgumentException if given text is {@code null}
	 * @see #resolveRequiredPlaceholders
	 */
	String resolvePlaceholders(String text);

	/**
	 * Resolve ${...} placeholders in the given text, replacing them with corresponding
	 * property values as resolved by {@link #getProperty}. Unresolvable placeholders with
	 * no default value will cause an IllegalArgumentException to be thrown.
	 * @return the resolved String (never {@code null})
	 *
	 * <p>
	 * 占位符解析功能
	 * <p>
	 * 使用 {@link PropertyResolver#getProperty} 替换文本中的 {@code ${...}} 占位符，
	 * 替换为对应的 property 值，如果无法解析且无默认值则抛出 {@link IllegalArgumentException}
	 *
	 * @throws IllegalArgumentException if given text is {@code null}
	 * or if any placeholders are unresolvable
	 */
	String resolveRequiredPlaceholders(String text) throws IllegalArgumentException;

}
