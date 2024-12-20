package net.foxgenesis.springJDA.provider;

import java.util.Set;

import net.dv8tion.jda.api.interactions.commands.build.CommandData;

@FunctionalInterface
public interface GlobalCommandProvider {
	Set<CommandData> getCommandData();

	static GlobalCommandProvider of(CommandData... data) {
		return () -> Set.of(data);
	}
}
