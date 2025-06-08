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

package org.springframework.beans.factory.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValues;
import org.springframework.lang.Nullable;

/**
 * Subinterface of {@link BeanPostProcessor} that adds a before-instantiation callback,
 * and a callback after instantiation but before explicit properties are set or
 * autowiring occurs.
 *
 * <p>Typically used to suppress default instantiation for specific target beans,
 * for example to create proxies with special TargetSources (pooling targets,
 * lazily initializing targets, etc), or to implement additional injection strategies
 * such as field injection.
 *
 * <p><b>NOTE:</b> This interface is a special purpose interface, mainly for
 * internal use within the framework. It is recommended to implement the plain
 * {@link BeanPostProcessor} interface as far as possible.
 *
 * <p>
 * InstantiationAwareBeanPostProcessor 扩展了 BeanPostProcessor 的功能，提供了在 Bean 实例化前后进行干预的能力，
 * 从而允许开发者对 Bean 的创建过程进行定制。
 *
 * <p>
 * InstantiationAwareBeanPostProcessor 提供了比普通 BeanPostProcessor 更早介入 Bean 生命周期的机会，
 * 使得开发者可以在 Bean 初始化流程的不同阶段进行深度定制。
 *
 * @author Juergen Hoeller
 * @author Rod Johnson
 * @since 1.2
 * @see org.springframework.aop.framework.autoproxy.AbstractAutoProxyCreator#setCustomTargetSourceCreators
 * @see org.springframework.aop.framework.autoproxy.target.LazyInitTargetSourceCreator
 */
public interface InstantiationAwareBeanPostProcessor extends BeanPostProcessor {

	/**
	 * Apply this BeanPostProcessor <i>before the target bean gets instantiated</i>.
	 * The returned bean object may be a proxy to use instead of the target bean,
	 * effectively suppressing default instantiation of the target bean.
	 * <p>If a non-null object is returned by this method, the bean creation process
	 * will be short-circuited. The only further processing applied is the
	 * {@link #postProcessAfterInitialization} callback from the configured
	 * {@link BeanPostProcessor BeanPostProcessors}.
	 * <p>This callback will be applied to bean definitions with their bean class,
	 * as well as to factory-method definitions in which case the returned bean type
	 * will be passed in here.
	 * <p>Post-processors may implement the extended
	 * {@link SmartInstantiationAwareBeanPostProcessor} interface in order
	 * to predict the type of the bean object that they are going to return here.
	 * <p>The default implementation returns {@code null}.
	 *
	 *
	 * <p>
	 * 用于在 Spring 容器实例化 Bean 之前进行干预。它提供了一个机会让开发者可以完全自定义 Bean 的创建过程，甚至跳过 Spring 默认的实例化逻辑。
	 *
	 * <p>
	 * <b>执行时机：</b>
	 * <p>
	 * 在 Spring 准备创建 Bean 实例之前调用，此时 Bean 还未被实例化，也没有开始属性注入或初始化。
	 *
	 * <p>
	 * <b>短路行为：</b>
	 * <ul>
	 *     <li>返回非空对象，后续 InstantiationAwareBeanPostProcessor#postProcessBeforeInstantiation 不再执行；</li>
	 *     <li>返回非空对象，则会跳过默认的 Bean 实例化步骤，以及其余生命周期步骤（如属性注入、初始化方法等，
	 *     	   后续仅执行 BeanPostProcessor 的 postProcessAfterInitialization 方法。</li>
	 * </ul>
	 *
	 * <p>
	 * <b>默认逻辑：</b>
	 * <p>
	 * 返回 null，不干扰默认的 Bean 创建流程。
	 *
	 * <p>
	 * <b>常见用途：</b>
	 * <ul>
	 *     <li>创建 AOP 代理对象，例如 Spring AOP 就在此阶段生成代理；</li>
	 *     <li>替换某些特定类型的 Bean 实现，实现更灵活的对象管理策略；</li>
	 * </ul>
	 *
	 * @param beanClass the class of the bean to be instantiated
	 * @param beanName the name of the bean
	 * @return the bean object to expose instead of a default instance of the target bean,
	 * or {@code null} to proceed with default instantiation
	 *
	 * @throws org.springframework.beans.BeansException in case of errors
	 * @see #postProcessAfterInstantiation
	 * @see org.springframework.beans.factory.support.AbstractBeanDefinition#getBeanClass()
	 * @see org.springframework.beans.factory.support.AbstractBeanDefinition#getFactoryMethodName()
	 */
	@Nullable
	default Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) throws BeansException {
		return null;
	}

	/**
	 * Perform operations after the bean has been instantiated, via a constructor or factory method,
	 * but before Spring property population (from explicit properties or autowiring) occurs.
	 * <p>This is the ideal callback for performing custom field injection on the given bean
	 * instance, right before Spring's autowiring kicks in.
	 * <p>The default implementation returns {@code true}.
	 *
	 * <p>
	 * 在 Bean 被构造函数或工厂方法实例化之后，但在 Spring 进行属性填充（包括显式属性设置和自动装配）之前调用。
	 * 可以用于在此阶段执行自定义字段注入操作。
	 *
	 * <ul>
	 *     <li>1. 默认返回 true，表示继续进行属性填充；</li>
	 *     <li>2 .如果返回 false，则跳过属性填充，并阻止后续的并阻止后续的 InstantiationAwareBeanPostProcessor 实例对此 Bean 实例的处理。</li>
	 * </ul>
	 *
	 * @param bean the bean instance created, with properties not having been set yet
	 * @param beanName the name of the bean
	 * @return {@code true} if properties should be set on the bean; {@code false}
	 * if property population should be skipped. Normal implementations should return {@code true}.
	 * Returning {@code false} will also prevent any subsequent InstantiationAwareBeanPostProcessor
	 * instances being invoked on this bean instance.
	 * @throws org.springframework.beans.BeansException in case of errors
	 * @see #postProcessBeforeInstantiation
	 */
	default boolean postProcessAfterInstantiation(Object bean, String beanName) throws BeansException {
		return true;
	}

	/**
	 * Post-process the given property values before the factory applies them
	 * to the given bean.
	 * <p>The default implementation returns the given {@code pvs} as-is.
	 *
	 * <p>
	 * 将 PropertyValues 应用到 Bean 之前调用，允许修改或替换即将应用的属性值，可用于更细粒度地控制属性注入逻辑。
	 * <p>
	 * 默认不做处理。
	 *
	 * @param pvs the property values that the factory is about to apply (never {@code null})
	 * @param bean the bean instance created, but whose properties have not yet been set
	 * @param beanName the name of the bean
	 * @return the actual property values to apply to the given bean (can be the passed-in
	 * PropertyValues instance), or {@code null} to skip property population
	 * @throws org.springframework.beans.BeansException in case of errors
	 * @since 5.1
	 */
	@Nullable
	default PropertyValues postProcessProperties(PropertyValues pvs, Object bean, String beanName)
			throws BeansException {

		return pvs;
	}

}
