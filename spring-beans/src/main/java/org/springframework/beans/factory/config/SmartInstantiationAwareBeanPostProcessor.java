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

package org.springframework.beans.factory.config;

import java.lang.reflect.Constructor;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory;
import org.springframework.lang.Nullable;

/**
 * Extension of the {@link InstantiationAwareBeanPostProcessor} interface,
 * adding a callback for predicting the eventual type of a processed bean.
 *
 * <p><b>NOTE:</b> This interface is a special purpose interface, mainly for
 * internal use within the framework. In general, application-provided
 * post-processors should simply implement the plain {@link BeanPostProcessor}
 * interface.
 *
 * <p>
 * SmartInstantiationAwareBeanPostProcessor 是 InstantiationAwareBeanPostProcessor 的扩展接口，
 * 提供了更精细的控制能力，特别是在预测 Bean 类型、选择构造函数和处理早期 Bean 引用方面。
 *
 * @author Juergen Hoeller
 * @since 2.0.3
 */
public interface SmartInstantiationAwareBeanPostProcessor extends InstantiationAwareBeanPostProcessor {

	/**
	 * Predict the type of the bean to be eventually returned from this
	 * processor's {@link #postProcessBeforeInstantiation} callback.
	 * <p>The default implementation returns {@code null}.
	 * Specific implementations should try to predict the bean type as
	 * far as known/cached already, without extra processing steps.
	 *
	 * <p>
	 * 预测由 {@link #postProcessBeforeInstantiation} 返回的 Bean 的最终类型。
	 * 默认返回 null。
	 * 实现时应尽可能基于已有信息快速预测类型，避免额外处理开销。
	 * 用于在 Spring 容器中提前确定代理或其他包装对象的实际类型。
	 *
	 * @param beanClass the raw class of the bean
	 * @param beanName the name of the bean
	 * @return the type of the bean, or {@code null} if not predictable
	 * @throws org.springframework.beans.BeansException in case of errors
	 */
	@Nullable
	default Class<?> predictBeanType(Class<?> beanClass, String beanName) throws BeansException {
		return null;
	}

	/**
	 * Determine the type of the bean to be eventually returned from this
	 * processor's {@link #postProcessBeforeInstantiation} callback.
	 * <p>The default implementation returns the given bean class as-is.
	 * Specific implementations should fully evaluate their processing steps
	 * in order to create/initialize a potential proxy class upfront.
	 *
	 * <p>
	 * 更准确地确定由 {@link #postProcessBeforeInstantiation} 返回的 Bean 类型。
	 * 默认返回原始 beanClass。
	 * 相较于 predictBeanType，此方法允许实现者完整评估其处理逻辑以生成/初始化代理类。
	 *
	 * @param beanClass the raw class of the bean
	 * @param beanName the name of the bean
	 * @return the type of the bean (never {@code null})
	 * @throws org.springframework.beans.BeansException in case of errors
	 * @since 6.0
	 */
	default Class<?> determineBeanType(Class<?> beanClass, String beanName) throws BeansException {
		return beanClass;
	}

	/**
	 * Determine the candidate constructors to use for the given bean.
	 * <p>The default implementation returns {@code null}.
	 *
	 * <p>
	 * 确定用于实例化 Bean 的候选构造函数。
	 * 默认返回 null，表示使用默认的构造函数解析机制。
	 * 可用于自定义构造函数注入逻辑，例如根据注解或配置筛选构造函数。
	 *
	 * @param beanClass the raw class of the bean (never {@code null})
	 * @param beanName the name of the bean
	 * @return the candidate constructors, or {@code null} if none specified
	 * @throws org.springframework.beans.BeansException in case of errors
	 */
	@Nullable
	default Constructor<?>[] determineCandidateConstructors(Class<?> beanClass, String beanName)
			throws BeansException {

		return null;
	}

	/**
	 * Obtain a reference for early access to the specified bean,
	 * typically for the purpose of resolving a circular reference.
	 * <p>This callback gives post-processors a chance to expose a wrapper
	 * early - that is, before the target bean instance is fully initialized.
	 * The exposed object should be equivalent to the what
	 * {@link #postProcessBeforeInitialization} / {@link #postProcessAfterInitialization}
	 * would expose otherwise. Note that the object returned by this method will
	 * be used as bean reference unless the post-processor returns a different
	 * wrapper from said post-process callbacks. In other words: Those post-process
	 * callbacks may either eventually expose the same reference or alternatively
	 * return the raw bean instance from those subsequent callbacks (if the wrapper
	 * for the affected bean has been built for a call to this method already,
	 * it will be exposes as final bean reference by default).
	 * <p>The default implementation returns the given {@code bean} as-is.
	 *
	 * <p>
	 * 获取一个 Bean 的早期引用，通常用于解决循环引用。
	 * <p>
	 * 为 SmartInstantiationAwareBeanPostProcessor 提供一个机会，在 target bean 完全初始化前，提前暴露一个 wrapper。
	 * 这个暴露的对象应该和 postProcessBeforeInitialization / postProcessAfterInitialization 方法返回的结果相同。
	 *
	 *
	 * <p>
	 * <b>调用时机</b>
	 * <p>
	 * <ul>
	 *     <li>循环引用时，在 SingletonRegistry 的三级缓存中拿到 ObjectFactory，
	 *     	   调用其中的 {@link AbstractAutowireCapableBeanFactory#getEarlyBeanReference} 方法；</li>
	 *     <li>Spring AOP 也是通过此方法提前暴露代理，避免代理对象和最终实例在循环引用时不一致</li>
	 * </ul>
	 *
	 * <p>
	 * <b>默认实现</b>
	 * <p>
	 * 不做任何处理，返回原始 bean 实例
	 *
	 * @param bean the raw bean instance
	 * @param beanName the name of the bean
	 * @return the object to expose as bean reference
	 * (typically with the passed-in bean instance as default)
	 * @throws org.springframework.beans.BeansException in case of errors
	 */
	default Object getEarlyBeanReference(Object bean, String beanName) throws BeansException {
		return bean;
	}

}
