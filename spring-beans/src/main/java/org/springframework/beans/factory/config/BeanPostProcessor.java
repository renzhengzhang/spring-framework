/*
 * Copyright 2002-2019 the original author or authors.
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
import org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory;
import org.springframework.lang.Nullable;

/**
 * Factory hook that allows for custom modification of new bean instances &mdash;
 * for example, checking for marker interfaces or wrapping beans with proxies.
 *
 * <p>Typically, post-processors that populate beans via marker interfaces
 * or the like will implement {@link #postProcessBeforeInitialization},
 * while post-processors that wrap beans with proxies will normally
 * implement {@link #postProcessAfterInitialization}.
 *
 * <h3>Registration</h3>
 * <p>An {@code ApplicationContext} can autodetect {@code BeanPostProcessor} beans
 * in its bean definitions and apply those post-processors to any beans subsequently
 * created. A plain {@code BeanFactory} allows for programmatic registration of
 * post-processors, applying them to all beans created through the bean factory.
 *
 * <h3>Ordering</h3>
 * <p>{@code BeanPostProcessor} beans that are autodetected in an
 * {@code ApplicationContext} will be ordered according to
 * {@link org.springframework.core.PriorityOrdered} and
 * {@link org.springframework.core.Ordered} semantics. In contrast,
 * {@code BeanPostProcessor} beans that are registered programmatically with a
 * {@code BeanFactory} will be applied in the order of registration; any ordering
 * semantics expressed through implementing the
 * {@code PriorityOrdered} or {@code Ordered} interface will be ignored for
 * programmatically registered post-processors. Furthermore, the
 * {@link org.springframework.core.annotation.Order @Order} annotation is not
 * taken into account for {@code BeanPostProcessor} beans.
 *
 * <p>
 * BeanPostProcessor 允许开发者在 Spring 容器完成 Bean 的实例化、依赖注入后，在调用显式初始化方法之前和之后，对 Bean 实例进行自定义修改。
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 10.10.2003
 * @see InstantiationAwareBeanPostProcessor
 * @see DestructionAwareBeanPostProcessor
 * @see ConfigurableBeanFactory#addBeanPostProcessor
 * @see BeanFactoryPostProcessor
 */
public interface BeanPostProcessor {

	/**
	 * Apply this {@code BeanPostProcessor} to the given new bean instance <i>before</i> any bean
	 * initialization callbacks (like InitializingBean's {@code afterPropertiesSet}
	 * or a custom init-method). The bean will already be populated with property values.
	 * The returned bean instance may be a wrapper around the original.
	 * <p>The default implementation returns the given {@code bean} as-is.
	 *
	 * <p>
	 * 执行时机：
	 * <p>
	 * {@link AbstractAutowireCapableBeanFactory} 会在 Bean 实例化、属性注入之后，执行 Bean 的初始化之前，
	 * 例如 InitializingBean 的 afterPropertiesSet 方法或自定义的 init-method 方法，调用当前方法。
	 *
	 * <p>
	 * 功能：
	 * <ul>
	 *     <li>用来对 Bean 实例进行自定义修改，也可以返回一个原始 Bean 实例的包装实例；</li>
	 * 	   <li>允许开发者对 Bean 进行额外的处理，例如设置某些默认值、校验 Bean 状态或为 Bean 创建代理对象。</li>
	 * </ul>
	 *
	 * <p>
	 * 短路行为：
	 * <p>
	 * 如果当前方法返回了 null，后续的 BeanPostProcessor 将不会被调用。
	 *
	 * <p>
	 * 默认实现：
	 * <p>
	 * 直接返回原始 Bean，不做任何修改。
	 *
	 * <p>
	 * 应用场景：
	 * <ul>
	 *     <li>自动装配逻辑增强：可以在 Bean 初始化前注入额外依赖或配置；</li>
	 *     <li>标记接口检查：根据特定接口或注解对 Bean 进行处理；</li>
	 *     <li>代理创建：虽然通常在 postProcessAfterInitialization 中创建 AOP 代理，但也可以在此阶段进行轻量级包装。</li>
	 * </ul>
	 *
	 * @param bean the new bean instance
	 * @param beanName the name of the bean
	 * @return the bean instance to use, either the original or a wrapped one;
	 * if {@code null}, no subsequent BeanPostProcessors will be invoked
	 * @throws org.springframework.beans.BeansException in case of errors
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet
	 */
	@Nullable
	default Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

	/**
	 * Apply this {@code BeanPostProcessor} to the given new bean instance <i>after</i> any bean
	 * initialization callbacks (like InitializingBean's {@code afterPropertiesSet}
	 * or a custom init-method). The bean will already be populated with property values.
	 * The returned bean instance may be a wrapper around the original.
	 * <p>In case of a FactoryBean, this callback will be invoked for both the FactoryBean
	 * instance and the objects created by the FactoryBean (as of Spring 2.0). The
	 * post-processor can decide whether to apply to either the FactoryBean or created
	 * objects or both through corresponding {@code bean instanceof FactoryBean} checks.
	 * <p>This callback will also be invoked after a short-circuiting triggered by a
	 * {@link InstantiationAwareBeanPostProcessor#postProcessBeforeInstantiation} method,
	 * in contrast to all other {@code BeanPostProcessor} callbacks.
	 * <p>The default implementation returns the given {@code bean} as-is.
	 *
	 * <p>
	 * 执行时机：
	 * <p>
	 * {@link AbstractAutowireCapableBeanFactory} 会在 Bean 实例化、属性注入之后，执行 Bean 的初始化之后，调用此方法。
	 *
	 * <p>
	 * 常见用途：
	 * <p>
	 * 允许开发者对 Bean 进行额外的处理，例如为其创建代理对象（如 AOP 代理），以实现更复杂的功能。
	 * 如果需要区分处理的是 FactoryBean 本身还是其生成的对象，可以通过 {@code bean instanceof FactoryBean} 判断。
	 *
	 * <p>
	 * 短路行为：
	 * <p>
	 * 如果当前方法返回了 null，后续的 BeanPostProcessor 将不会被调用。
	 * <p>
	 * 如果某个 InstantiationAwareBeanPostProcessor 的 postProcessBeforeInstantiation 方法返回了一个非空 Bean 实例，
	 * 则此方法仍然会在该短路行为后被调用。
	 *
	 * <p>
	 * 默认实现：
	 * <p>
	 * 默认情况下，该方法直接返回原始 Bean，不做任何修改。
	 *
	 * <p>
	 * 应用场景
	 * <p>
	 * <ul>
	 *     <li>	创建 AOP 代理：Spring AOP 利用这个方法为 Bean 创建动态代理；</li>
	 * 	   <li>	修改 Bean 属性：虽然通常在 postProcessBeforeInitialization 中处理，但也可以在此阶段进行某些调整；</li>
	 * 	   <li>	注册 Bean 到其他系统：可以在 Bean 初始化完成后注册到外部系统中。</li>
	 * </ul>
	 *
	 * @param bean the new bean instance
	 * @param beanName the name of the bean
	 * @return the bean instance to use, either the original or a wrapped one;
	 * if {@code null}, no subsequent BeanPostProcessors will be invoked
	 * @throws org.springframework.beans.BeansException in case of errors
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet
	 * @see org.springframework.beans.factory.FactoryBean
	 */
	@Nullable
	default Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

}
