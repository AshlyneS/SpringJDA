package net.foxgenesis.springJDA.impl;

import java.util.concurrent.RejectedExecutionException;
import java.util.function.IntFunction;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.core.GenericTypeResolver;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDA.Status;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.utils.cache.ShardCacheView;
import net.foxgenesis.springJDA.ShardedSpringJDA;
import net.foxgenesis.springJDA.context.SpringJDAInitializer;
import net.foxgenesis.springJDA.context.impl.DefaultShardedSpringJDAContext;
import net.foxgenesis.springJDA.event.AllShardsCreatedEvent;

/**
 * Default implementation of {@link ShardedSpringJDA}.
 * 
 * @author Ashley
 * @see ShardedSpringJDA
 */
public class DefaultShardedSpringJDA extends AbstractSpringJDA implements ShardedSpringJDA {
	
	private final DefaultShardedSpringJDAContext context;
	
	protected ShardManager manager;
	
	public DefaultShardedSpringJDA(DefaultShardedSpringJDAContext context) {
		this.context = context;
	}
	
	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected void preStart() {
		for (SpringJDAInitializer initializer : ctx.getBeanProvider(SpringJDAInitializer.class)) {
			Class<?> requiredType = GenericTypeResolver.resolveTypeArgument(initializer.getClass(),
					SpringJDAInitializer.class);
			Assert.isInstanceOf(requiredType, context, "Unable to call initializer.");
			initializer.initialize(context);
		}
		this.manager = context.build();
	}

	@Override
	public void startJDA() {
		if (manager == null)
			throw new RejectedExecutionException("SpringJDA is already shutdown");

		if (isRunning()) {
			logger.info("Restarting all shards");
			manager.restart();
		} else {
			logger.info("Starting all shards");
			manager.login();
		}
	}

	@Override
	protected void awaitReady() {
		while (manager.getShardsRunning() < manager.getShardsTotal()) {
			Thread.onSpinWait();
		}

		ctx.publishEvent(new AllShardsCreatedEvent(this));

		logger.info("Waiting for all shards to be ready");
		for (JDA jda : manager.getShards()) {
			try {
				jda.awaitReady();
				logger.info("Shard {} ready", jda.getShardInfo().getShardString());
			} catch (InterruptedException e) {
				throw new BeanCreationException("Failed to start SpringJDA", e);
			}
		}
	}

	@Override
	public void stop() {
		logger.info("Shutting down SpringJDA");
		manager.shutdown();
		manager = null;
		logger.info("Shutdown complete");
	}

	@SuppressWarnings("unused")
	@Override
	public boolean isRunning() {
		if (manager == null)
			return false;
		if (manager.getShardCache() == null)
			return false;
		return manager.getShardsRunning() > 0;
	}

	@Override
	public void close() throws Exception {
		if (isRunning()) {
			logger.info("Force closing");
			manager.shutdown();
			manager = null;
		}
	}

	@Override
	public int getShardsQueued() {
		return manager.getShardsQueued();
	}

	@NonNull
	@Override
	public ShardCacheView getShardCache() {
		return manager.getShardCache();
	}

	@Override
	public void removeEventListenerProvider(@NonNull IntFunction<Object> eventListenerProvider) {
		manager.removeEventListenerProvider(eventListenerProvider);
	}

	@Override
	public boolean isValid() {
		return !(manager == null ? true
				: getShardCache().stream().map(JDA::getStatus)
						.allMatch(status -> status == Status.SHUTDOWN || status == Status.SHUTTING_DOWN));
	}
}
