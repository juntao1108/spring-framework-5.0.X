/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.beans.factory.annotation;

import javax.inject.Inject;
import javax.inject.Named;

import org.junit.Test;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.test.A;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.stereotype.Component;

import static org.junit.Assert.*;

public class BridgeMethodAutowiringTests {

	@Test
	public void SPR8434() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(AppConfig.class);
		// BeanDefinitionRegistryPostProcessor beanDefinitionRegistryPostProcessor = new BeanDefinitionRegistryPostProcessor() {
		// 	@Override
		// 	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		// 		System.out.println("自定义的的BeanDefinitionRegistryPostProcessor.postProcessBeanFactory-------执行了");
		// 	}
		//
		// 	@Override
		// 	public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
		// 		System.out.println("自定义的的BeanDefinitionRegistryPostProcessor.postProcessBeanDefinitionRegistry-------执行了");
		// 	}
		// };
		// BeanFactoryPostProcessor beanFactoryPostProcessor = new BeanFactoryPostProcessor() {
		// 	@Override
		// 	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		// 		System.out.println("自定义的的BeanFactoryPostProcessor.postProcessBeanFactory-------执行了");
		// 	}
		// };
		// ctx.addBeanFactoryPostProcessor(beanFactoryPostProcessor);
		// ctx.addBeanFactoryPostProcessor(beanDefinitionRegistryPostProcessor);
		ctx.refresh();
		System.out.println(ctx.getBean(A.class));
		// assertNotNull(ctx.getBean(A.class));
	}


	static abstract class GenericServiceImpl<D> {

		public abstract void setObject(D object);
	}


	public static class UserServiceImpl extends GenericServiceImpl<Foo> {

		protected Foo object;

		@Override
		@Inject
		@Named("userObject")
		public void setObject(Foo object) {
			if (this.object != null) {
				throw new IllegalStateException("Already called");
			}
			this.object = object;
		}
	}


	@Component("userObject")
	public static class Foo {
	}

}
