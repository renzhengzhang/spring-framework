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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.AjType;
import org.aspectj.lang.reflect.AjTypeSystem;
import org.aspectj.lang.reflect.PerClauseKind;

import org.springframework.aop.Pointcut;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.aop.aspectj.TypePatternClassFilter;
import org.springframework.aop.framework.AopConfigException;
import org.springframework.aop.support.ComposablePointcut;

/**
 * Metadata for an AspectJ aspect class, with an additional Spring AOP pointcut
 * for the per clause.
 *
 * <p>Uses AspectJ 5 AJType reflection API, enabling us to work with different
 * AspectJ instantiation models such as "singleton", "pertarget" and "perthis".
 *
 * <p>
 * 用于存储和管理一个 AspectJ 切面类的元数据信息。不仅支持对切面类的类型信息进行解析，还提供了与 Spring AOP 相关的额外功能，
 * 例如根据 {@link Aspect} 注解的 perClause 生成对应的 Pointcut
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 2.0
 * @see org.springframework.aop.aspectj.AspectJExpressionPointcut
 */
@SuppressWarnings("serial")
public class AspectMetadata implements Serializable {

	/**
	 * The name of this aspect as defined to Spring (the bean name) -
	 * allows us to determine if two pieces of advice come from the
	 * same aspect and hence their relative precedence.
	 *
	 * <p>
	 * Aspect 在 Spring 容器中的 beanName，用于判断多个 Advice 是否来自同一个 Aspect 并确定其优先级
	 */
	private final String aspectName;

	/**
	 * The aspect class, stored separately for re-resolution of the
	 * corresponding AjType on deserialization.
	 *
	 * <p>
	 * Aspect 类的实际类对象，存储此值是为了在反序列化时重新解析
	 */
	private final Class<?> aspectClass;

	/**
	 * AspectJ reflection information.
	 * <p>Re-resolved on deserialization since it isn't serializable itself.
	 *
	 * <p>
	 * 使用 AspectJ 的反射 API (AjType) 获取的切面类型信息，用于识别切面特性
	 */
	private transient AjType<?> ajType;

	/**
	 * Spring AOP pointcut corresponding to the per clause of the
	 * aspect. Will be the {@code Pointcut.TRUE} canonical instance in the
	 * case of a singleton, otherwise an AspectJExpressionPointcut.
	 *
	 * <p>
	 * 对应于切面 @Aspect 注解中定义的 perClause（实例化模型）的 Spring AOP Pointcut 表达式
	 * <ul>
	 *     <li>如果是单例模式，则使用 Pointcut.TRUE;</li>
	 *     <li>如果是 pertarget 或 perthis，则使用 AspectJExpressionPointcut</li>
	 *     <li>如果是 pertypewithin，则使用 ComposablePointcut 和 TypePatternClassFilter</li>
	 * </ul>
	 */
	private final Pointcut perClausePointcut;


	/**
	 * Create a new AspectMetadata instance for the given aspect class.
	 * @param aspectClass the aspect class
	 * @param aspectName the name of the aspect
	 */
	public AspectMetadata(Class<?> aspectClass, String aspectName) {
		this.aspectName = aspectName;

		// 遍历类继承链，找到第一个带有 @Aspect 注解的类，并获取其 AjType
		Class<?> currClass = aspectClass;
		AjType<?> ajType = null;
		while (currClass != Object.class) {
			AjType<?> ajTypeToCheck = AjTypeSystem.getAjType(currClass);
			if (ajTypeToCheck.isAspect()) {
				ajType = ajTypeToCheck;
				break;
			}
			currClass = currClass.getSuperclass();
		}
		if (ajType == null) {
			throw new IllegalArgumentException("Class '" + aspectClass.getName() + "' is not an @AspectJ aspect");
		}
		if (ajType.getDeclarePrecedence().length > 0) {
			throw new IllegalArgumentException("DeclarePrecedence not presently supported in Spring AOP");
		}
		this.aspectClass = ajType.getJavaClass();
		this.ajType = ajType;

		// 解析 @Aspect 注解中的 perClause，并根据不同的类型初始化 perClausePointcut
		switch (this.ajType.getPerClause().getKind()) {
			case SINGLETON -> {
				this.perClausePointcut = Pointcut.TRUE;
			}
			case PERTARGET, PERTHIS -> {
				AspectJExpressionPointcut ajexp = new AspectJExpressionPointcut();
				ajexp.setLocation(aspectClass.getName());
				ajexp.setExpression(findPerClause(aspectClass));
				ajexp.setPointcutDeclarationScope(aspectClass);
				this.perClausePointcut = ajexp;
			}
			case PERTYPEWITHIN -> {
				// Works with a type pattern
				this.perClausePointcut = new ComposablePointcut(new TypePatternClassFilter(findPerClause(aspectClass)));
			}
			default -> throw new AopConfigException(
					"PerClause " + ajType.getPerClause().getKind() + " not supported by Spring AOP for " + aspectClass);
		}
	}

	/**
	 * Extract contents from String of form {@code pertarget(contents)}.
	 *
	 * <p>
	 * 解析 @Aspect 注解的 value 值，提取其中的 perClause 内容（如 {@code pertarget(contents)} 中的 {@code contents}
	 */
	private String findPerClause(Class<?> aspectClass) {
		String str = aspectClass.getAnnotation(Aspect.class).value();
		int beginIndex = str.indexOf('(') + 1;
		int endIndex = str.length() - 1;
		return str.substring(beginIndex, endIndex);
	}


	/**
	 * Return AspectJ reflection information.
	 */
	public AjType<?> getAjType() {
		return this.ajType;
	}

	/**
	 * Return the aspect class.
	 */
	public Class<?> getAspectClass() {
		return this.aspectClass;
	}

	/**
	 * Return the aspect name.
	 */
	public String getAspectName() {
		return this.aspectName;
	}

	/**
	 * Return a Spring pointcut expression for a singleton aspect.
	 * (e.g. {@code Pointcut.TRUE} if it's a singleton).
	 */
	public Pointcut getPerClausePointcut() {
		return this.perClausePointcut;
	}

	/**
	 * Return whether the aspect is defined as "perthis" or "pertarget".
	 */
	public boolean isPerThisOrPerTarget() {
		PerClauseKind kind = getAjType().getPerClause().getKind();
		return (kind == PerClauseKind.PERTARGET || kind == PerClauseKind.PERTHIS);
	}

	/**
	 * Return whether the aspect is defined as "pertypewithin".
	 */
	public boolean isPerTypeWithin() {
		PerClauseKind kind = getAjType().getPerClause().getKind();
		return (kind == PerClauseKind.PERTYPEWITHIN);
	}

	/**
	 * Return whether the aspect needs to be lazily instantiated.
	 */
	public boolean isLazilyInstantiated() {
		return (isPerThisOrPerTarget() || isPerTypeWithin());
	}


	private void readObject(ObjectInputStream inputStream) throws IOException, ClassNotFoundException {
		inputStream.defaultReadObject();
		this.ajType = AjTypeSystem.getAjType(this.aspectClass);
	}

}
