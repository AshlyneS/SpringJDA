package net.foxgenesis.springJDA.context.impl;

import org.springframework.util.Assert;

import net.foxgenesis.springJDA.SpringJDAFactory;
import net.foxgenesis.springJDA.context.SpringJDAContext;

public abstract class AbstractSpringJDAContext implements SpringJDAContext, SpringJDAFactory {

	public AbstractSpringJDAContext(String token) {
		Assert.hasText(token, "Token can not be blank!");
	}
}
