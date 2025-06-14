/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.aop.framework;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.AopInvocationException;
import org.springframework.aop.RawTargetAccess;
import org.springframework.aop.TargetSource;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.DecoratingProxy;
import org.springframework.core.KotlinDetector;
import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * JDK-based {@link AopProxy} implementation for the Spring AOP framework,
 * based on JDK {@link java.lang.reflect.Proxy dynamic proxies}.
 *
 * <p>Creates a dynamic proxy, implementing the interfaces exposed by
 * the AopProxy. Dynamic proxies <i>cannot</i> be used to proxy methods
 * defined in classes, rather than interfaces.
 *
 * <p>Objects of this type should be obtained through proxy factories,
 * configured by an {@link AdvisedSupport} class. This class is internal
 * to Spring's AOP framework and need not be used directly by client code.
 *
 * <p>Proxies created using this class will be thread-safe if the
 * underlying (target) class is thread-safe.
 *
 * <p>Proxies are serializable so long as all Advisors (including Advices
 * and Pointcuts) and the TargetSource are serializable.
 *
 * <p>
 * {@link JdkDynamicAopProxy} 用于创建和处理基于 interfaces 的 dynamic proxies，
 * 利用 JDK 的 {@link java.lang.reflect.Proxy dynamic proxies} 来为目标对象创建代理实例
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Dave Syer
 * @author Sergey Tsypanov
 * @author Sebastien Deleuze
 * @see java.lang.reflect.Proxy
 * @see AdvisedSupport
 * @see ProxyFactory
 */
final class JdkDynamicAopProxy implements AopProxy, InvocationHandler, Serializable {

	/** use serialVersionUID from Spring 1.2 for interoperability. */
	private static final long serialVersionUID = 5531744639992436476L;


	/*
	 * NOTE: We could avoid the code duplication between this class and the CGLIB
	 * proxies by refactoring "invoke" into a template method. However, this approach
	 * adds at least 10% performance overhead versus a copy-paste solution, so we sacrifice
	 * elegance for performance (we have a good test suite to ensure that the different
	 * proxies behave the same :-)).
	 * This way, we can also more easily take advantage of minor optimizations in each class.
	 */

	/** We use a static Log to avoid serialization issues. */
	private static final Log logger = LogFactory.getLog(JdkDynamicAopProxy.class);

	private static final String COROUTINES_FLOW_CLASS_NAME = "kotlinx.coroutines.flow.Flow";

	/**
	 * Config used to configure this proxy.
	 * <p>
	 * 包含 target、targetSource、Advisor 等一系列配置
	 */
	private final AdvisedSupport advised;

	/**
	 * 代理实现的 interfaces array
	 */
	private final Class<?>[] proxiedInterfaces;

	/**
	 * Is the {@link #equals} method defined on the proxied interfaces?
	 * <p>
	 * 标记代理接口中是否定义了 equals 方法
	 */
	private boolean equalsDefined;

	/**
	 * Is the {@link #hashCode} method defined on the proxied interfaces?
	 * <p>
	 * 标记代理接口中是否定义了 hashCode 方法
	 */
	private boolean hashCodeDefined;


	/**
	 * Construct a new JdkDynamicAopProxy for the given AOP configuration.
	 * @param config the AOP configuration as AdvisedSupport object
	 * @throws AopConfigException if the config is invalid. We try to throw an informative
	 * exception in this case, rather than let a mysterious failure happen later.
	 */
	public JdkDynamicAopProxy(AdvisedSupport config) throws AopConfigException {
		Assert.notNull(config, "AdvisedSupport must not be null");
		this.advised = config;
		// 根据 AOP 配置，分析构建代理对象需要实现的所有接口
		this.proxiedInterfaces = AopProxyUtils.completeProxiedInterfaces(this.advised, true);
		// 检查接口中是否定义了 equals 和 hashCode 方法
		findDefinedEqualsAndHashCodeMethods(this.proxiedInterfaces);
	}


	@Override
	public Object getProxy() {
		return getProxy(ClassUtils.getDefaultClassLoader());
	}

	@Override
	public Object getProxy(@Nullable ClassLoader classLoader) {
		if (logger.isTraceEnabled()) {
			logger.trace("Creating JDK dynamic proxy: " + this.advised.getTargetSource());
		}

		// JdkDynamicAopProxy 自身实现了 InvocationHandler，作为 InvocationHandler 传入
		// 当代理对象的方法被调用时，会委托给当前 InvocationHandler 实例的 invoke() 方法处理
		return Proxy.newProxyInstance(determineClassLoader(classLoader), this.proxiedInterfaces, this);
	}

	@SuppressWarnings("deprecation")
	@Override
	public Class<?> getProxyClass(@Nullable ClassLoader classLoader) {
		return Proxy.getProxyClass(determineClassLoader(classLoader), this.proxiedInterfaces);
	}

