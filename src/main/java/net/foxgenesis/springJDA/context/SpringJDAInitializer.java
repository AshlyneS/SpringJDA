package net.foxgenesis.springJDA.context;

@FunctionalInterface
public interface SpringJDAInitializer<C extends SpringJDAContext> {
	void initialize(C context);
}
