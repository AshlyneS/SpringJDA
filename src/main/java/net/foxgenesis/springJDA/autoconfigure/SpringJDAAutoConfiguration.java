package net.foxgenesis.springJDA.autoconfigure;

import static net.foxgenesis.springJDA.SpringJDA.SPRING_JDA;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.env.Environment;
import org.springframework.core.metrics.ApplicationStartup;
import org.springframework.core.metrics.StartupStep;
import org.springframework.util.Assert;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDA.Status;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.StatusChangeEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.foxgenesis.springJDA.Scope;
import net.foxgenesis.springJDA.SpringJDA;
import net.foxgenesis.springJDA.annotation.AutoExclude;
import net.foxgenesis.springJDA.context.SpringJDAInitializer;
import net.foxgenesis.springJDA.context.impl.AbstractSpringJDAContext;
import net.foxgenesis.springJDA.context.impl.DefaultShardedSpringJDAContext;
import net.foxgenesis.springJDA.context.impl.DefaultSingleSpringJDAContext;
import net.foxgenesis.springJDA.impl.CommandRegistryImpl;
import net.foxgenesis.springJDA.provider.PermissionProvider;
import net.foxgenesis.springJDA.provider.ScopeProvider;

@AutoConfiguration
@ConditionalOnClass(JDA.class)
@Import(CommandRegistryImpl.class)
public class SpringJDAAutoConfiguration {
	public static final String PROPERTY_USE_SHARDING = SPRING_JDA + ".use-sharding";

	public static final String PERMISSIONS_BEAN_NAME = SPRING_JDA + ".permissions";

	private static final String TOKEN_PROPERTY_KEY = SPRING_JDA + ".token";

	private static final Logger log = LoggerFactory.getLogger(SpringJDA.class);

	@Lazy
	@Bean
	@ConditionalOnProperty(PROPERTY_USE_SHARDING)
	@ConditionalOnMissingBean(AbstractSpringJDAContext.class)
	DefaultShardedSpringJDAContext shardedContext(Environment ev) {
		log.info("Creating sharding SpringJDA context");
		return new DefaultShardedSpringJDAContext(ev.getRequiredProperty(TOKEN_PROPERTY_KEY));
	}

	@Lazy
	@Bean
	@ConditionalOnMissingBean(AbstractSpringJDAContext.class)
	@ConditionalOnProperty(name = PROPERTY_USE_SHARDING, matchIfMissing = true, havingValue = "false")
	DefaultSingleSpringJDAContext singleContext(Environment ev) {
		log.info("Creating single SpringJDA context");
		return new DefaultSingleSpringJDAContext(ev.getRequiredProperty(TOKEN_PROPERTY_KEY));
	}

	@Bean
	@ConditionalOnMissingBean
	@SuppressWarnings({ "rawtypes", "unchecked" })
	SpringJDA defaultJDA(AbstractSpringJDAContext context, ObjectProvider<SpringJDAInitializer> inits) {
		log.info("Configuring SpringJDA context");
		StartupStep create = ApplicationStartup.DEFAULT.start("SpringJDA.create");
		for (SpringJDAInitializer initializer : inits) {
			Class<?> requiredType = GenericTypeResolver.resolveTypeArgument(initializer.getClass(),
					SpringJDAInitializer.class);
			Assert.isInstanceOf(requiredType, context, "Unable to call initializer.");
			initializer.initialize(context);
		}
		log.info("Finalizing SpringJDA");
		SpringJDA jda = context.createSpringJDA();
		create.end();
		return jda;
	}

	@Bean
	@org.springframework.context.annotation.Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	SpringJDAInitializer<?> beanInitializer(SpringJDAConfiguration config, ObjectProvider<Set<GatewayIntent>> intents,
			ObjectProvider<Set<CacheFlag>> flags, ObjectProvider<ScopeProvider> scopes,
			ObjectProvider<EventListener> eventListeners) {
		return context -> {
			intents.forEach(context::enableIntents);
			flags.forEach(context::enableCache);

			if (config.eventAutoRegister()) {
				Object[] listeners = eventListeners
						// Stream
						.stream()
						// Only beans without AutoRegisterExclude
						.filter(SpringJDAAutoConfiguration::isAutoExcluded)
						// To array
						.toArray();
				log.info("Adding {} event listeners", listeners.length);
				context.addEventListeners(listeners);
			}

			List<String> collected = scopes
					// Stream
					.stream()
					// Only beans without AutoRegisterExclude
					.filter(SpringJDAAutoConfiguration::isAutoExcluded)
					// Join all collections
					.mapMulti((ScopeProvider provider, Consumer<Scope> mapper) -> {
						for (Scope scope : provider.getScopes())
							mapper.accept(scope);
					})
					// Only distinct values
					.distinct()
					// As scope name
					.map(Scope::getName)
					// To list
					.toList();
			log.info("Discord invite scopes: {}", collected);

			context.addEventListeners(new ListenerAdapter() {
				public void onStatusChange(StatusChangeEvent event) {
					if (event.getNewStatus() == Status.INITIALIZED) {
						event.getJDA().setRequiredScopes(collected);
					}
				}
			});
		};
	}

	@Bean(PERMISSIONS_BEAN_NAME)
	@ConditionalOnMissingBean(name = PERMISSIONS_BEAN_NAME)
	Set<Permission> perms(ConfigurableListableBeanFactory factory) {
		Set<Permission> permissions = Arrays
				// Get all PermissionProvider bean names
				.stream(factory.getBeanNamesForType(PermissionProvider.class))
				// Filter out auto excluded beans
				.filter(name -> isAutoExcluded(factory, name))
				// Get bean as PermissionProvider
				.map(name -> factory.getBean(name, PermissionProvider.class))
				// Join all collections
				.mapMulti((PermissionProvider provider, Consumer<Permission> mapper) -> {
					for (Permission permission : provider.getPermissions())
						mapper.accept(permission);
				})
				// Only distinct values
				.distinct()
				// As set
				.collect(Collectors.toUnmodifiableSet());

		log.info("Discord invite permissions: {}", permissions);
		return permissions;
	}
	
	static boolean isAutoExcluded(Object obj) {
		return obj.getClass().isAnnotationPresent(AutoExclude.class);
	}

	static boolean isAutoExcluded(ConfigurableListableBeanFactory factory, String bean) {
		return factory.findAnnotationOnBean(bean, AutoExclude.class, false) != null;
	}
}
