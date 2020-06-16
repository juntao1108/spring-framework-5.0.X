package com.ant.aop.dao;

import org.springframework.stereotype.Component;

/**
 * @ClassName OrderDaoImpl
 * @Description OrderDaoImpl
 * @Author Ant
 * @Date 2019-05-25 18:32
 * @Version 1.0
 **/

@Component
public class OrderDaoImpl implements OrderDao{
	@Override
	public void query() {
		try {
			System.out.println("原始逻辑");
		} catch (Exception e) {
			System.out.println("wucoa");
			e.printStackTrace();
		}
	}
}
