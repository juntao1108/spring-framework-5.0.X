/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.context.annotation;

import java.lang.annotation.Annotation;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.AnnotatedGenericBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionCustomizer;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.AutowireCandidateQualifier;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.core.env.Environment;
import org.springframework.core.env.EnvironmentCapable;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Convenient adapter for programmatic registration of annotated bean classes.
 * This is an alternative to {@link ClassPathBeanDefinitionScanner}, applying
 * the same resolution of annotations but for explicitly registered classes only.
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Sam Brannen
 * @author Phillip Webb
 * @since 3.0
 * @see AnnotationConfigApplicationContext#register
 */
public class AnnotatedBeanDefinitionReader {

	private final BeanDefinitionRegistry registry;

	private BeanNameGenerator beanNameGenerator = new AnnotationBeanNameGenerator();

	private ScopeMetadataResolver scopeMetadataResolver = new AnnotationScopeMetadataResolver();

	private ConditionEvaluator conditionEvaluator;


	/**
	 * Create a new {@code AnnotatedBeanDefinitionReader} for the given registry.
	 * If the registry is {@link EnvironmentCapable}, e.g. is an {@code ApplicationContext},
	 * the {@link Environment} will be inherited, otherwise a new
	 * {@link StandardEnvironment} will be created and used.
	 * @param registry the {@code BeanFactory} to load bean definitions into,
	 * in the form of a {@code BeanDefinitionRegistry}
	 * @see #AnnotatedBeanDefinitionReader(BeanDefinitionRegistry, Environment)
	 * @see #setEnvironment(Environment)
	 */

	/**
	 *  这里的BeanDefinitionRegistry registry是通过在AnnotationConfigApplicationContext
	 *  的构造方法中传进来的this
	 *  由此说明AnnotationConfigApplicationContext是一个BeanDefinitionRegistry类型的类
	 *  何以证明我们可以看到AnnotationConfigApplicationContext的类关系：
	 *  GenericApplicationContext extends AbstractApplicationContext implements BeanDefinitionRegistry
	 *  看到他实现了BeanDefinitionRegistry证明上面的说法，那么BeanDefinitionRegistry的作用是什么呢？
	 *  BeanDefinitionRegistry 顾名思义就是BeanDefinition的注册器
	 *  那么何为BeanDefinition呢？参考BeanDefinition的源码的注释
	 * @param registry
	 */
	public AnnotatedBeanDefinitionReader(BeanDefinitionRegistry registry) {
		this(registry, getOrCreateEnvironment(registry));
	}

	/**
	 * Create a new {@code AnnotatedBeanDefinitionReader} for the given registry and using
	 * the given {@link Environment}.
	 * @param registry the {@code BeanFactory} to load bean definitions into,
	 * in the form of a {@code BeanDefinitionRegistry}
	 * @param environment the {@code Environment} to use when evaluating bean definition
	 * profiles.
	 * @since 3.1
	 */
	public AnnotatedBeanDefinitionReader(BeanDefinitionRegistry registry, Environment environment) {
		Assert.notNull(registry, "BeanDefinitionRegistry must not be null");

		// registry实际上是我们传过来的AnnotationConfigApplicationContext（spring容器）
		// 它继承了BeanDefinitionRegistry接口；
		// 这里把它赋值给当前类定义的BeanDefinitionRegistry，用来做容器的初始化
		this.registry = registry;
		this.conditionEvaluator = new ConditionEvaluator(registry, environment, null);
		// 这个方法会定义6+1个处理我们bean的类，也就是注册6个BeanDefinition到spring工厂的BeanDefinitionMap中去
		AnnotationConfigUtils.registerAnnotationConfigProcessors(this.registry);
	}


	/**
	 * Return the BeanDefinitionRegistry that this scanner operates on.
	 */
	public final BeanDefinitionRegistry getRegistry() {
		return this.registry;
	}

	/**
	 * Set the Environment to use when evaluating whether
	 * {@link Conditional @Conditional}-annotated component classes should be registered.
	 * <p>The default is a {@link StandardEnvironment}.
	 * @see #registerBean(Class, String, Class...)
	 */
	public void setEnvironment(Environment environment) {
		this.conditionEvaluator = new ConditionEvaluator(this.registry, environment, null);
	}

	/**
	 * Set the BeanNameGenerator to use for detected bean classes.
	 * <p>The default is a {@link AnnotationBeanNameGenerator}.
	 */
	public void setBeanNameGenerator(@Nullable BeanNameGenerator beanNameGenerator) {
		this.beanNameGenerator = (beanNameGenerator != null ? beanNameGenerator : new AnnotationBeanNameGenerator());
	}

