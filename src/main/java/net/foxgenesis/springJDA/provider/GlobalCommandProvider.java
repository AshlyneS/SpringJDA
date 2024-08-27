package net.foxgenesis.springJDA.provider;

import net.dv8tion.jda.api.interactions.commands.build.CommandData;

@FunctionalInterface
public interface GlobalCommandProvider {
	CommandData getCommandData();
	
	static GlobalCommandProvider of(CommandData data) {
		return () -> data;
	}
}
