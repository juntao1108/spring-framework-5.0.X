/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.context.support;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.MergedBeanDefinitionPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.OrderComparator;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.lang.Nullable;

/**
 * Delegate for AbstractApplicationContext's post-processor handling.
 *
 * @author Juergen Hoeller
 * @since 4.0
 */
final class PostProcessorRegistrationDelegate {

	public static void invokeBeanFactoryPostProcessors(
			ConfigurableListableBeanFactory beanFactory, List<BeanFactoryPostProcessor> beanFactoryPostProcessors) {

		// Invoke BeanDefinitionRegistryPostProcessors first, if any.
		Set<String> processedBeans = new HashSet<>();

		/**
		 * 这里是循环我们自己定义的beanFactoryPostProcessors,这里应该我们程序员自己写的，也就是实现BeanFactoryProcessor,
		 * 并且调用annotationConfigApplicationContext.addBeanFactoryPostProcessor(beanFactoryPostProcessor);这个方法放进来的；
		 *
		 */
		// 注意这里判断beanFactory实例的时候为什么是true，因为beanFactory在前面实例话的时候返回的是一个DefaultListableBeanFactory实例
		// 而DefaultListableBeanFactory是继承了BeanDefinitionRegistry，所以这里返回是true
		if (beanFactory instanceof BeanDefinitionRegistry) {
			BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;

			// 存放我们程序员自己定义的常规的实现BeanFactoryPostProcessor接口的beanFactoryPostProcessor
			List<BeanFactoryPostProcessor> regularPostProcessors = new ArrayList<>();
			// 存放我们程序员自己定义的实现BeanDefinitionRegistryPostProcessor接口的beanFactroyPostProcessor
			// 这个后置处理器的作用要远比BeanFactoryPostProcessor要强大,这个后置处理器可以拿到BeanDefinitionRegistry
			// 拿到这个对象说明我们可以手动的相bean工厂注册自己的对象，mybatis就是利用的这一点去把它的对象注册到spring容器中来交给spring管理的
			List<BeanDefinitionRegistryPostProcessor> registryProcessors = new ArrayList<>();

			// 这里循环处理我们程序员自己定义的对bean工厂操作的后置处理器（这里的后置处理器是直接放到spring的容器当中的，它不是放到beanFatory中的）
			for (BeanFactoryPostProcessor postProcessor : beanFactoryPostProcessors) {
				if (postProcessor instanceof BeanDefinitionRegistryPostProcessor) {
					BeanDefinitionRegistryPostProcessor registryProcessor =
							(BeanDefinitionRegistryPostProcessor) postProcessor;

					// 如果这个地方定义了一个处理器，则会调用registryProcessor的postProcessBeanDefinitionRegistry(registry)
					// 方法，这个地方是调用我们程序员自己定义的registryProcessor的postProcessBeanDefinitionRegistry回调方法，
					// 如果我们没有定义这样一个后置处理器，则会跳过
					// 这里会增强benFactoryProcessor的一些功能
					// 为什么会在这里执行回调函数，因为BeanDefinitionRegistryPostProcessor这个后置处理器是参与bean工厂的构建的
					// 而不是参与bean实例话构建，所以这里直接执行了回调函数
					registryProcessor.postProcessBeanDefinitionRegistry(registry);

					registryProcessors.add(registryProcessor);
				} else {
					// 这里大家有没有疑问？我们程序员自己定义的实现BeanFactoryPostProcessor和BeanDefinitionRegistryPostProcessor这两个接口的类
					// 为什么上面的判断只判断是否BeanDefinitionRegistryPostProcessor这个类，
					// 并且执行了postProcessBeanDefinitionRegistry(registry)方法
					// 但是不判断是否是属于BeanFactoryPostProcessor这个类？？而是直接放到集合中去
					// 甭急，咱们先往下看......
					regularPostProcessors.add(postProcessor);
				}
			}

			// Do not initialize FactoryBeans here: We need to leave all regular beans
			// uninitialized to let the bean factory post-processors apply to them!
			// Separate between BeanDefinitionRegistryPostProcessors that implement
			// PriorityOrdered, Ordered, and the rest.
			//这个currentRegistryProcessors 放的是spring内部自己实现了BeanDefinitionRegistryPostProcessor接口的对象

			// 这个地方又定义了一个List<BeanDefinitionRegistryPostProcessor>,
			// 这个List主要是维护spring自己实现了BeanDefinitionRegistryPostProcessor接口的对象
			// 其实这里实现BeanDefinitionRegistryPostProcessor接口的只有一个，就是那个最重要的db
			// 这个类ConfigurationClassPostProcessor，名字叫做internalConfigurationAnnotationProcessor
			// 是不是很熟悉？？？哈哈
			//ConfigurationClassPostProcessor这个类其实是实现了BeanDefinitionRegistryPostProcessor
			List<BeanDefinitionRegistryPostProcessor> currentRegistryProcessors = new ArrayList<>();

			// First, invoke the BeanDefinitionRegistryPostProcessors that implement PriorityOrdered.
			// 这里我在第一次看到的时候，我认为这个数组肯定是0，因为我们从来没有在beanFactory中放入过后置处理器，其实不然，
			// 这里我们在初始化容器的时候，就已经放入了6个，就是在最开始new容器的时候；因为spring实在用了太多的类继承和接口继承，
			// 所以要理清楚这段逻辑，还需要从开始的那段代码再理一下
			String[] postProcessorNames =
					beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
			//这个地方可以得到一个BeanFactoryPostProcessor，因为是spring默认在最开始自己注册的
			//为什么要在最开始注册这个呢？
			//因为spring的工厂需要许解析去扫描等等功能
			//而这些功能都是需要在spring工厂初始化完成之前执行
			//要么在工厂最开始的时候、要么在工厂初始化之中，反正不能再之后
			//因为如果在之后就没有意义，因为那个时候已经需要使用工厂了
			//所以这里spring'在一开始就注册了一个BeanFactoryPostProcessor，用来插手springfactory的实例化过程
			//在这个地方断点可以知道这个类叫做ConfigurationClassPostProcessor
			//ConfigurationClassPostProcessor那么这个类能干嘛呢？可以参考源码
			//下面我们对这个牛逼哄哄的类（他能插手spring工厂的实例化过程还不牛逼吗？）重点解释
			for (String ppName : postProcessorNames) {
				if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
					currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
					processedBeans.add(ppName);
				}
			}
			//排序不重要，况且currentRegistryProcessors这里也只有一个数据
			sortPostProcessors(currentRegistryProcessors, beanFactory);

			// 这里是把spring的和我们定义的（这里是指加了注解交给spring管理的，不包括我们自己通过实现接口不加注解）合并
			// 别问为为啥合并，应为他们都是实现BeanDefinitionRegistryPostProcessor这个接口的
			registryProcessors.addAll(currentRegistryProcessors);
			//最重要。注意这里是方法调用
			//执行所有BeanDefinitionRegistryPostProcessor

			/**
			 * 重中之重
			 * 1.最重要，注意这里是调用方法,注意这里的参数currentRegistryProcessors，
			 *   它传过来的是spring自己的BeanFactoryPostProcessor，
			 *   为什么没有把我们程序员自己定义的传进去那？
			 *   （因为我们程序员自己定义的上面已经处理过了）86行代码调用的
			 *
			 *   扫描类就是在这个方法中完成的
			 */
			invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry);