	/**
	 * Set the ScopeMetadataResolver to use for detected bean classes.
	 * <p>The default is an {@link AnnotationScopeMetadataResolver}.
	 */
	public void setScopeMetadataResolver(@Nullable ScopeMetadataResolver scopeMetadataResolver) {
		this.scopeMetadataResolver =
				(scopeMetadataResolver != null ? scopeMetadataResolver : new AnnotationScopeMetadataResolver());
	}


	/**
	 * 循环注册要处理的加了@Configuration的配置类
	 * Register one or more annotated classes to be processed.
	 * <p>Calls to {@code register} are idempotent; adding the same
	 * annotated class more than once has no additional effect.
	 * @param annotatedClasses one or more annotated classes,
	 * e.g. {@link Configuration @Configuration} classes
	 */
	public void register(Class<?>... annotatedClasses) {
		for (Class<?> annotatedClass : annotatedClasses) {
			registerBean(annotatedClass);
		}
	}

	/**
	 * Register a bean from the given bean class, deriving its metadata from
	 * class-declared annotations.
	 * @param annotatedClass the class of the bean
	 */
	public void registerBean(Class<?> annotatedClass) {
		doRegisterBean(annotatedClass, null, null, null);
	}

	/**
	 * Register a bean from the given bean class, deriving its metadata from
	 * class-declared annotations, using the given supplier for obtaining a new
	 * instance (possibly declared as a lambda expression or method reference).
	 * @param annotatedClass the class of the bean
	 * @param instanceSupplier a callback for creating an instance of the bean
	 * (may be {@code null})
	 * @since 5.0
	 */
	public <T> void registerBean(Class<T> annotatedClass, @Nullable Supplier<T> instanceSupplier) {
		doRegisterBean(annotatedClass, instanceSupplier, null, null);
	}

	/**
	 * Register a bean from the given bean class, deriving its metadata from
	 * class-declared annotations, using the given supplier for obtaining a new
	 * instance (possibly declared as a lambda expression or method reference).
	 * @param annotatedClass the class of the bean
	 * @param name an explicit name for the bean
	 * @param instanceSupplier a callback for creating an instance of the bean
	 * (may be {@code null})
	 * @since 5.0
	 */
	public <T> void registerBean(Class<T> annotatedClass, String name, @Nullable Supplier<T> instanceSupplier) {
		doRegisterBean(annotatedClass, instanceSupplier, name, null);
	}

	/**
	 * Register a bean from the given bean class, deriving its metadata from
	 * class-declared annotations.
	 * @param annotatedClass the class of the bean
	 * @param qualifiers specific qualifier annotations to consider,
	 * in addition to qualifiers at the bean class level
	 */
	@SuppressWarnings("unchecked")
	public void registerBean(Class<?> annotatedClass, Class<? extends Annotation>... qualifiers) {
		doRegisterBean(annotatedClass, null, null, qualifiers);
	}

	/**
	 * Register a bean from the given bean class, deriving its metadata from
	 * class-declared annotations.
	 * @param annotatedClass the class of the bean
	 * @param name an explicit name for the bean
	 * @param qualifiers specific qualifier annotations to consider,
	 * in addition to qualifiers at the bean class level
	 */
	@SuppressWarnings("unchecked")
	public void registerBean(Class<?> annotatedClass, String name, Class<? extends Annotation>... qualifiers) {
		doRegisterBean(annotatedClass, null, name, qualifiers);
	}

