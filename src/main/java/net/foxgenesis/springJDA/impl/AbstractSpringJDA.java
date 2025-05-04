package net.foxgenesis.springJDA.impl;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ResolvableType;
import org.springframework.core.metrics.ApplicationStartup;
import org.springframework.core.metrics.StartupStep;

import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.utils.data.DataObject;
import net.foxgenesis.springJDA.SpringJDA;
import net.foxgenesis.springJDA.annotation.GlobalCommand;
import net.foxgenesis.springJDA.autoconfigure.SpringJDAConfiguration;
import net.foxgenesis.springJDA.event.SpringJDAReadyEvent;
import net.foxgenesis.springJDA.event.SpringJDASemiReadyEvent;
import net.foxgenesis.springJDA.provider.GlobalCommandProvider;

/**
 * Base implementation for SpringJDA beans.
 * 
 * @author Ashley
 */
public abstract class AbstractSpringJDA implements SpringJDA, SmartLifecycle, AutoCloseable, ApplicationContextAware {
	protected final Logger logger = LoggerFactory.getLogger(SpringJDA.class);

	protected ApplicationContext ctx;

	protected SpringJDAConfiguration config;

	@Override
	public void start() {
		StartupStep startup = ApplicationStartup.DEFAULT.start("SpringJDA.start");

		config = ctx.getBean(SpringJDAConfiguration.class);

		logger.info("Starting SpringJDA");
		preStart();

		// Start JDA
		startJDA();

		// Wait until JDA is in a semi-usable state
		while (!isRunning())
			Thread.onSpinWait();

		ctx.publishEvent(new SpringJDASemiReadyEvent(this));

		attemptCommandUpdate();

		// Wait until JDA is fully ready
		awaitReady();

		startup.end();
		ctx.publishEvent(new SpringJDAReadyEvent(this));
	}

	protected abstract void preStart();

	/**
	 * Start JDA or restart if already running.
	 */
	protected abstract void startJDA();

	/**
	 * Block the current thread until JDA is fully ready for use.
	 */
	protected abstract void awaitReady();

	@Override
	public int getPhase() {
		return 0;
	}

	@Override
	public void setApplicationContext(ApplicationContext ctx) {
		this.ctx = ctx;
	}

	private void attemptCommandUpdate() {
		logger.info("Requesting commands from Discord");
		CompletableFuture<List<Command>> future = retrieveCommands(true).submit();

		// Scan for global commands in application
		Set<CommandData> commandData = new HashSet<>();
		getCommands()
				// Filter out null objects
				.filter(Objects::nonNull)
				// distinct
				.distinct()
				// Iterate over commands
				.forEachOrdered(command -> {
					// Attempt to add command
					if (!commandData.add(command))
						throw new IllegalArgumentException("command " + command + " is already registered");
				});

		future.thenCompose(commands -> {
			// Return if scanned commands match current commands
			if (isSame(commands, commandData))
				return CompletableFuture.completedFuture(commands);

			// Scanned commands do not match current commands
			logger.warn("Commands requested from Discord does not match with the current application!");

			// Check if updating commands is permitted
			if (!config.updateCommands()) {
				logger.warn("Updating commands disabled! Continuing...");
				return CompletableFuture.completedFuture(commands);
			}

			// Update current commands with scanned commands
			logger.warn("Uploading {} commands to Discord: " + commandData.size(),
					commandData.stream().map(CommandData::getName).sorted().toList());

			// Map to future uploaded commands
			return updateCommands()
					// Add scanned commands
					.addCommands(commandData)
					// As future
					.submit()
					// Log when complete
					.whenComplete((uploaded, err) -> {
						if (err == null)
							logger.warn("Upload complete");
						else
							logger.error("Error while uploading commands to discord", err);
					});
		}).join();
	}

	private Stream<CommandData> getCommands() {
		Stream<CommandData> providers = ctx
				// Get GlobalCommandProvider provider
				.getBeanProvider(GlobalCommandProvider.class)
				// Stream
				.stream()
				// Get command data
				.map(GlobalCommandProvider::getCommandData)
				// Merge collections
				.mapMulti(AbstractSpringJDA::mergeCollectionToStream);
		// If annotation configuration is enabled, get and merge @GlobalCommand beans
		return config.annotationConfiguration() ? Stream.concat(providers, getGlobalCommandAnnotationBeans())
				: providers;
	}

	@SuppressWarnings("unchecked")
	private Stream<CommandData> getGlobalCommandAnnotationBeans() {
		final ParameterizedTypeReference<Collection<CommandData>> typeRef = new ParameterizedTypeReference<Collection<CommandData>>() {
		};

		// Get a stream of all beans with @GlobalCommand annotation
		return Arrays.stream(ctx.getBeanNamesForAnnotation(GlobalCommand.class))
				.mapMulti((String name, Consumer<CommandData> con) -> {
					// If bean instance of CommandData
					if (ctx.isTypeMatch(name, CommandData.class)) {
						con.accept(ctx.getBean(name, CommandData.class));
						return;
					}

					// If bean instance of Collection<CommandData>
					if (ctx.isTypeMatch(name, ResolvableType.forType(typeRef))) {
						mergeCollectionToStream(ctx.getBean(name, Collection.class), con);
						return;
					}

					// Invalid bean
					throw new BeanCreationException(name, "Bean '" + name
							+ "' is marked as @GlobalCommand but is not an instance of CommandData/Collection<CommandData>");
				});
	}

	private boolean isSame(Collection<Command> commands, Collection<CommandData> data) {
		if (commands.size() != data.size())
			return false;
		// FIXME match does not work while localization is active
		Collection<DataObject> discordData = commands
				// Stream
				.stream()
				// Get command data
				.map(CommandData::fromCommand)
				// As data map
				.map(CommandData::toData)
				// To List
				.toList();
		Collection<DataObject> ourData = data
				// Stream
				.stream()
				// As data map
				.map(CommandData::toData)
				// To list
				.toList();
		
		for(DataObject d: ourData) {
			if(!discordData.contains(d)) {
				logger.warn("Command is missing from discord: {}", d);
				return false;
			}
		}

		// Check if both data are the same
		return discordData.containsAll(ourData);
	}

	private static <V> void mergeCollectionToStream(Collection<V> set, Consumer<V> consumer) {
		if (set != null)
			set.forEach(consumer::accept);
	}
}
