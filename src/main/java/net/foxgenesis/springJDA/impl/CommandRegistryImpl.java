package net.foxgenesis.springJDA.impl;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.foxgenesis.springJDA.CommandRegistry;
import net.foxgenesis.springJDA.SpringJDA;
import net.foxgenesis.springJDA.annotation.GlobalCommand;
import net.foxgenesis.springJDA.autoconfigure.SpringJDAConfiguration;
import net.foxgenesis.springJDA.event.SpringJDASemiReadyEvent;
import net.foxgenesis.springJDA.provider.GlobalCommandProvider;

@Service
@EnableConfigurationProperties(SpringJDAConfiguration.class)
public class CommandRegistryImpl
		implements CommandRegistry, ApplicationListener<SpringJDASemiReadyEvent> {
	private final Logger logger = LoggerFactory.getLogger(CommandRegistry.class);

	@Autowired
	private SpringJDAConfiguration config;

	@Autowired
	private SpringJDA jda;

	@Autowired
	private ApplicationContext ctx;

	private CopyOnWriteArraySet<Command> commands;

	@Override
	public void onApplicationEvent(SpringJDASemiReadyEvent event) {
		if (config.updateCommands()) {
			Set<CommandData> commandData = new HashSet<>();

			logger.info("Scanning for global commands");
			// Get commands
			getCommands()
					// Iterate over commands
					.forEach(command -> {
						// Attempt to add command
						if (!commandData.add(command))
							throw new IllegalArgumentException("command " + command + " is already registered");
					});
			logger.info("Uploading {} commands to Discord: " + commandData.size(),
					commandData.stream().map(CommandData::getName).sorted().toList());

			this.commands = new CopyOnWriteArraySet<>(jda.updateCommands().addCommands(commandData).complete());
		} else {
			logger.info("Requesting commands from Discord");
			this.commands = new CopyOnWriteArraySet<>(jda.retrieveCommands().complete());
		}

		logger.info("Loaded {} commands", commands.size());
	}

	private Stream<CommandData> getCommands() {
		Stream<CommandData> providers = ctx
				// Get GlobalCommandProvider provider
				.getBeanProvider(GlobalCommandProvider.class)
				// Stream
				.stream()
				// Get command data
				.map(GlobalCommandProvider::getCommandData);
		// If annotation configuration is enabled, get and merge @GlobalCommand beans
		return config.annotationConfiguration() ? Stream.concat(providers, getGlobalCommandAnnotationBeans())
				: providers;
	}

	private Stream<CommandData> getGlobalCommandAnnotationBeans() {
		Map<String, Object> beans = ctx.getBeansWithAnnotation(GlobalCommand.class);
		// Check that all beans are of correct type
		beans.forEach((key, bean) -> Assert.isAssignable(CommandData.class, bean.getClass(),
				"Bean '" + key + "' annotated with @GlobalCommand must be an instance of " + CommandData.class + "!"));

		return beans
				// Get beans
				.values()
				// Stream
				.stream()
				// As CommandData
				.map(CommandData.class::cast);
	}

	@Override
	public Iterator<Command> iterator() {
		if (commands != null)
			return commands.iterator();
		throw new UnsupportedOperationException(
				"CommandService has not been setup yet. All calls must be after context refresh");
	}
}
