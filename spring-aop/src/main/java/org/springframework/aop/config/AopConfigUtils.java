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

package org.springframework.aop.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.aop.aspectj.annotation.AnnotationAwareAspectJAutoProxyCreator;
import org.springframework.aop.aspectj.autoproxy.AspectJAwareAdvisorAutoProxyCreator;
import org.springframework.aop.framework.autoproxy.InfrastructureAdvisorAutoProxyCreator;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.Ordered;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Utility class for handling registration of AOP auto-proxy creators.
 *
 * <p>Only a single auto-proxy creator should be registered yet multiple concrete
 * implementations are available. This class provides a simple escalation protocol,
 * allowing a caller to request a particular auto-proxy creator and know that creator,
 * <i>or a more capable variant thereof</i>, will be registered as a post-processor.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @since 2.5
 * @see AopNamespaceUtils
 */
public abstract class AopConfigUtils {

	/**
	 * The bean name of the internally managed auto-proxy creator.
	 */
	public static final String AUTO_PROXY_CREATOR_BEAN_NAME =
			"org.springframework.aop.config.internalAutoProxyCreator";

	/**
	 * Stores the auto proxy creator classes in escalation order.
	 */
	private static final List<Class<?>> APC_PRIORITY_LIST = new ArrayList<>(3);

	static {
		// Set up the escalation list...
		APC_PRIORITY_LIST.add(InfrastructureAdvisorAutoProxyCreator.class);
		APC_PRIORITY_LIST.add(AspectJAwareAdvisorAutoProxyCreator.class);
		APC_PRIORITY_LIST.add(AnnotationAwareAspectJAutoProxyCreator.class);
	}


	@Nullable
	public static BeanDefinition registerAutoProxyCreatorIfNecessary(BeanDefinitionRegistry registry) {
		return registerAutoProxyCreatorIfNecessary(registry, null);
	}

	@Nullable
	public static BeanDefinition registerAutoProxyCreatorIfNecessary(
			BeanDefinitionRegistry registry, @Nullable Object source) {

		return registerOrEscalateApcAsRequired(InfrastructureAdvisorAutoProxyCreator.class, registry, source);
	}

	@Nullable
	public static BeanDefinition registerAspectJAutoProxyCreatorIfNecessary(BeanDefinitionRegistry registry) {
		return registerAspectJAutoProxyCreatorIfNecessary(registry, null);
	}

	@Nullable
	public static BeanDefinition registerAspectJAutoProxyCreatorIfNecessary(
			BeanDefinitionRegistry registry, @Nullable Object source) {

		return registerOrEscalateApcAsRequired(AspectJAwareAdvisorAutoProxyCreator.class, registry, source);
	}

	@Nullable
	public static BeanDefinition registerAspectJAnnotationAutoProxyCreatorIfNecessary(BeanDefinitionRegistry registry) {
		return registerAspectJAnnotationAutoProxyCreatorIfNecessary(registry, null);
	}

	@Nullable
	public static BeanDefinition registerAspectJAnnotationAutoProxyCreatorIfNecessary(
			BeanDefinitionRegistry registry, @Nullable Object source) {
		// 将 AnnotationAwareAspectJAutoProxyCreator 的 BeanDefinition 注册到 BeanDefinitionRegistry 中
		// 1. 如果 BeanDefinitionRegistry 中已经存在名为 AopConfigUtils.AUTO_PROXY_CREATOR_BEAN_NAME 的 BeanDefinition
		//    1. 若已有的 AutoProxyCreator 优先级更高，则不进行处理，返回 null；
		//    2. 否则更新 BeanDefinition 的 class 为 AnnotationAwareAspectJAutoProxyCreator，返回 null
		// 2. 如果 BeanDefinitionRegistry 不存在名为 AopConfigUtils.AUTO_PROXY_CREATOR_BEAN_NAME 的 BeanDefinition，
		//    注册并返回新的 BeanDefinition
		return registerOrEscalateApcAsRequired(AnnotationAwareAspectJAutoProxyCreator.class, registry, source);
	}