			// 好了，到此为止，包括我们程序员自己定义的以及spring的所有的BeanDefinitionRegistryPostProcessor.postProcessBeanDefinitionRegistry()方法都已经执行完成了
			// 下边要执行我们自定义的的BeanFactoryPostProcessor和spring中的BeanDefinitionRegistryPostProcessor接口中的postProcessBeanFactory()方法
			/***********************************************************************************************************************/

			// 因为currentRegistryProcessors已经合并到了registryProcessors，
			// 并且也已经执行完了，也是为后面判断排序做处理，所以这里清理了
			currentRegistryProcessors.clear();

			//获取所有的的BeanDefinitionRegistryPostProcessor子类判断是否是实现Ordered接口的，前面执行的是实现PriorityOrdered接口的
			// PriorityOrdered实现这个接口优先执行，所有现在执行实现Ordered的，这里spring内部我程序员自定义的都没有
			// Next, invoke the BeanDefinitionRegistryPostProcessors that implement Ordered.
			postProcessorNames = beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
			for (String ppName : postProcessorNames) {
				if (!processedBeans.contains(ppName) && beanFactory.isTypeMatch(ppName, Ordered.class)) {
					currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
					processedBeans.add(ppName);
				}
			}
			//这里和上面的方法一模一样，只是如果上面判断有排序
			//currentRegistryProcessors这个结果集才有内容
			sortPostProcessors(currentRegistryProcessors, beanFactory);
			registryProcessors.addAll(currentRegistryProcessors);
			invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry);
			currentRegistryProcessors.clear();

			//这里执行除了上面以外的BeanDefinitionRegistryPostProcessor，这里也是没有
			// Finally, invoke all other BeanDefinitionRegistryPostProcessors until no further ones appear.
			boolean reiterate = true;
			while (reiterate) {
				reiterate = false;
				postProcessorNames = beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
				for (String ppName : postProcessorNames) {
					if (!processedBeans.contains(ppName)) {
						currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
						processedBeans.add(ppName);
						reiterate = true;
					}
				}
				sortPostProcessors(currentRegistryProcessors, beanFactory);

				registryProcessors.addAll(currentRegistryProcessors);

				invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry);

				currentRegistryProcessors.clear();
			}

			// 上面的执行完了程序员自己定义的以及spring自己的实现了BeanDefinitionRegistryPostProcessor的后置处理器中的postProcessBeanDefinitionRegistry方法
			// 这里就是执行spring和程序员自己的BeanDefinitionRegistryPostProcessor类中的postProcessBeanFactory()方法
			// Now, invoke the postProcessBeanFactory callback of all processors handled so far.
			// 因为BeanFactoryPostProcessor中有一个postProcessBeanFactory
			// 所以实现了继承BeanFactoryPostProcessor子类BeanDefinitionRegistryPostProcessor的后置处理器类也要执行BeanFactoryPostProcessor中的postProcessBeanFactory方法
			invokeBeanFactoryPostProcessors(registryProcessors, beanFactory);

			//处理程序员自己定义的实现了BeanFactoryPostProcessor.postProcessBeanFactory()中的类的后置处理器类
			invokeBeanFactoryPostProcessors(regularPostProcessors, beanFactory);
		}

		else {
			// Invoke factory processors registered with the context instance.
			invokeBeanFactoryPostProcessors(beanFactoryPostProcessors, beanFactory);
		}

		// Do not initialize FactoryBeans here: We need to leave all regular beans
		// uninitialized to let the bean factory post-processors apply to them!
		String[] postProcessorNames =
				beanFactory.getBeanNamesForType(BeanFactoryPostProcessor.class, true, false);

		// Separate between BeanFactoryPostProcessors that implement PriorityOrdered,
		// Ordered, and the rest.
		List<BeanFactoryPostProcessor> priorityOrderedPostProcessors = new ArrayList<>();
		List<String> orderedPostProcessorNames = new ArrayList<>();
		List<String> nonOrderedPostProcessorNames = new ArrayList<>();
		for (String ppName : postProcessorNames) {
			if (processedBeans.contains(ppName)) {
				// skip - already processed in first phase above
			}
			else if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
				priorityOrderedPostProcessors.add(beanFactory.getBean(ppName, BeanFactoryPostProcessor.class));
			}
			else if (beanFactory.isTypeMatch(ppName, Ordered.class)) {
				orderedPostProcessorNames.add(ppName);
			}
			else {
				nonOrderedPostProcessorNames.add(ppName);
			}
		}

		// First, invoke the BeanFactoryPostProcessors that implement PriorityOrdered.
		sortPostProcessors(priorityOrderedPostProcessors, beanFactory);
		invokeBeanFactoryPostProcessors(priorityOrderedPostProcessors, beanFactory);

		// Next, invoke the BeanFactoryPostProcessors that implement Ordered.
		List<BeanFactoryPostProcessor> orderedPostProcessors = new ArrayList<>();
		for (String postProcessorName : orderedPostProcessorNames) {
			orderedPostProcessors.add(beanFactory.getBean(postProcessorName, BeanFactoryPostProcessor.class));
		}
		sortPostProcessors(orderedPostProcessors, beanFactory);
		invokeBeanFactoryPostProcessors(orderedPostProcessors, beanFactory);

		// Finally, invoke all other BeanFactoryPostProcessors.
		List<BeanFactoryPostProcessor> nonOrderedPostProcessors = new ArrayList<>();
		for (String postProcessorName : nonOrderedPostProcessorNames) {
			nonOrderedPostProcessors.add(beanFactory.getBean(postProcessorName, BeanFactoryPostProcessor.class));
		}
		invokeBeanFactoryPostProcessors(nonOrderedPostProcessors, beanFactory);

		// Clear cached merged bean definitions since the post-processors might have
		// modified the original metadata, e.g. replacing placeholders in values...
		beanFactory.clearMetadataCache();
	}

	public static void registerBeanPostProcessors(
			ConfigurableListableBeanFactory beanFactory, AbstractApplicationContext applicationContext) {

		// 从bean工厂中拿到后置处理器的名称
		// 后面会根据名称循环找到继承了BeanPostProcessor的类，
		// 然后把它放到后置处理器的list中去
		String[] postProcessorNames = beanFactory.getBeanNamesForType(BeanPostProcessor.class, true, false);

		// 注册BeanPostProcessorChecker，这个意思是bean在BeanPostProcessor实例化过程中创建时，
		// 如果当前的bean没有找到和自己对应的后置处理器(BeanPostProcessor)处理时，则进行信息标识
		// 这个应该是不重要，不是Spring的核心，暂且先忽略掉。
		int beanProcessorTargetCount = beanFactory.getBeanPostProcessorCount() + 1 + postProcessorNames.length;
		beanFactory.addBeanPostProcessor(new BeanPostProcessorChecker(beanFactory, beanProcessorTargetCount));

		// 分别定义两个BeanPostProcessor集合，分别用来存放BeanPostProcessors，和PriorityOrdered
		// 两种类型的BeanPostProcessor
		// Separate between BeanPostProcessors that implement PriorityOrdered,
		// Ordered, and the rest.优先排序的后置处理器，下边的集合是内部后置处理器
		List<BeanPostProcessor> priorityOrderedPostProcessors = new ArrayList<>();
		List<BeanPostProcessor> internalPostProcessors = new ArrayList<>();

		List<String> orderedPostProcessorNames = new ArrayList<>();
		List<String> nonOrderedPostProcessorNames = new ArrayList<>();
		for (String ppName : postProcessorNames) {
			if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
				BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
				priorityOrderedPostProcessors.add(pp);
				if (pp instanceof MergedBeanDefinitionPostProcessor) {
					internalPostProcessors.add(pp);
				}
			}
			else if (beanFactory.isTypeMatch(ppName, Ordered.class)) {
				orderedPostProcessorNames.add(ppName);
			}
			else {
				nonOrderedPostProcessorNames.add(ppName);
			}
		}

		// First, register the BeanPostProcessors that implement PriorityOrdered.
		sortPostProcessors(priorityOrderedPostProcessors, beanFactory);
		registerBeanPostProcessors(beanFactory, priorityOrderedPostProcessors);

		// Next, register the BeanPostProcessors that implement Ordered.
		List<BeanPostProcessor> orderedPostProcessors = new ArrayList<>();
		for (String ppName : orderedPostProcessorNames) {
			BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
			orderedPostProcessors.add(pp);
			if (pp instanceof MergedBeanDefinitionPostProcessor) {
				internalPostProcessors.add(pp);
			}
		}
		sortPostProcessors(orderedPostProcessors, beanFactory);
		registerBeanPostProcessors(beanFactory, orderedPostProcessors);

		/**
		 * 现在，注册所有常规的bean postprocessor(后置处理器)。
		 */
		List<BeanPostProcessor> nonOrderedPostProcessors = new ArrayList<>();
		for (String ppName : nonOrderedPostProcessorNames) {
			BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
			nonOrderedPostProcessors.add(pp);
			if (pp instanceof MergedBeanDefinitionPostProcessor) {
				internalPostProcessors.add(pp);
			}
		}
		registerBeanPostProcessors(beanFactory, nonOrderedPostProcessors);

		// Finally, re-register all internal BeanPostProcessors.
		sortPostProcessors(internalPostProcessors, beanFactory);
		registerBeanPostProcessors(beanFactory, internalPostProcessors);

		// Re-register post-processor for detecting inner beans as ApplicationListeners,
		// moving it to the end of the processor chain (for picking up proxies etc).
		beanFactory.addBeanPostProcessor(new ApplicationListenerDetector(applicationContext));
	}

	private static void sortPostProcessors(List<?> postProcessors, ConfigurableListableBeanFactory beanFactory) {
		Comparator<Object> comparatorToUse = null;
		if (beanFactory instanceof DefaultListableBeanFactory) {
			comparatorToUse = ((DefaultListableBeanFactory) beanFactory).getDependencyComparator();
		}
		if (comparatorToUse == null) {
			comparatorToUse = OrderComparator.INSTANCE;
		}
		postProcessors.sort(comparatorToUse);
	}

	/**
	 * Invoke the given BeanDefinitionRegistryPostProcessor beans.
	 * 注意对比下面这个方法
	 * BeanDefinitionRegistryPostProcessor和BeanFactoryPostProcessor
	 */
	private static void invokeBeanDefinitionRegistryPostProcessors(
			Collection<? extends BeanDefinitionRegistryPostProcessor> postProcessors, BeanDefinitionRegistry registry) {

		// 这里循环spring自己定义的BeanDefinitionRegistryPostProcessor，这里是只有一个：ConfigurationClassPostProcessor
		// 这里其实就是执行回调函数
		for (BeanDefinitionRegistryPostProcessor postProcessor : postProcessors) {
			postProcessor.postProcessBeanDefinitionRegistry(registry);
		}
	}

	/**
	 * Invoke the given BeanFactoryPostProcessor beans.
	 */
	private static void invokeBeanFactoryPostProcessors(
			Collection<? extends BeanFactoryPostProcessor> postProcessors, ConfigurableListableBeanFactory beanFactory) {

		// 这里就是执行beanFactoryPostProcessors的回调方法
		// 比如程序员自己写的实现了BeanFactoryPostProcessors的类，
		// 重写的postProcessBeanFactory方法将会在这里一一执行；
		for (BeanFactoryPostProcessor postProcessor : postProcessors) {
			postProcessor.postProcessBeanFactory(beanFactory);
		}
	}

	/**
	 * Register the given BeanPostProcessor beans.
	 */
	private static void registerBeanPostProcessors(
			ConfigurableListableBeanFactory beanFactory, List<BeanPostProcessor> postProcessors) {

		for (BeanPostProcessor postProcessor : postProcessors) {
			beanFactory.addBeanPostProcessor(postProcessor);
		}
	}


	/**
	 * BeanPostProcessor that logs an info message when a bean is created during
	 * BeanPostProcessor instantiation, i.e. when a bean is not eligible for
	 * getting processed by all BeanPostProcessors.
	 * 当Spring的配置中的后处理器还没有被注册就已经开始了bean的初始化
	 *	便会打印出BeanPostProcessorChecker中设定的信息
	 */
	private static final class BeanPostProcessorChecker implements BeanPostProcessor {

		private static final Log logger = LogFactory.getLog(BeanPostProcessorChecker.class);

		private final ConfigurableListableBeanFactory beanFactory;

		private final int beanPostProcessorTargetCount;

		public BeanPostProcessorChecker(ConfigurableListableBeanFactory beanFactory, int beanPostProcessorTargetCount) {
			this.beanFactory = beanFactory;
			this.beanPostProcessorTargetCount = beanPostProcessorTargetCount;
		}

		@Override
		public Object postProcessBeforeInitialization(Object bean, String beanName) {
			return bean;
		}

		@Override
		public Object postProcessAfterInitialization(Object bean, String beanName) {
			if (!(bean instanceof BeanPostProcessor) && !isInfrastructureBean(beanName) &&
					this.beanFactory.getBeanPostProcessorCount() < this.beanPostProcessorTargetCount) {
				if (logger.isInfoEnabled()) {
					logger.info("Bean '" + beanName + "' of type [" + bean.getClass().getName() +
							"] is not eligible for getting processed by all BeanPostProcessors " +
							"(for example: not eligible for auto-proxying)");
				}
			}
			return bean;
		}

		private boolean isInfrastructureBean(@Nullable String beanName) {
			if (beanName != null && this.beanFactory.containsBeanDefinition(beanName)) {
				BeanDefinition bd = this.beanFactory.getBeanDefinition(beanName);
				return (bd.getRole() == RootBeanDefinition.ROLE_INFRASTRUCTURE);
			}
			return false;
		}
	}

}
