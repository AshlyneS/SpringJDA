package net.foxgenesis.springJDA.impl;

import java.util.Objects;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.IntFunction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.SmartLifecycle;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDA.Status;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.utils.Once.Builder;
import net.dv8tion.jda.api.utils.cache.ShardCacheView;
import net.foxgenesis.springJDA.ShardedSpringJDA;
import net.foxgenesis.springJDA.event.AllShardsCreatedEvent;
import net.foxgenesis.springJDA.event.SpringJDAReadyEvent;
import net.foxgenesis.springJDA.event.SpringJDASemiReadyEvent;

/**
 * Default implementation of {@link ShardedSpringJDA}.
 * 
 * @author Ashley
 * @see ShardedSpringJDA
 */
public class DefaultShardedSpringJDA implements ShardedSpringJDA, SmartLifecycle, AutoCloseable, ApplicationEventPublisherAware {
	private final Logger logger = LoggerFactory.getLogger(ShardedSpringJDA.class);
	
	private ApplicationEventPublisher publisher;
	
	private ShardManager manager;

	public DefaultShardedSpringJDA(ObjectFactory<ShardManager> factory) {
		this(factory.getObject());
	}

	public DefaultShardedSpringJDA(ShardManager manager) {
		this.manager = Objects.requireNonNull(manager);
	}

	@Override
	public void start() {
		if (manager == null)
			throw new RejectedExecutionException("SpringJDA is already shutdown");

		if (isRunning()) {
			logger.info("Restarting all shards");
			manager.restart();
		} else {
			logger.info("Starting all shards");
			manager.login();
		}
		
		while(!isRunning()) {
			Thread.onSpinWait();
		}
		
		publisher.publishEvent(new SpringJDASemiReadyEvent(this));
		
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
		
		logger.info("All shards ready");
		publisher.publishEvent(new SpringJDAReadyEvent(this));
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
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.publisher = applicationEventPublisher;
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
	public boolean isShuttingDown() {
		return manager == null ? true
				: getShardCache().stream().map(JDA::getStatus)
						.allMatch(status -> status == Status.SHUTDOWN || status == Status.SHUTTING_DOWN);
	}

	@Override
	public <E extends GenericEvent> Builder<E> listenOnce(Class<E> eventType) {
		throw new UnsupportedOperationException("Listen once not supporeted in shard manager. Please use the method from the shard itself");
	}
}
