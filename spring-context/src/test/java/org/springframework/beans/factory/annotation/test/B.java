package org.springframework.beans.factory.annotation.test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class B{

		@Autowired
		A a;

	}