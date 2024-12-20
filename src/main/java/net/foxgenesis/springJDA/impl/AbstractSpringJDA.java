package net.foxgenesis.springJDA.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.metrics.ApplicationStartup;
import org.springframework.core.metrics.StartupStep;

import net.foxgenesis.springJDA.SpringJDA;
import net.foxgenesis.springJDA.event.SpringJDAReadyEvent;
import net.foxgenesis.springJDA.event.SpringJDASemiReadyEvent;

/**
 * Base implementation for SpringJDA beans.
 * 
 * @author Ashley
 */
public abstract class AbstractSpringJDA
		implements SpringJDA, SmartLifecycle, AutoCloseable, ApplicationContextAware {
	protected final Logger logger = LoggerFactory.getLogger(SpringJDA.class);

	protected ApplicationContext ctx;

	@Override
	public void start() {
		StartupStep startup = ApplicationStartup.DEFAULT.start("SpringJDA.start");
		
		logger.info("Starting SpringJDA");
		preStart();

		// Start JDA
		startJDA();

		// Wait until JDA is in a semi-usable state
		while (!isRunning())
			Thread.onSpinWait();

		ctx.publishEvent(new SpringJDASemiReadyEvent(this));

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

}
