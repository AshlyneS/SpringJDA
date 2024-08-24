package net.foxgenesis.springJDA.autoconfigure;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.util.Assert;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.foxgenesis.springJDA.Scope;
import net.foxgenesis.springJDA.annotation.CacheFlags;
import net.foxgenesis.springJDA.annotation.GatewayIntents;
import net.foxgenesis.springJDA.annotation.Permissions;
import net.foxgenesis.springJDA.annotation.Scopes;
import net.foxgenesis.springJDA.provider.PermissionProvider;
import net.foxgenesis.springJDA.provider.ScopeProvider;

@net.foxgenesis.springJDA.annotation.SpringJDAAutoConfiguration
@ConditionalOnProperty(SpringJDAAutoConfiguration.PROPERTY_ANNOTATION_CONFIGURATION)
public class SpringJDAAnnotationConfiguration {
	public static final String GATEWAY_INTENTS_BEAN_NAME = SpringJDAAutoConfiguration.SPRING_JDA + ".intents";
	public static final String CACHE_FLAGS_BEAN_NAME = SpringJDAAutoConfiguration.SPRING_JDA + ".flags";

	private static final Logger log = LoggerFactory.getLogger(SpringJDAAnnotationConfiguration.class);

	@Bean
	@ConditionalOnBean(annotation = Permissions.class)
//	@ConditionalOnMissingBean(value = Permission.class, parameterizedContainer = Set.class, name = SpringJDAAutoConfiguration.PERMISSIONS_BEAN_NAME)
	static BeanFactoryPostProcessor permissionsPostProcessor() {
		return factory -> {
			Set<Permission> permissions = collectAnnotations(factory, Permissions.class, Permissions::value);
			log.info("Annotation declared permissions: {}", permissions);
			factory.registerSingleton(SpringJDAAutoConfiguration.PERMISSIONS_BEAN_NAME, PermissionProvider.of(permissions));
		};
	}
	
	@Bean
	@ConditionalOnBean(annotation = Scopes.class)
//	@ConditionalOnMissingBean(value = Scope.class, parameterizedContainer = Set.class, name = SpringJDAAutoConfiguration.SCOPES_BEAN_NAME)
	static BeanFactoryPostProcessor scopesPostProcessor() {
		return factory -> {
			Set<Scope> scopes = collectAnnotations(factory, Scopes.class, Scopes::value);
			log.info("Annotation declared scopes: {}", scopes);
			factory.registerSingleton(SpringJDAAutoConfiguration.SCOPES_BEAN_NAME, ScopeProvider.of(scopes));
		};
	}

	@Bean
	@ConditionalOnBean(annotation = GatewayIntents.class)
	@ConditionalOnMissingBean(value = GatewayIntent.class, parameterizedContainer = Set.class, name = GATEWAY_INTENTS_BEAN_NAME)
	static BeanFactoryPostProcessor gatewayIntentsPostProcessor() {
		return factory -> {
			Set<GatewayIntent> intents = collectAnnotations(factory, GatewayIntents.class, GatewayIntents::value);
			log.info("Annotation declared gateway intents: {}", intents);
			factory.registerSingleton(GATEWAY_INTENTS_BEAN_NAME, intents);
		};
	}

	@Bean
	@ConditionalOnBean(annotation = CacheFlags.class)
	@ConditionalOnMissingBean(value = CacheFlag.class, parameterizedContainer = Set.class, name = CACHE_FLAGS_BEAN_NAME)
	static BeanFactoryPostProcessor cacheFlagsPostProcessor() {
		return factory -> {
			Set<CacheFlag> flags = collectAnnotations(factory, CacheFlags.class, CacheFlags::value);
			log.info("Annotation declared cache flags: {}", flags);
			factory.registerSingleton(CACHE_FLAGS_BEAN_NAME, flags);
		};
	}

	private static <A extends Annotation, R> Set<R> collectAnnotations(ConfigurableListableBeanFactory factory,
			Class<A> type, Function<A, R[]> mapper) {
		Assert.notNull(mapper, "Mapper can not be null!");
		Set<R> collected = Arrays.stream(factory.getBeanNamesForAnnotation(type))
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
}
