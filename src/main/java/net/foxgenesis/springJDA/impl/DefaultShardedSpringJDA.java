package net.foxgenesis.springJDA.impl;

import java.util.Objects;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.IntFunction;

import org.springframework.beans.factory.BeanCreationException;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDA.Status;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.utils.Once.Builder;
import net.dv8tion.jda.api.utils.cache.ShardCacheView;
import net.foxgenesis.springJDA.ShardedSpringJDA;
import net.foxgenesis.springJDA.event.AllShardsCreatedEvent;

/**
 * Default implementation of {@link ShardedSpringJDA}.
 * 
 * @author Ashley
 * @see ShardedSpringJDA
 */
public class DefaultShardedSpringJDA extends AbstractSpringJDA implements ShardedSpringJDA {
	protected ShardManager manager;

	public DefaultShardedSpringJDA(ShardManager manager) {
		this.manager = Objects.requireNonNull(manager);
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

		publisher.publishEvent(new AllShardsCreatedEvent(this));

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
		logger.info("Shutting down all shards");
		manager.shutdown();
		manager = null;
	}

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
		}
	}

	@Override
	public int getShardsQueued() {
		return manager.getShardsQueued();
	}

	@Override
	public ShardCacheView getShardCache() {
		return manager.getShardCache();
	}

	@Override
	public void removeEventListenerProvider(IntFunction<Object> eventListenerProvider) {
		manager.removeEventListenerProvider(eventListenerProvider);
	}

	@Override
	public boolean isValid() {
		return manager == null ? true
				: getShardCache().stream().map(JDA::getStatus)
						.allMatch(status -> status == Status.SHUTDOWN || status == Status.SHUTTING_DOWN);
	}

	@Override
	public <E extends GenericEvent> Builder<E> listenOnce(Class<E> eventType) {
		throw new UnsupportedOperationException(
				"Listen once not supporeted in shard manager. Please use the method from the shard itself");
	}
}
