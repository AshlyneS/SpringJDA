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
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.util.Assert;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.foxgenesis.springJDA.Scope;
import net.foxgenesis.springJDA.SpringJDA;
import net.foxgenesis.springJDA.annotation.CacheFlags;
import net.foxgenesis.springJDA.annotation.ContextConfiguration;
import net.foxgenesis.springJDA.annotation.GatewayIntents;
import net.foxgenesis.springJDA.annotation.Permissions;
import net.foxgenesis.springJDA.annotation.Scopes;
import net.foxgenesis.springJDA.provider.PermissionProvider;
import net.foxgenesis.springJDA.provider.ScopeProvider;

@net.foxgenesis.springJDA.annotation.SpringJDAAutoConfiguration
@ConditionalOnProperty(SpringJDAAnnotationConfiguration.KEY)
public class SpringJDAAnnotationConfiguration {
	private static final String KEY = SpringJDA.SPRING_JDA + ".annotation-configuration";

	private static final Logger log = LoggerFactory.getLogger(SpringJDA.class);

	@Bean
	@ConditionalOnBean(annotation = ContextConfiguration.class)
	@org.springframework.context.annotation.Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	static BeanFactoryPostProcessor annotationPostProcessor() {
		return factory -> {
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
