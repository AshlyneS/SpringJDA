package net.foxgenesis.springJDA.context.impl;

import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

import net.foxgenesis.springJDA.context.SpringJDAContext;

public abstract class AbstractSpringJDAContext implements SpringJDAContext {

	public AbstractSpringJDAContext(@NonNull String token) {
		Assert.hasText(token, "Token can not be blank!");
	}
}
