package net.foxgenesis.springJDA.autoconfigure;

import static net.foxgenesis.springJDA.SpringJDA.SPRING_JDA;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

import jakarta.validation.constraints.NotBlank;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDA.Status;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.StatusChangeEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.foxgenesis.springJDA.SpringJDA;
import net.foxgenesis.springJDA.annotation.AutoExclude;
import net.foxgenesis.springJDA.annotation.CacheFlags;
import net.foxgenesis.springJDA.annotation.ContextConfiguration;
import net.foxgenesis.springJDA.annotation.GatewayIntents;
import net.foxgenesis.springJDA.annotation.Permissions;
import net.foxgenesis.springJDA.annotation.Scopes;
import net.foxgenesis.springJDA.context.Scope;
import net.foxgenesis.springJDA.context.SpringJDAInitializer;
import net.foxgenesis.springJDA.context.impl.DefaultShardedSpringJDAContext;
import net.foxgenesis.springJDA.context.impl.DefaultSingleSpringJDAContext;
import net.foxgenesis.springJDA.impl.DefaultShardedSpringJDA;
import net.foxgenesis.springJDA.impl.DefaultSingleSpringJDA;
import net.foxgenesis.springJDA.provider.PermissionProvider;
import net.foxgenesis.springJDA.provider.ScopeProvider;

@AutoConfiguration
@ConditionalOnClass(JDA.class)
@EnableConfigurationProperties(SpringJDAConfiguration.class)
public class SpringJDAAutoConfiguration {
	public static final String PROPERTY_USE_SHARDING = SPRING_JDA + ".use-sharding";

	public static final String PERMISSIONS_BEAN_NAME = SPRING_JDA + ".permissions";

	private static final String TOKEN_PROPERTY_KEY = SPRING_JDA + ".token";

	private static final Logger log = LoggerFactory.getLogger(SpringJDA.class);

	@Bean
	@ConditionalOnMissingBean
	SpringJDA defaultJDA(SpringJDAConfiguration config,
			@NotBlank @NonNull @Value("${" + TOKEN_PROPERTY_KEY + "}") String token) {
		if (config.useSharding())
			return new DefaultShardedSpringJDA(new DefaultShardedSpringJDAContext(token));

		return new DefaultSingleSpringJDA(new DefaultSingleSpringJDAContext(token));
	}

	@Bean
	SpringJDAInitializer<?> beanInitializer(ConfigurableListableBeanFactory factory, SpringJDAConfiguration config,
			@Qualifier(SpringJDA.SPRING_JDA + ".annotation-configuration.intents") Optional<Set<GatewayIntent>> intents,
			@Qualifier(SpringJDA.SPRING_JDA + ".annotation-configuration.flags") Optional<Set<CacheFlag>> flags) {
		return context -> {
			intents.ifPresent(i -> i.forEach(context::enableIntents));
			flags.ifPresent(f -> f.forEach(context::enableCache));

			if (config.eventAutoRegister()) {
				Object[] listeners = getAutoRegisterBeans(factory, EventListener.class).toArray();
				log.info("Adding {} event listeners", listeners.length);
				context.addEventListeners(listeners);
			}

			List<String> collected = getAutoRegisterBeans(factory, ScopeProvider.class)
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

			if (!(collected == null || collected.isEmpty())) {
				log.info("Discord invite scopes: {}", collected);
				context.addEventListeners(new ListenerAdapter() {
					public void onStatusChange(@NonNull StatusChangeEvent event) {
						if (event.getNewStatus() == Status.INITIALIZED) {
							event.getJDA().setRequiredScopes(collected);
							event.getJDA().removeEventListener(this);
						}
					}
				});
			}
		};
	}

	@Bean(PERMISSIONS_BEAN_NAME)
	@ConditionalOnMissingBean(name = PERMISSIONS_BEAN_NAME)
	Set<Permission> perms(ConfigurableListableBeanFactory factory) {
		Set<Permission> permissions = getAutoRegisterBeans(factory, PermissionProvider.class)
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

	@Bean
	@ConditionalOnBean(annotation = ContextConfiguration.class)
	@ConditionalOnProperty(value = SpringJDA.SPRING_JDA + ".annotation-configuration", matchIfMissing = true)
	static BeanFactoryPostProcessor annotationPostProcessor() {
		return factory -> {
			log.info("Scanning for JDA context annotations");

			String KEY = SpringJDA.SPRING_JDA + ".annotation-configuration";
			Set<Permission> permissions = collectAnnotations(factory, Permissions.class, Permissions::value);
			Set<Scope> scopes = collectAnnotations(factory, Scopes.class, Scopes::value);
			Set<GatewayIntent> intents = collectAnnotations(factory, GatewayIntents.class, GatewayIntents::value);
			Set<CacheFlag> flags = collectAnnotations(factory, CacheFlags.class, CacheFlags::value);

			log.info("Annotation declared permissions: {}", permissions);
			log.info("Annotation declared scopes: {}", scopes);
			log.info("Annotation declared gateway intents: {}", intents);
			log.info("Annotation declared cache flags: {}", flags);

			factory.registerSingleton(KEY + ".permissions", PermissionProvider.of(permissions));
			factory.registerSingleton(KEY + ".scopes", ScopeProvider.of(scopes));
			factory.registerSingleton(KEY + ".intents", intents);
			factory.registerSingleton(KEY + ".flags", flags);
		};
	}

	private static <A extends Annotation, R> Set<R> collectAnnotations(ConfigurableListableBeanFactory factory,
			Class<A> type, Function<A, R[]> mapper) {
		Assert.notNull(mapper, "Mapper can not be null!");
		Set<R> collected = Arrays.stream(factory.getBeanNamesForAnnotation(type))
				// Filter out auto excluded
				.filter(name -> factory.findAnnotationOnBean(name, AutoExclude.class) == null)
				// Get annotation
				.map(c -> factory.findAnnotationOnBean(c, type, false))
				// Get data array
				.map(mapper::apply)
				// Only non null
				.filter(Objects::nonNull)
				// Merge values
				.mapMulti((R[] data, Consumer<R> consumer) -> {
					for (R r : data)
						consumer.accept(r);
				})
				// Extra check to ensure all values are valid
				.filter(Objects::nonNull)
				// As list
				.collect(Collectors.toUnmodifiableSet());
		return collected.isEmpty() ? Set.of() : collected;
	}

	private static <V> Stream<V> getAutoRegisterBeans(ConfigurableListableBeanFactory factory, Class<V> beanType) {
		return Arrays.stream(factory.getBeanNamesForType(beanType))
				// Filter out auto excluded
				.filter(name -> factory.findAnnotationOnBean(name, AutoExclude.class) == null)
				// Get bean
				.map(name -> factory.getBean(name, beanType));
	}
}
