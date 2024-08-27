package net.foxgenesis.springJDA;

import net.foxgenesis.springJDA.impl.AbstractSpringJDA;

@FunctionalInterface
public interface SpringJDAFactory {
	AbstractSpringJDA createSpringJDA();
}