	/**
	 * Determine whether the JDK bootstrap or platform loader has been suggested ->
	 * use higher-level loader which can see Spring infrastructure classes instead.
	 *
	 * <p>
	 * 解决 JDK 动态代理在不同 ClassLoader 环境下可能遇到的类可见性问题，特别是在模块化系统（如 JDK 9+ 的模块系统）中
	 */
	private ClassLoader determineClassLoader(@Nullable ClassLoader classLoader) {
		if (classLoader == null) {
			// JDK bootstrap loader -> use spring-aop ClassLoader instead.
			// 当传入的 classLoader 为 null 时，表示要使用 JDK 的 Bootstrap ClassLoader
			// 但 Bootstrap ClassLoader 只能加载 JDK 核心类（如 java.lang.*），无法看到 Spring AOP 框架的类
			// 使用当前 JdkDynamicAopProxy 类的 ClassLoader，这样可以确保能够访问 Spring AOP 的所有基础设施类
			return getClass().getClassLoader();
		}

		if (classLoader.getParent() == null) {
			// Potentially the JDK platform loader on JDK 9+
			// 在 JDK 9+ 中，classLoader.getParent() == null 通常表示这是 Platform ClassLoader
			ClassLoader aopClassLoader = getClass().getClassLoader();
			ClassLoader aopParent = aopClassLoader.getParent();

			// 向上遍历 aopClassLoader 的 parent ClassLoader chain，如果发现传入的 classLoader 是 aopClassLoader 的某个祖先，
			// 则直接使用 aopClassLoader，这样可以确保能够访问 Spring AOP 的所有基础设施类
			while (aopParent != null) {
				if (classLoader == aopParent) {
					// Suggested ClassLoader is ancestor of spring-aop ClassLoader
					// -> use spring-aop ClassLoader itself instead.
					return aopClassLoader;
				}
				aopParent = aopParent.getParent();
			}
		}

		// Regular case: use suggested ClassLoader as-is.
		// 常规情况, 直接使用传入的 ClassLoader
		return classLoader;
	}

	/**
	 * Finds any {@link #equals} or {@link #hashCode} method that may be defined
	 * on the supplied set of interfaces.
	 *
	 * <p>
	 * 遍历给定的所有 Interface 的所有 Method，判断其中是否存在 equals 和 hashCode 方法
	 *
	 * @param proxiedInterfaces the interfaces to introspect
	 */
	private void findDefinedEqualsAndHashCodeMethods(Class<?>[] proxiedInterfaces) {
		for (Class<?> proxiedInterface : proxiedInterfaces) {
			Method[] methods = proxiedInterface.getDeclaredMethods();
			for (Method method : methods) {
				// 判断给定的方法是否是 equals 方法
				if (AopUtils.isEqualsMethod(method)) {
					this.equalsDefined = true;
				}
				// 判断给定的方法是否是 hashCode 方法
				if (AopUtils.isHashCodeMethod(method)) {
					this.hashCodeDefined = true;
				}

				// 都已经判断好了，无需再继续遍历
				if (this.equalsDefined && this.hashCodeDefined) {
					return;
				}
			}
		}
	}


	/**
	 * Implementation of {@code InvocationHandler.invoke}.
	 * <p>Callers will see exactly the exception thrown by the target,
	 * unless a hook method throws an exception.
	 *
	 * <p>
	 * {@link InvocationHandler#invoke} 方法的实现。
	 * <p>
	 * 当代理对象的任何方法被调用时，会委托给这个方法处理。
	 */
	@Override
	@Nullable
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		Object oldProxy = null;
		boolean setProxyContext = false;

		TargetSource targetSource = this.advised.targetSource;
		Object target = null;

