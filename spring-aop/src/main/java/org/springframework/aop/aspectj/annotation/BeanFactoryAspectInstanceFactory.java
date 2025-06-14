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

package org.springframework.aop.aspectj.annotation;

import java.io.Serializable;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.OrderUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * {@link org.springframework.aop.aspectj.AspectInstanceFactory} implementation
 * backed by a Spring {@link org.springframework.beans.factory.BeanFactory}.
 *
 * <p>Note that this may instantiate multiple times if using a prototype,
 * which probably won't give the semantics you expect.
 * Use a {@link LazySingletonAspectInstanceFactoryDecorator}
 * to wrap this to ensure only one new aspect comes back.
 *
 * <p>
 * 用于通过 BeanFactory 来创建和管理 AspectJ 切面实例
 *
 * <ul>
 *     <li>Aspect 实例管理：负责从 BeanFactory 中获取 AspectJ 切面 Bean 实例，将 AspectJ 切面实例的生命周期管理委托给 BeanFactory；</li>
 *     <li>元数据处理：实现了 MetadataAwareAspectInstanceFactory 接口，提供切面的元数据信息 AspectMetadata。</li>
 * </ul>
 *
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 2.0
 * @see org.springframework.beans.factory.BeanFactory
 * @see LazySingletonAspectInstanceFactoryDecorator
 */
@SuppressWarnings("serial")
public class BeanFactoryAspectInstanceFactory implements MetadataAwareAspectInstanceFactory, Serializable {

	private final BeanFactory beanFactory;

	/**
	 * AspectJ 切面 Bean 的 beanName
	 */
	private final String name;

	/**
	 * AspectJ 切面 Bean 的元数据
	 */
	private final AspectMetadata aspectMetadata;


	/**
	 * Create a BeanFactoryAspectInstanceFactory. AspectJ will be called to
	 * introspect to create AJType metadata using the type returned for the
	 * given bean name from the BeanFactory.
	 * @param beanFactory the BeanFactory to obtain instance(s) from
	 * @param name the name of the bean
	 */
	public BeanFactoryAspectInstanceFactory(BeanFactory beanFactory, String name) {
		this(beanFactory, name, null);
	}

	/**
	 * Create a BeanFactoryAspectInstanceFactory, providing a type that AspectJ should
	 * introspect to create AJType metadata. Use if the BeanFactory may consider the type
	 * to be a subclass (as when using CGLIB), and the information should relate to a superclass.
	 * @param beanFactory the BeanFactory to obtain instance(s) from
	 * @param name the name of the bean
	 * @param type the type that should be introspected by AspectJ
	 * ({@code null} indicates resolution through {@link BeanFactory#getType} via the bean name)
	 */
	public BeanFactoryAspectInstanceFactory(BeanFactory beanFactory, String name, @Nullable Class<?> type) {
		Assert.notNull(beanFactory, "BeanFactory must not be null");
		Assert.notNull(name, "Bean name must not be null");
		this.beanFactory = beanFactory;
		this.name = name;

		// 通过 BeanFactory 获取 AspectJ 切面 Bean 的类型，并创建 AspectMetadata
		Class<?> resolvedType = type;
		if (type == null) {
			resolvedType = beanFactory.getType(name);
			Assert.notNull(resolvedType, "Unresolvable bean type - explicitly specify the aspect class");
		}
		this.aspectMetadata = new AspectMetadata(resolvedType, name);
	}


	/**
	 * 切面实例获取：直接委托给 BeanFactory 获取 Bean 实例，如果是 prototype 作用域，每次调用都会创建新实例
	 */
	@Override
	public Object getAspectInstance() {
		return this.beanFactory.getBean(this.name);
	}

	@Override
	@Nullable
	public ClassLoader getAspectClassLoader() {
		return (this.beanFactory instanceof ConfigurableBeanFactory cbf ?
				cbf.getBeanClassLoader() : ClassUtils.getDefaultClassLoader());
	}

	@Override
	public AspectMetadata getAspectMetadata() {
		return this.aspectMetadata;
	}

	/**
	 * 线程安全控制
	 * <p>
	 * 根据 Bean 的作用域采用不同的锁策略：
	 * <ul>
	 *     <li>Singleton Bean 依赖容器的单例语义，不需要额外锁；</li>
	 *     <li>非 Singleton Bean 需要同步控制以避免并发问题。</li>
	 *
	 *
	 *     <li>singleton：如果 Bean 作用域是 singleton，则直接返回 null，表示不进行线程安全控制；</li>
	 *     <li>prototype：如果 Bean 作用域是 prototype，则使用 BeanFactory 的 singletonMutex 锁，
	 *             如果 BeanFactory 不是 ConfigurableBeanFactory 类型的，则使用 BeanFactory 本身作为锁；</li>
	 * </ul>
	 */
	@Override
	@Nullable
	public Object getAspectCreationMutex() {
		// Singleton Bean 不进行线程安全控制
		if (this.beanFactory.isSingleton(this.name)) {
			// Rely on singleton semantics provided by the factory -> no local lock.
			return null;
		}
		// 非 Singleton Bean 需要同步控制，使用 ConfigurableBeanFactory 的 singletonMutex，
		// 或者 BeanFactoryAspectInstanceFactory 自身
		else if (this.beanFactory instanceof ConfigurableBeanFactory cbf) {
			// No singleton guarantees from the factory -> let's lock locally but
			// reuse the factory's singleton lock, just in case a lazy dependency
			// of our advice bean happens to trigger the singleton lock implicitly...
			return cbf.getSingletonMutex();
		}
		else {
			return this;
		}
	}

	/**
	 * Determine the order for this factory's target aspect, either
	 * an instance-specific order expressed through implementing the
	 * {@link org.springframework.core.Ordered} interface (only
	 * checked for singleton beans), or an order expressed through the
	 * {@link org.springframework.core.annotation.Order} annotation
	 * at the class level.
	 * @see org.springframework.core.Ordered
	 * @see org.springframework.core.annotation.Order
	 */
	@Override
	public int getOrder() {
		Class<?> type = this.beanFactory.getType(this.name);
		if (type != null) {
			// Singleton Bean 可以使用通过 Ordered 接口或获取顺序
			if (Ordered.class.isAssignableFrom(type) && this.beanFactory.isSingleton(this.name)) {
				return ((Ordered) this.beanFactory.getBean(this.name)).getOrder();
			}

			// 使用 @Order 注解获取顺序
			return OrderUtils.getOrder(type, Ordered.LOWEST_PRECEDENCE);
		}

		// 默认使用最低优先级
		return Ordered.LOWEST_PRECEDENCE;
	}


	@Override
	public String toString() {
		return getClass().getSimpleName() + ": bean name '" + this.name + "'";
	}

}