	/**
	 * 这个方法是把我们的注解的类注册到BeanDefinition中去
	 * Register a bean from the given bean class, deriving its metadata from
	 * class-declared annotations.
	 * @param annotatedClass the class of the bean
	 * @param instanceSupplier a callback for creating an instance of the bean
	 * (may be {@code null})
	 * @param name an explicit name for the bean
	 * @param qualifiers specific qualifier annotations to consider, if any,
	 * in addition to qualifiers at the bean class level
	 * @param definitionCustomizers one or more callbacks for customizing the
	 * factory's {@link BeanDefinition}, e.g. setting a lazy-init or primary flag
	 * @since 5.0
	 */
	<T> void doRegisterBean(Class<T> annotatedClass, @Nullable Supplier<T> instanceSupplier, @Nullable String name,
			@Nullable Class<? extends Annotation>[] qualifiers, BeanDefinitionCustomizer... definitionCustomizers) {

		/**   --------------   接下来这个方法中的过程实际上把bean转换成一个bd的过程 ----------------  **/


		/**
		 * 根据指定的bean创建一个AnnotatedGenericBeanDefinition
		 * 这个AnnotatedGenericBeanDefinition可以理解为一个数据结构
		 *
		 * BeanDefinition分为几个种类，前面初始化的spring的6个后置处理器的类型为RootBeanDefinition
		 * 这里是我们自己定义的Appconfig配置类，这里使用的是AnnotatedGenericBeanDefinition类型
		 * 但是他们都是BeanDefinition的子类
		 */
		AnnotatedGenericBeanDefinition abd = new AnnotatedGenericBeanDefinition(annotatedClass);

		/**
		 * 这句话判断类是否加了注解（查看类中是否有@Compotent @service  @Repository诸如此类的注解）
		 * 通过判断有无注解，spring来判断是否需要解析，如果没有加注解，则Spring会跳过解析
		 */
		if (this.conditionEvaluator.shouldSkip(abd.getMetadata())) {
			return;
		}
		//判读是否有方法回调
		abd.setInstanceSupplier(instanceSupplier);
		/**
		 * 得到bean类的作用域，默认为singleton
		 * 如果Appconfig上面加了@Scope(value = "prototype")
		 * 则spring会设定scopeMetadata的scopeName="prototype"
		 * 表示是使用原型，而不是使用单例
		 */
		ScopeMetadata scopeMetadata = this.scopeMetadataResolver.resolveScopeMetadata(abd);

		/**
		 * 把bean类的作用域添加到类定义中
		 */
		abd.setScope(scopeMetadata.getScopeName());

		/**
		 * 通过beanNameGenerator生成一个BeanName
		 */
		// 查看是否Bean有定义自己的beanName；如：@Configuration(value = "appConfig")、@Component(value = "appConfig")
	    // 如果有，则把程序员自己定义的beanName返回出去，否则则使用spring的默认规则（类名的首字母小写）
		String beanName = (name != null ? name : this.beanNameGenerator.generateBeanName(abd, this.registry));
		/**
		 * 处理类当中的通用注解
		 * 分析源码可以知道他主要处理
		 * Lazy DependsOn Primary Role等等注解
		 * 处理完成之后processCommonDefinitionAnnotations中依然是把他添加到数据结构当中
		 *
		 */
		AnnotationConfigUtils.processCommonDefinitionAnnotations(abd);

		/**
		 * 如果在向容器注册注解Bean定义时，使用了额外的限定符注解则解析
		 * 关于Qualifier和Primary，主要涉及到spring的自动装配
		 * 这里需要注意的
		 * byName和qualifiers这个变量是Annotation类型的数组，里面存不仅仅是Qualifier注解
		 * 理论上里面里面存的是一切注解，所以可以看到下面的代码spring去循环了这个数组
		 * 然后依次判断了注解当中是否包含了Primary，是否包含了Lazyd
		 */
		if (qualifiers != null) {
			for (Class<? extends Annotation> qualifier : qualifiers) {
				// @Primary注解的作用是当spring扫描到一个接口的两个实现类的时候，
				// 加了@Primary的bean将作为首选
				if (Primary.class == qualifier) {
					abd.setPrimary(true);
				}
				//懒加载，前面加过
				else if (Lazy.class == qualifier) {
					abd.setLazyInit(true);
				}
				else {
					//如果使用了除@Primary和@Lazy以外的其他注解，则为该Bean添加一个根据名字自动装配的限定符
					//这里难以理解，后面会详细介绍
					abd.addQualifier(new AutowireCandidateQualifier(qualifier));
				}
			}
		}
		// 处理spring和客户自定义的bean 暂时不知道是个什么鬼东西
		for (BeanDefinitionCustomizer customizer : definitionCustomizers) {
			customizer.customize(abd);
		}

		/**
		 * 定义一个BeanDefinitionHolder用来包装我们spring中beanDeintionMap结构中bd+beanName，
		 * 方便后面调用（后面会代码中会再分解开），没有实际意义
		 */
		BeanDefinitionHolder definitionHolder = new BeanDefinitionHolder(abd, beanName);

		/**
		 * 是否启用代理模式
		 */
		definitionHolder = AnnotationConfigUtils.applyScopedProxyMode(scopeMetadata, definitionHolder, this.registry);
		/** -------   从这一行往上的代码就是把一个bean变为一个bd（BeanDefinition）过程   --------- **/
		/**
		 * 把上述的这个数据结构注册给registry
		 * registy就是AnnotatonConfigApplicationContext
		 * AnnotatonConfigApplicationContext在初始化的時候通過調用父類的構造方法
		 * 實例化了一个DefaultListableBeanFactory
		 * *registerBeanDefinition里面就是把definitionHolder这个数据结构包含的信息注册到
		 * DefaultListableBeanFactory这个工厂
		 */
		BeanDefinitionReaderUtils.registerBeanDefinition(definitionHolder, this.registry);
	}


	/**
	 * Get the Environment from the given registry if possible, otherwise return a new
	 * StandardEnvironment.
	 */
	private static Environment getOrCreateEnvironment(BeanDefinitionRegistry registry) {
		Assert.notNull(registry, "BeanDefinitionRegistry must not be null");
		if (registry instanceof EnvironmentCapable) {
			return ((EnvironmentCapable) registry).getEnvironment();
		}
		return new StandardEnvironment();
	}

}
