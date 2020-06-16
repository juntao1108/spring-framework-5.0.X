package org.springframework.beans.factory.annotation.test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class A{

		@Autowired
		B b;

	// public A(B b) {
	// }
}