package net.foxgenesis.springJDA.provider;

import java.util.Collection;
import java.util.EnumSet;

import net.foxgenesis.springJDA.Scope;

public interface ScopeProvider {

	Collection<Scope> getScopes();
	
	static ScopeProvider of(Collection<Scope> scopes) {
		return () -> scopes;
	}
	
	static ScopeProvider of(Scope scope, Scope... scopes) {
		return of(EnumSet.of(scope, scopes));
	}
}
