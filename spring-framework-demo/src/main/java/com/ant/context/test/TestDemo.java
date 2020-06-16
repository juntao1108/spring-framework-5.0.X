package com.ant.context.test;

import com.ant.context.beanfactoryfostprocessor.MyBeanDefinitionRegistryPostProcessor;
import com.ant.context.config.AppConfig;
import com.ant.context.factorybean.TempFactoryBean;
import com.ant.context.factorybean.TestFactorybean;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * @author Ant
 * @ClassName: TestDemo
 * @Description: 读取spring5.源码入口
 * @datetime 2018/11/8 19:50
 * @Version 1.0
 */
public class TestDemo {
	public static void main(String[] args) {
		// 把spring的所有前提环境准备好
		// 1、准备工厂 == DefaultListableBeanFactory
		// 2、实例化一个bdReader 和一个 scanner
		AnnotationConfigApplicationContext annotationConfigApplicationContext
				= new AnnotationConfigApplicationContext();

//		ClassPathXmlApplicationContext classPathXmlApplicationContext =
//				new ClassPathXmlApplicationContext("xxx.xml");


		//测试自定义的BeanFactoryPostProcessor
//		BeanFactoryPostProcessor beanFactoryPostProcessor = new MyBeanFactoryPostProcessor();
//		annotationConfigApplicationContext.addBeanFactoryPostProcessor(beanFactoryPostProcessor);

		//测试我们自己定义的BeanDefinitionRegistryPostProcessor
//		BeanFactoryPostProcessor beanFactoryPostProcessor1 = new MyBeanDefinitionRegistryPostProcessor();
//		annotationConfigApplicationContext.addBeanFactoryPostProcessor(beanFactoryPostProcessor1);

		/**
		 * 这行代码是把我们自己定义的AppConfig.class的class类转换成bd,
		 * 然后put到我们的beanDifinitionMap当中去;
		 * beanDifinitonMap实际上是我们的DefaultListableBeanFactory的属性
		 * 那么我们的DefaultListableBeanFactory是怎么来的呢？在工厂中的什么位置呢？
		 **/

		annotationConfigApplicationContext.register(AppConfig.class);

		/**
		 * 初始化Spring的环境
		 */
		annotationConfigApplicationContext.refresh();

		TestFactorybean testFactoryBean = (TestFactorybean) annotationConfigApplicationContext.getBean("&testFactoryBean");
		TempFactoryBean tempFactoryBean = (TempFactoryBean) annotationConfigApplicationContext.getBean("testFactoryBean");
		testFactoryBean.testBean();
		tempFactoryBean.test();
//		System.out.println(annotationConfigApplicationContext.getBean("user"));
//
//		Dao dao = (Dao) annotationConfigApplicationContext.getBean("indexDao");
//		dao.query();

//		annotationConfigApplicationContext.getBean(IndexDao3.class).query();

//		Dao indexDao = (Dao) annotationConfigApplicationContext.getBean("indexDao");
//		System.out.println(indexDao.query());

//		UserService service = annotationConfigApplicationContext.getBean(UserService.class);
//		service.query();
	}
}
