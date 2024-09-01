package net.foxgenesis.springJDA.impl;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationListener;
import org.springframework.core.metrics.ApplicationStartup;
import org.springframework.core.metrics.StartupStep;
import org.springframework.stereotype.Service;

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
public class CommandRegistryImpl implements CommandRegistry, ApplicationListener<SpringJDASemiReadyEvent> {
	private final Logger logger = LoggerFactory.getLogger(CommandRegistry.class);

	@Autowired
	private SpringJDAConfiguration config;

	@Autowired
	private SpringJDA jda;

	@Autowired
	private ConfigurableListableBeanFactory ctx;

	private CopyOnWriteArraySet<Command> commands;

	@Override
	public void onApplicationEvent(SpringJDASemiReadyEvent event) {
		ApplicationStartup startup = ApplicationStartup.DEFAULT;
		StartupStep initStep = startup.start("SpringJDA.commandRegistryInit");
		
		// Get current commands from Discord
		logger.info("Requesting commands from Discord");
		StartupStep loadStep = startup.start("SpringJDA.retrieveCommands");
		CompletableFuture<List<Command>> future = jda.retrieveCommands().submit().whenComplete((v,err) -> loadStep.end());

		// Scan for global commands in application
		Set<CommandData> commandData = new HashSet<>();
		getCommands()
				// Iterate over commands
				.forEach(command -> {
					// Attempt to add command
					if (!commandData.add(command))
						throw new IllegalArgumentException("command " + command + " is already registered");
				});
		
		this.commands = future
				// If scanned commands do not match current commands then update
				.thenCompose(commands -> {
					// Return if scanned commands match current commands
					if (isSame(commands, commandData))
						return CompletableFuture.completedFuture(commands);
					
					// Scanned commands do not match current commands
					logger.warn("Commands requested from Discord does not match with the current application!");
					
					// Check if updating commands is permitted
					if(!config.updateCommands()) {
						logger.warn("Updating commands disabled! Continuing...");
						return CompletableFuture.completedFuture(commands);
					}
					
					// Update current commands with scanned commands
					logger.warn("Commands requested from Discord does not match with the current application!");
					logger.warn("Uploading {} commands to Discord: " + commandData.size(),
							commandData.stream().map(CommandData::getName).sorted().toList());

					// Map to future uploaded commands
					StartupStep uploadStep = startup.start("SpringJDA.uploadCommands");
					return jda
							// New update command request
							.updateCommands()
							// Add scanned commands
							.addCommands(commandData)
							// As future
							.submit()
							// Log when complete
							.whenComplete((uploaded, err) -> {
								uploadStep.end();
								if (err != null)
									logger.warn("Upload complete");
							});
				})
				// Map to CopyOnWriteArraySet
				.thenApply(CopyOnWriteArraySet::new)
				// Block
				.join();
		initStep.end();
		logger.info("Loaded {} commands", commands.size());

//		if (config.updateCommands()) {
//			Set<CommandData> commandData = new HashSet<>();
//
//			logger.info("Scanning for global commands");
//			// Get commands
//			getCommands()
//					// Iterate over commands
//					.forEach(command -> {
//						// Attempt to add command
//						if (!commandData.add(command))
//							throw new IllegalArgumentException("command " + command + " is already registered");
//					});
//			logger.info("Uploading {} commands to Discord: " + commandData.size(),
//					commandData.stream().map(CommandData::getName).sorted().toList());
//
//			this.commands = new CopyOnWriteArraySet<>(jda.updateCommands().addCommands(commandData).complete());
//		} else {
//			logger.info("Requesting commands from Discord");
//			this.commands = new CopyOnWriteArraySet<>(jda.retrieveCommands().complete());
//		}
//
//		logger.info("Loaded {} commands", commands.size());
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
		// Get a stream of all beans with @GlobalCommand annotation
		return Arrays.stream(ctx.getBeanNamesForAnnotation(GlobalCommand.class))
		// Only instances of CommandData
		.filter(name -> ctx.isTypeMatch(name, CommandData.class))
		// As CommandData
		.map(name -> ctx.getBean(name, CommandData.class));
		
		
//		Map<String, Object> beans = ctx.getBeansWithAnnotation(GlobalCommand.class);
//		// Check that all beans are of correct type
//		beans.forEach((key, bean) -> Assert.isAssignable(CommandData.class, bean.getClass(),
//				"Bean '" + key + "' annotated with @GlobalCommand must be an instance of " + CommandData.class + "!"));
//
//		return beans
//				// Get beans
//				.values()
//				// Stream
//				.stream()
//				// As CommandData
//				.map(CommandData.class::cast);
	}

	@Override
	public Iterator<Command> iterator() {
		if (commands != null)
			return commands.iterator();
		throw new UnsupportedOperationException(
				"CommandService has not been setup yet. All calls must be after context refresh");
	}

	private boolean isSame(Collection<Command> commands, Collection<CommandData> data) {
		return commands.size() == data.size()
				? commands.stream().map(CommandData::fromCommand).toList().containsAll(data)
				: false;
	}
}
