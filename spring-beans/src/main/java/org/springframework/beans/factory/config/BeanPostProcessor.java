/*
 * Copyright 2002-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.beans.factory.config;

import org.springframework.beans.BeansException;
import org.springframework.lang.Nullable;

/**
 * BeanPostProcessor是spring框架的一个扩展的类点-后置处理器（有很多个）
 * 通过实现BeanPostProcessor接口，程序员自己可以插手bean的实例化过程
 *
 * 我们再写代码的过程中可以通过实现BeanPostProcessor这个接口，
 * 在Spring对bean进行实例化的过程中，实现对Bean的一系列操作，
 * 从而真正实现插手“她”的人生。进而减轻BeanFactory的负担；
 * 特别注意的一点是这个接口可以实现多个，实现一个列表，然后依次执行；
 * 比如SpringAOP就是通过Spring对bean实例化的后期，
 * 对bean进行一些横切逻辑的织入处理的，从而和IOC容器建立联系。
 *
 * spring默认给我们提供了很多postProcessor实现，下面是spring初始化给我们实例化的7个(
 *    5.x的高版本中去掉了1个,剩下了6了）
 *
 * 接口用起来很简单，但是底层的实现原理和源码实现是及其复杂的，初看会令人头皮发麻，
 * 后面我们来一一看这些接口到底是做什么用的，以及它的底层实现原理到底是什么？
 *
 * 常见的内置处理器如下：
 * 0 = {ApplicationContextAwareProcessor}:
 *     这个处理的功能是，当应用程序定义的Bean实现ApplicationContextAware
 *     接口时注入ApplicationContext对象；
 *     Application是spring的上下文，这里几乎涵盖所有的配置信息，
 *     环境变量，还有spring以及我们程序员自己定义的
 *     各种类的对象；
 * 1 = {InitDestroyAnnotationBeanPostProcessor}
 *     用来处理自定义的的的初始化方法和销毁方法,spring提供了3种自定义初始化和销毁方法：
 *     a: 通过@Bean指定init-method和destory-method属性
 *     b: Bean实现InitialzingBean接口和实现DisposableBean
 *     c: @PostConstruct , @PreDestroy
 * 2 = {InstantiationAwareBeanPostProcessor}
 *
 * 3 = {CommonAnnotationBeanPostProcessor@2008}
 * 4 = {AutowiredAnnotationBeanPostProcessor@2009}
 * 5 = {RequiredAnnotationBeanPostProcessor@2010}
 * 6 = {ApplicationListenerDetector@2011}
 *
 * Factory hook that allows for custom modification of new bean instances,
 * e.g. checking for marker interfaces or wrapping them with proxies.
 *
 * <p>ApplicationContexts can autodetect BeanPostProcessor beans in their
 * bean definitions and apply them to any beans subsequently created.
 * Plain bean factories allow for programmatic registration of post-processors,
 * applying to all beans created through this factory.
 *
 * <p>Typically, post-processors that populate beans via marker interfaces
 * or the like will implement {@link #postProcessBeforeInitialization},
 * while post-processors that wrap beans with proxies will normally
 * implement {@link #postProcessAfterInitialization}.
 *
 * @author Juergen Hoeller
 * @since 10.10.2003
 * @see InstantiationAwareBeanPostProcessor
 * @see DestructionAwareBeanPostProcessor
 * @see ConfigurableBeanFactory#addBeanPostProcessor
 * @see BeanFactoryPostProcessor
 */
public interface  BeanPostProcessor {

	/**
	 * 在bean的初始化之前执行
	 * Apply this BeanPostProcessor to the given new bean instance <i>before</i> any bean
	 * initialization callbacks (like InitializingBean's {@code afterPropertiesSet}
	 * or a custom init-method). The bean will already be populated with property values.
	 * The returned bean instance may be a wrapper around the original.
	 * <p>The default implementation returns the given {@code bean} as-is.
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
	 * 在bean的初始化后执行
	 * Apply this BeanPostProcessor to the given new bean instance <i>after</i> any bean
	 * initialization callbacks (like InitializingBean's {@code afterPropertiesSet}
	 * or a custom init-method). The bean will already be populated with property values.
	 * The returned bean instance may be a wrapper around the original.
	 * <p>In case of a FactoryBean, this callback will be invoked for both the FactoryBean
	 * instance and the objects created by the FactoryBean (as of Spring 2.0). The
	 * post-processor can decide whether to apply to either the FactoryBean or created
	 * objects or both through corresponding {@code bean instanceof FactoryBean} checks.
	 * <p>This callback will also be invoked after a short-circuiting triggered by a
	 * {@link InstantiationAwareBeanPostProcessor#postProcessBeforeInstantiation} method,
	 * in contrast to all other BeanPostProcessor callbacks.
	 * <p>The default implementation returns the given {@code bean} as-is.
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
