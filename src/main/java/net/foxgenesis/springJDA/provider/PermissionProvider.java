package net.foxgenesis.springJDA.provider;

import java.util.Collection;
import java.util.EnumSet;

import net.dv8tion.jda.api.Permission;

public interface PermissionProvider {

	Collection<Permission> getPermissions();
	
	static PermissionProvider of(Collection<Permission> permissions) {
		return () -> permissions;
	}
	
	static PermissionProvider of(Permission permission, Permission... permissions) {
		return of(EnumSet.of(permission, permissions));
	}
}