	/**
	 * 强制 AutoProxyCreator 使用基于类的代理（CGLIB）
	 * <p>
	 * Spring 同时使用 JDK 动态代理和 CGLIB 代理来处理 AOP 代理，
	 * 如果被代理的目标对象实现了至少一个接口，则会使用 JDK 动态代理，所有该目标类型实现的接口都将被代理。
	 * 若该目标对象没有实现任何接口，则创建一个 CGLIB 代理。若需要强制使用 CGLIB 基于类的代理，可以调用此方法。
	 *
	 * @param registry BeanDefinitionRegistry
	 */
	public static void forceAutoProxyCreatorToUseClassProxying(BeanDefinitionRegistry registry) {
		if (registry.containsBeanDefinition(AUTO_PROXY_CREATOR_BEAN_NAME)) {
			BeanDefinition definition = registry.getBeanDefinition(AUTO_PROXY_CREATOR_BEAN_NAME);

			// 将 BeanDefinition 的 proxyTargetClass 属性设置为 true，强制使用 CGLIB 基于类的代理
			definition.getPropertyValues().add("proxyTargetClass", Boolean.TRUE);
		}
	}

	/**
	 * 强制 AutoProxyCreator 暴露当前代理对象
	 * <p>
	 * 目标对象内部的自我调用将无法实施切面中的增强，暴露当前代理对象可以通过 AopContext 获取当前代理
	 */
	public static void forceAutoProxyCreatorToExposeProxy(BeanDefinitionRegistry registry) {
		if (registry.containsBeanDefinition(AUTO_PROXY_CREATOR_BEAN_NAME)) {
			BeanDefinition definition = registry.getBeanDefinition(AUTO_PROXY_CREATOR_BEAN_NAME);

			// 将 BeanDefinition 的 exposeProxy 属性设置为 true，启用当前代理的暴露功能
			definition.getPropertyValues().add("exposeProxy", Boolean.TRUE);
		}
	}

	/**
	 * 注册或者升级 AutoProxyCreator BeanDefinition
	 */
	@Nullable
	private static BeanDefinition registerOrEscalateApcAsRequired(
			Class<?> cls, BeanDefinitionRegistry registry, @Nullable Object source) {

		Assert.notNull(registry, "BeanDefinitionRegistry must not be null");

		// BeanDefinitionRegistry 已经存在 AUTO_PROXY_CREATOR_BEAN_NAME 的 BeanDefinition，判断是否需要升级
		if (registry.containsBeanDefinition(AUTO_PROXY_CREATOR_BEAN_NAME)) {
			BeanDefinition apcDefinition = registry.getBeanDefinition(AUTO_PROXY_CREATOR_BEAN_NAME);
			if (!cls.getName().equals(apcDefinition.getBeanClassName())) {
				// 如果当前的 BeanDefinition 的类型与 cls 不同，则基于 className 获取优先级，
				// 如果当前的优先级小于 cls 的优先级，则升级 BeanDefinition 的类型
				int currentPriority = findPriorityForClass(apcDefinition.getBeanClassName());
				int requiredPriority = findPriorityForClass(cls);
				if (currentPriority < requiredPriority) {
					apcDefinition.setBeanClassName(cls.getName());
				}
			}

			// 如果当前的 BeanDefinition 的类型与 cls 相同，不做处理
			return null;
		}

		// BeanDefinitionRegistry 不存在 AUTO_PROXY_CREATOR_BEAN_NAME 的 BeanDefinition，则进行注册
		RootBeanDefinition beanDefinition = new RootBeanDefinition(cls);
		beanDefinition.setSource(source);
		beanDefinition.getPropertyValues().add("order", Ordered.HIGHEST_PRECEDENCE);
		beanDefinition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		registry.registerBeanDefinition(AUTO_PROXY_CREATOR_BEAN_NAME, beanDefinition);
		return beanDefinition;
	}

	private static int findPriorityForClass(Class<?> clazz) {
		return APC_PRIORITY_LIST.indexOf(clazz);
	}

	/**
	 * 根据类名查找对应的优先级
	 *
	 * @param className 类名
	 * @return 优先级索引
	 * @throws IllegalArgumentException 如果类名不在已知的自动代理创建者类列表中
	 */
	private static int findPriorityForClass(@Nullable String className) {
		for (int i = 0; i < APC_PRIORITY_LIST.size(); i++) {
			Class<?> clazz = APC_PRIORITY_LIST.get(i);
			if (clazz.getName().equals(className)) {
				return i;
			}
		}
		throw new IllegalArgumentException(
				"Class name [" + className + "] is not a known auto-proxy creator class");
	}

}