		try {
			// 特殊方法处理
			// 当调用的是 equals 方法，且代理接口中没有定义 equals 方法时，使用代理对象自身的 equals 实现
			if (!this.equalsDefined && AopUtils.isEqualsMethod(method)) {
				// The target does not implement the equals(Object) method itself.
				return equals(args[0]);
			}
			// 当调用的是 hashCode 方法，且代理接口中没有定义 hashCode 方法时，使用代理对象自身的 hashCode 实现
			else if (!this.hashCodeDefined && AopUtils.isHashCodeMethod(method)) {
				// The target does not implement the hashCode() method itself.
				return hashCode();
			}
			// 调用 DecoratingProxy.getDecoratedClass() 方法，返回代理对象所代理的最终的 target class
			else if (method.getDeclaringClass() == DecoratingProxy.class) {
				// There is only getDecoratedClass() declared -> dispatch to proxy config.
				// TODO: 查看实现
				return AopProxyUtils.ultimateTargetClass(this.advised);
			}
			// 调用 Advised 接口的方法（如获取 Advisors、添加 Advice 等），直接在 AdvisedSupport 对象上执行方法
			else if (!this.advised.opaque && method.getDeclaringClass().isInterface() &&
					method.getDeclaringClass().isAssignableFrom(Advised.class)) {
				// Service invocations on ProxyConfig with the proxy config...
				return AopUtils.invokeJoinpointUsingReflection(this.advised, method, args);
			}

			Object retVal;

			// 如果配置了 exposeProxy=true，将当前代理对象暴露到 AopContext 中，
			// 支持在 Advice 中通过 AopContext.currentProxy() 获取当前代理对象
			if (this.advised.exposeProxy) {
				// Make invocation available if necessary.
				oldProxy = AopContext.setCurrentProxy(proxy);
				setProxyContext = true;
			}

			// Get as late as possible to minimize the time we "own" the target,
			// in case it comes from a pool.
			// 获取目标对象
			// 尽可能晚地获取目标对象，最小化"拥有"目标对象的时间，目标对象可能来自对象池，需要及时释放
			target = targetSource.getTarget();
			Class<?> targetClass = (target != null ? target.getClass() : null);

			// Get the interception chain for this method.
			// 构建拦截器链
			// 获取适用于当前方法的所有 MethodInterceptor 和 InterceptorAndDynamicMethodMatcher
			List<Object> chain = this.advised.getInterceptorsAndDynamicInterceptionAdvice(method, targetClass);

			// Check whether we have any advice. If we don't, we can fall back on direct
			// reflective invocation of the target, and avoid creating a MethodInvocation.
			// 无 Interceptor 的情况，直接反射调用目标方法，避免创建 MethodInvocation 对象的开销
			if (chain.isEmpty()) {
				// We can skip creating a MethodInvocation: just invoke the target directly
				// Note that the final invoker must be an InvokerInterceptor so we know it does
				// nothing but a reflective operation on the target, and no hot swapping or fancy proxying.
				Object[] argsToUse = AopProxyUtils.adaptArgumentsIfNecessary(method, args);
				retVal = AopUtils.invokeJoinpointUsingReflection(target, method, argsToUse);
			}
			// 有 Interceptor 的情况，创建 MethodInvocation 对象，通过 proceed() 方法依次执行拦截器链
			else {
				// We need to create a method invocation...
				MethodInvocation invocation =
						new ReflectiveMethodInvocation(proxy, target, method, args, targetClass, chain);
				// Proceed to the joinpoint through the interceptor chain.
				retVal = invocation.proceed();
			}

			// Massage return value if necessary.
			// 特殊返回情况处理，目标方法返回 this，目标方法返回自身，需要将返回值替换为代理对象，以保持代理链的一致性
			Class<?> returnType = method.getReturnType();
			if (retVal != null && retVal == target &&
					returnType != Object.class && returnType.isInstance(proxy) &&
					!RawTargetAccess.class.isAssignableFrom(method.getDeclaringClass())) {
				// Special case: it returned "this" and the return type of the method
				// is type-compatible. Note that we can't help if the target sets
				// a reference to itself in another returned object.
				retVal = proxy;
			}
			// 基本类型 null 检查，方法返回基本类型但实际返回了 null，抛出异常
			else if (retVal == null && returnType != Void.TYPE && returnType.isPrimitive()) {
				throw new AopInvocationException(
						"Null return value from advice does not match primitive return type for: " + method);
			}
			// Kotlin 协程支持，根据返回类型进行相应的协程处理
			if (KotlinDetector.isSuspendingFunction(method)) {
				return COROUTINES_FLOW_CLASS_NAME.equals(new MethodParameter(method, -1).getParameterType().getName()) ?
						CoroutinesUtils.asFlow(retVal) : CoroutinesUtils.awaitSingleOrNull(retVal, args[args.length - 1]);
			}
			return retVal;
		}
		finally {
			if (target != null && !targetSource.isStatic()) {
				// Must have come from TargetSource.
				// 如果是非静态 TargetSource，释放 target
				targetSource.releaseTarget(target);
			}
			if (setProxyContext) {
				// Restore old proxy.
				AopContext.setCurrentProxy(oldProxy);
			}
		}
	}


	/**
	 * Equality means interfaces, advisors and TargetSource are equal.
	 * <p>The compared object may be a JdkDynamicAopProxy instance itself
	 * or a dynamic proxy wrapping a JdkDynamicAopProxy instance.
	 */
	@Override
	public boolean equals(@Nullable Object other) {
		if (other == this) {
			return true;
		}
		if (other == null) {
			return false;
		}

		JdkDynamicAopProxy otherProxy;
		if (other instanceof JdkDynamicAopProxy jdkDynamicAopProxy) {
			otherProxy = jdkDynamicAopProxy;
		}
		else if (Proxy.isProxyClass(other.getClass())) {
			InvocationHandler ih = Proxy.getInvocationHandler(other);
			if (!(ih instanceof JdkDynamicAopProxy jdkDynamicAopProxy)) {
				return false;
			}
			otherProxy = jdkDynamicAopProxy;
		}
		else {
			// Not a valid comparison...
			return false;
		}

		// If we get here, otherProxy is the other AopProxy.
		return AopProxyUtils.equalsInProxy(this.advised, otherProxy.advised);
	}

	/**
	 * Proxy uses the hash code of the TargetSource.
	 */
	@Override
	public int hashCode() {
		return JdkDynamicAopProxy.class.hashCode() * 13 + this.advised.getTargetSource().hashCode();
	}

}
