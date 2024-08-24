package net.foxgenesis.springJDA;

import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import net.dv8tion.jda.api.interactions.commands.Command;

public interface CommandRegistry extends Iterable<Command> {

	default Optional<Command> getCommandByName(String name) {
		return findCommand(command -> command.getName().equalsIgnoreCase(name));
	}

	default Optional<Command> findCommand(Predicate<Command> filter) {
		for (Command command : this)
			if (filter.test(command))
				return Optional.of(command);
		return Optional.empty();
	}
	
	default Stream<Command> stream() {
		return StreamSupport.stream(spliterator(), false);
	}
}
