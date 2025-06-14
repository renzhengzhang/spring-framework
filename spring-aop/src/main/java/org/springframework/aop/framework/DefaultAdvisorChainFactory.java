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
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.aopalliance.intercept.Interceptor;
import org.aopalliance.intercept.MethodInterceptor;

import org.springframework.aop.Advisor;
import org.springframework.aop.IntroductionAdvisor;
import org.springframework.aop.IntroductionAwareMethodMatcher;
import org.springframework.aop.MethodMatcher;
import org.springframework.aop.PointcutAdvisor;
import org.springframework.aop.framework.adapter.AdvisorAdapterRegistry;
import org.springframework.aop.framework.adapter.GlobalAdvisorAdapterRegistry;
import org.springframework.lang.Nullable;

/**
 * A simple but definitive way of working out an advice chain for a Method,
 * given an {@link Advised} object. Always rebuilds each advice chain;
 * caching can be provided by subclasses.
 *
 * @author Juergen Hoeller
 * @author Rod Johnson
 * @author Adrian Colyer
 * @since 2.0.3
 */
@SuppressWarnings("serial")
public class DefaultAdvisorChainFactory implements AdvisorChainFactory, Serializable {

	/**
	 * Singleton instance of this class.
	 * @since 6.0.10
	 */
	public static final DefaultAdvisorChainFactory INSTANCE = new DefaultAdvisorChainFactory();


	/**
	 * 将 Advised(Proxy Config) 中的 Advisor 转换为可执行的 MethodInterceptor chain
	 */
	@Override
	public List<Object> getInterceptorsAndDynamicInterceptionAdvice(
			Advised config, Method method, @Nullable Class<?> targetClass) {

		// This is somewhat tricky... We have to process introductions first,
		// but we need to preserve order in the ultimate list.

		// 获取 AdvisorAdapterRegistry，用于从 Advisor 中获取 MethodInterceptor
		AdvisorAdapterRegistry registry = GlobalAdvisorAdapterRegistry.getInstance();
		// 从 AOP 配置中获取所有 Advisor
		Advisor[] advisors = config.getAdvisors();
		List<Object> interceptorList = new ArrayList<>(advisors.length);
		// 确定实际类，用于 Pointcut 匹配
		Class<?> actualClass = (targetClass != null ? targetClass : method.getDeclaringClass());
		// 标记是否包含接口 Introduction，
		Boolean hasIntroductions = null;

		// 遍历所有 Advisors，筛选出适用于当前方法的 Advisor
		for (Advisor advisor : advisors) {
			if (advisor instanceof PointcutAdvisor pointcutAdvisor) {
				// PointcutAdvisor 处理（最常见的情况）
				// Add it conditionally.
				// 类级别过滤
				// 判断配置已经预过滤，以及 Pointcut 的类过滤器是否匹配目标类
				if (config.isPreFiltered() || pointcutAdvisor.getPointcut().getClassFilter().matches(actualClass)) {

					// 方法级别过滤
					// 类级别过滤通过，使用 Pointcut 的 MethodMatcher 进行匹配
					MethodMatcher mm = pointcutAdvisor.getPointcut().getMethodMatcher();
					boolean match;
					// IntroductionAwareMethodMatcher，考虑 Introduction 存在
					if (mm instanceof IntroductionAwareMethodMatcher iamm) {
						if (hasIntroductions == null) {
							hasIntroductions = hasMatchingIntroductions(advisors, actualClass);
						}
						match = iamm.matches(method, actualClass, hasIntroductions);
					}
					// 常规 MethodMatcher 匹配
					else {
						match = mm.matches(method, actualClass);
					}

					if (match) {
						// 匹配成功，使用 AdvisorAdapterRegistry 从 Advisor 中提取 MethodInterceptor
						MethodInterceptor[] interceptors = registry.getInterceptors(advisor);

						if (mm.isRuntime()) {
							// Creating a new object instance in the getInterceptors() method
							// isn't a problem as we normally cache created chains.
							// 动态方法匹配，运行时根据参数决定
							for (MethodInterceptor interceptor : interceptors) {
								interceptorList.add(new InterceptorAndDynamicMethodMatcher(interceptor, mm));
							}
						}
						else {
							// 静态方法匹配
							interceptorList.addAll(Arrays.asList(interceptors));
						}
					}
				}
			}
			else if (advisor instanceof IntroductionAdvisor ia) {
				// IntroductionAdvisor 处理
				// 匹配后直接添加 Interceptor，无需动态匹配
				if (config.isPreFiltered() || ia.getClassFilter().matches(actualClass)) {
					Interceptor[] interceptors = registry.getInterceptors(advisor);
					interceptorList.addAll(Arrays.asList(interceptors));
				}
			}
			else {
				// 其他类型 Advisor 处理
				// 直接添加 Interceptor，无需匹配
				Interceptor[] interceptors = registry.getInterceptors(advisor);
				interceptorList.addAll(Arrays.asList(interceptors));
			}
		}

		return interceptorList;
	}

	/**
	 * Determine whether the Advisors contain matching introductions.
	 */
	private static boolean hasMatchingIntroductions(Advisor[] advisors, Class<?> actualClass) {
		for (Advisor advisor : advisors) {
			if (advisor instanceof IntroductionAdvisor ia && ia.getClassFilter().matches(actualClass)) {
				return true;
			}
		}
		return false;
	}

}
