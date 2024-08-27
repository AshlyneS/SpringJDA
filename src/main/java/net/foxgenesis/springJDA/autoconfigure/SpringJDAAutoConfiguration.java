package net.foxgenesis.springJDA.autoconfigure;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.env.Environment;
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
import net.foxgenesis.springJDA.annotation.AutoRegisterExclude;
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
	public static final String SPRING_JDA = "spring-jda";
	public static final String PROPERTY_USE_SHARDING = SPRING_JDA + ".use-sharding";
	public static final String PROPERTY_AUTO_REGISTER = SPRING_JDA + ".event-auto-register";
	public static final String PROPERTY_ANNOTATION_CONFIGURATION = SPRING_JDA + ".annotation-configuration";

	public static final String PERMISSIONS_BEAN_NAME = SPRING_JDA + ".permissions";
	public static final String SCOPES_BEAN_NAME = SPRING_JDA + ".scopes";

	private static final String TOKEN_PROPERTY_KEY = SPRING_JDA + ".token";

	private static final Logger log = LoggerFactory.getLogger(SpringJDAAutoConfiguration.class);

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
		for (SpringJDAInitializer initializer : inits) {
			Class<?> requiredType = GenericTypeResolver.resolveTypeArgument(initializer.getClass(),
					SpringJDAInitializer.class);
			Assert.isInstanceOf(requiredType, context, "Unable to call initializer.");
			initializer.initialize(context);
		}
		log.info("Finalizing SpringJDA");
		return context.createSpringJDA();
	}

	@Bean
	SpringJDAInitializer<?> beanInitializer(ObjectProvider<Set<GatewayIntent>> intents,
			ObjectProvider<Set<CacheFlag>> flags) {
		return context -> {
			intents.forEach(context::enableIntents);
			flags.forEach(context::enableCache);
		};
	}

	@Bean
	@ConditionalOnBean(EventListener.class)
	@ConditionalOnProperty(PROPERTY_AUTO_REGISTER)
	SpringJDAInitializer<?> autoRegisterListeners(ApplicationContext ctx) {
		Object[] listeners = ctx
				// Get all EventListener beans
				.getBeansOfType(EventListener.class)
				// Get beans
				.values()
				// Stream
				.stream()
				// Only beans without AutoRegisterExclude
				.filter(SpringJDAAutoConfiguration::isAutoExcluded)
				// To array
				.toArray();

		return builder -> {
			log.info("Adding {} event listeners", listeners.length);
			builder.addEventListeners(listeners);
		};
	}

	@Bean
	@ConditionalOnBean(ScopeProvider.class)
	SpringJDAInitializer<?> setScopes(ObjectProvider<ScopeProvider> scopes) {
		return context -> {
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
	Set<Permission> perms(ObjectProvider<PermissionProvider> providers) {
		Set<Permission> permissions = providers
				// Stream
				.stream()
				// Only beans without AutoRegisterExclude
				.filter(SpringJDAAutoConfiguration::isAutoExcluded)
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
		return !obj.getClass().isAnnotationPresent(AutoRegisterExclude.class);
	}
}
