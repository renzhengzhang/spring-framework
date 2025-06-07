/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.instrument.classloading;

import java.lang.instrument.ClassFileTransformer;

/**
 * Defines the contract for adding one or more
 * {@link ClassFileTransformer ClassFileTransformers} to a {@link ClassLoader}.
 *
 * <p>Implementations may operate on the current context {@code ClassLoader}
 * or expose their own instrumental {@code ClassLoader}.
 *
 * <p>
 * LoadTimeWeaver 定义了加载时织入（Load-Time Weaving，LTW）的契约，作为 Spring AOP 和 AspectJ 集成的核心组件之一。
 *
 * @author Rod Johnson
 * @author Costin Leau
 * @since 2.0
 * @see java.lang.instrument.ClassFileTransformer
 */
public interface LoadTimeWeaver {

	/**
	 * Add a {@code ClassFileTransformer} to be applied by this
	 * {@code LoadTimeWeaver}.
	 *
	 * <p>
	 * 添加一个 ClassFileTransformer 到当前的 LoadTimeWeaver，ClassFileTransformer 会在类加载时对字节码进行修改
	 *
	 * @param transformer the {@code ClassFileTransformer} to add
	 */
	void addTransformer(ClassFileTransformer transformer);

	/**
	 * Return a {@code ClassLoader} that supports instrumentation
	 * through AspectJ-style load-time weaving based on user-defined
	 * {@link ClassFileTransformer ClassFileTransformers}.
	 * <p>May be the current {@code ClassLoader}, or a {@code ClassLoader}
	 * created by this {@link LoadTimeWeaver} instance.
	 * @return the {@code ClassLoader} which will expose
	 * instrumented classes according to the registered transformers
	 *
	 * <p>
	 * 返回一个支持通过以 AspectJ 风格的加载时织入进行类增强的 ClassLoader，
	 * 可能是当前的 ClassLoader，也可能是由 LoadTimeWeaver 实例创建的新 ClassLoader
	 */
	ClassLoader getInstrumentableClassLoader();

	/**
	 * Return a throwaway {@code ClassLoader}, enabling classes to be
	 * loaded and inspected without affecting the parent {@code ClassLoader}.
	 * <p>Should <i>not</i> return the same instance of the {@link ClassLoader}
	 * returned from an invocation of {@link #getInstrumentableClassLoader()}.
	 * @return a temporary throwaway {@code ClassLoader}; should return
	 * a new instance for each call, with no existing state
	 *
	 * <p>
	 * 返回一个临时的、一次性的 ClassLoader，用于加载和检查类而不影响 parent ClassLoader
	 * 每次调用都应该返回一个新的实例，不包含任何现有状态
	 */
	ClassLoader getThrowawayClassLoader();

}
