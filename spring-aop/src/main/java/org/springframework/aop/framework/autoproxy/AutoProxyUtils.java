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

package org.springframework.aop.framework.autoproxy;

import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.Conventions;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * Utilities for auto-proxy aware components.
 * Mainly for internal use within the framework.
 *
 * @author Juergen Hoeller
 * @since 2.0.3
 * @see AbstractAutoProxyCreator
 */
public abstract class AutoProxyUtils {

	/**
	 * Bean definition attribute that may indicate whether a given bean is supposed
	 * to be proxied with its target class (in case of it getting proxied in the first
	 * place). The value is {@code Boolean.TRUE} or {@code Boolean.FALSE}.
	 * <p>Proxy factories can set this attribute if they built a target class proxy
	 * for a specific bean, and want to enforce that bean can always be cast
	 * to its target class (even if AOP advices get applied through auto-proxying).
	 * @see #shouldProxyTargetClass
	 */
	public static final String PRESERVE_TARGET_CLASS_ATTRIBUTE =
			Conventions.getQualifiedAttributeName(AutoProxyUtils.class, "preserveTargetClass");

	/**
	 * Bean definition attribute that indicates the original target class of an
	 * auto-proxied bean, e.g. to be used for the introspection of annotations
	 * on the target class behind an interface-based proxy.
	 * @since 4.2.3
	 * @see #determineTargetClass
	 */
	public static final String ORIGINAL_TARGET_CLASS_ATTRIBUTE =
			Conventions.getQualifiedAttributeName(AutoProxyUtils.class, "originalTargetClass");


	/**
	 * Determine whether the given bean should be proxied with its target
	 * class rather than its interfaces. Checks the
	 * {@link #PRESERVE_TARGET_CLASS_ATTRIBUTE "preserveTargetClass" attribute}
	 * of the corresponding bean definition.
	 *
	 * <p>
	 * 判断给定的 bean 是否应该使用其 targetClass 进行代理（即使用 CGLIB 代理），而不是接口代理（JDK 动态代理）
	 * <p>
	 * 通过 BeanDefinition 的 PRESERVE_TARGET_CLASS_ATTRIBUTE 属性判断
	 *
	 * @param beanFactory the containing ConfigurableListableBeanFactory
	 * @param beanName the name of the bean
	 * @return whether the given bean should be proxied with its target class
	 */
	public static boolean shouldProxyTargetClass(
			ConfigurableListableBeanFactory beanFactory, @Nullable String beanName) {

		// 检查给定 bean 的 BeanDefinition 的 PRESERVE_TARGET_CLASS_ATTRIBUTE 属性是否为 true
		// 可以通过如下方式指定使用 CGLIB 代理：
		// 1. XML 配置：<aop:aspectj-autoproxy proxy-target-class="true"/>
		// 2. 注解配置：@EnableAspectJAutoProxy(proxyTargetClass = true)
		if (beanName != null && beanFactory.containsBeanDefinition(beanName)) {
			BeanDefinition bd = beanFactory.getBeanDefinition(beanName);
			return Boolean.TRUE.equals(bd.getAttribute(PRESERVE_TARGET_CLASS_ATTRIBUTE));
		}
		return false;
	}

	/**
	 * Determine the original target class for the specified bean, if possible,
	 * otherwise falling back to a regular {@code getType} lookup.
	 * @param beanFactory the containing ConfigurableListableBeanFactory
	 * @param beanName the name of the bean
	 * @return the original target class as stored in the bean definition, if any
	 * @since 4.2.3
	 * @see org.springframework.beans.factory.BeanFactory#getType(String)
	 */
	@Nullable
	public static Class<?> determineTargetClass(
			ConfigurableListableBeanFactory beanFactory, @Nullable String beanName) {

		if (beanName == null) {
			return null;
		}
		if (beanFactory.containsBeanDefinition(beanName)) {
			BeanDefinition bd = beanFactory.getMergedBeanDefinition(beanName);
			Class<?> targetClass = (Class<?>) bd.getAttribute(ORIGINAL_TARGET_CLASS_ATTRIBUTE);
			if (targetClass != null) {
				return targetClass;
			}
		}
		return beanFactory.getType(beanName);
	}

	/**
	 * Expose the given target class for the specified bean, if possible.
	 *
	 * <p>
	 * 尽可能将 AOP 代理的 targetClass 暴露给 ConfigurableListableBeanFactory
	 *
	 * @param beanFactory the containing ConfigurableListableBeanFactory
	 * @param beanName the name of the bean
	 * @param targetClass the corresponding target class
	 * @since 4.2.3
	 */
	static void exposeTargetClass(
			ConfigurableListableBeanFactory beanFactory, @Nullable String beanName, Class<?> targetClass) {

		// 在给定 bean 的 'merged' BeanDefinition 中设置 targetClass 属性
		if (beanName != null && beanFactory.containsBeanDefinition(beanName)) {
			beanFactory.getMergedBeanDefinition(beanName).setAttribute(ORIGINAL_TARGET_CLASS_ATTRIBUTE, targetClass);
		}
	}

	/**
	 * Determine whether the given bean name indicates an "original instance"
	 * according to {@link AutowireCapableBeanFactory#ORIGINAL_INSTANCE_SUFFIX},
	 * skipping any proxy attempts for it.
	 *
	 * <p>
	 * 判断给定的 beanName 是否是一个"原始实例"，beanName 如果以 ".ORIGINAL" 作为后缀表示这是一个原始实例
	 *
	 * <p>
	 * 在 @Configuration 注解的配置类中，可以使用 @Bean("com.example.UserService.ORIGINAL") 声明一个原始实例，不用被代理
	 *
	 * @param beanName the name of the bean
	 * @param beanClass the corresponding bean class
	 * @since 5.1
	 * @see AutowireCapableBeanFactory#ORIGINAL_INSTANCE_SUFFIX
	 */
	static boolean isOriginalInstance(String beanName, Class<?> beanClass) {
		// beanName 不能为空
		if (!StringUtils.hasLength(beanName) || beanName.length() !=
				beanClass.getName().length() + AutowireCapableBeanFactory.ORIGINAL_INSTANCE_SUFFIX.length()) {
			return false;
		}

		// beanName 以 beanClass.getName() 开头， 以 ".ORIGINAL" 作为结尾，说明是原始实例
		return (beanName.startsWith(beanClass.getName()) &&
				beanName.endsWith(AutowireCapableBeanFactory.ORIGINAL_INSTANCE_SUFFIX));
	}

}
