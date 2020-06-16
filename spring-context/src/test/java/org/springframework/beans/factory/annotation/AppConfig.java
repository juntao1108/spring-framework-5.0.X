package org.springframework.beans.factory.annotation;

import org.springframework.beans.factory.annotation.test.C;
import org.springframework.beans.factory.annotation.test.D;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * @author sunjuntao
 * @create 2020-06-04 9:26
 * @description
 */
@Configuration
@ComponentScan("org.springframework.beans.factory.annotation.test")
public class AppConfig {

	// public static void main(String[] args) {
	// 	AnnotationConfigApplicationContext annotationConfigApplicationContext = new AnnotationConfigApplicationContext();
	// 	annotationConfigApplicationContext.register(A.class);
	// 	annotationConfigApplicationContext.refresh();
	//
	//
	//
	// }

	@Bean
	public C getC(){
		return new C();
	}

	@Bean
	public D getD(){
		getC();
		return new D();
	}

}
