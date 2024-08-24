package net.foxgenesis.springJDA.context.impl;

import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.IntFunction;

import org.springframework.beans.BeansException;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import com.neovisionaries.ws.client.WebSocketFactory;

import net.dv8tion.jda.api.GatewayEncoding;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.audio.factory.IAudioSendFactory;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.hooks.IEventManager;
import net.dv8tion.jda.api.hooks.VoiceDispatchInterceptor;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.RestConfig;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.api.sharding.ThreadPoolProvider;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.Compression;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.SessionController;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.foxgenesis.springJDA.ShardedSpringJDA;
import net.foxgenesis.springJDA.context.ShardedSpringJDAContext;
import net.foxgenesis.springJDA.impl.DefaultShardedSpringJDA;
import okhttp3.OkHttpClient;
import okhttp3.OkHttpClient.Builder;

/**
 * Default implementation of a {@link ShardedSpringJDAContext}.
 * @author Ashley
 * @see ShardedSpringJDAContext
 */
public class DefaultShardedSpringJDAContext extends AbstractSpringJDAContext implements ShardedSpringJDAContext {

	protected final DefaultShardManagerBuilder builder;
	
	public DefaultShardedSpringJDAContext(DefaultShardManagerBuilder builder) {
		this.builder = Objects.requireNonNull(builder);
	}

	@Override
	public ShardedSpringJDAContext setGatewayEncoding(GatewayEncoding encoding) {
		builder.setGatewayEncoding(encoding);
		return this;
	}

	@Override
	public ShardedSpringJDAContext setRawEventsEnabled(boolean enable) {
		builder.setRawEventsEnabled(enable);
		return this;
	}

	@Override
	public ShardedSpringJDAContext setEventPassthrough(boolean enable) {
		builder.setEventPassthrough(enable);
		return this;
	}

	@Override
	public ShardedSpringJDAContext setRestConfig(RestConfig config) {
		builder.setRestConfig(config);
		return this;
	}

	@Override
	public ShardedSpringJDAContext setRestConfigProvider(@NonNull IntFunction<? extends RestConfig> provider) {
		builder.setRestConfigProvider(provider);
		return this;
	}

	@Override
	public ShardedSpringJDAContext enableCache(Collection<CacheFlag> flags) {
		builder.enableCache(flags);
		return this;
	}

	@Override
	public ShardedSpringJDAContext enableCache(CacheFlag flag, CacheFlag... flags) {
		builder.enableCache(flag, flags);
		return this;
	}

	@Override
	public ShardedSpringJDAContext disableCache(Collection<CacheFlag> flags) {
		builder.disableCache(flags);
		return this;
	}

	@Override
	public ShardedSpringJDAContext disableCache(CacheFlag flag, CacheFlag... flags) {
		builder.disableCache(flag, flags);
		return this;
	}

	@Override
	public ShardedSpringJDAContext setMemberCachePolicy(MemberCachePolicy policy) {
		builder.setMemberCachePolicy(policy);
		return this;
	}

	@Override
	public ShardedSpringJDAContext setSessionController(SessionController controller) {
		builder.setSessionController(controller);
		return this;
	}

	@Override
	public ShardedSpringJDAContext setContextMap(@Nullable IntFunction<? extends ConcurrentMap<String, String>> provider) {
		builder.setContextMap(provider);
		return this;
	}

	@Override
	public ShardedSpringJDAContext setContextEnabled(boolean enable) {
		builder.setContextEnabled(enable);
		return this;
	}

	@Override
	public ShardedSpringJDAContext setCompression(Compression compression) {
		builder.setCompression(compression);
		return this;
	}

	@Override
	public ShardedSpringJDAContext setRequestTimeoutRetry(boolean retryOnTimeout) {
		builder.setRequestTimeoutRetry(retryOnTimeout);
		return this;
	}

	@Override
	public ShardedSpringJDAContext setHttpClientBuilder(Builder builder) {
		this.builder.setHttpClientBuilder(builder);
		return this;
	}

	@Override
	public ShardedSpringJDAContext setHttpClient(OkHttpClient client) {
		builder.setHttpClient(client);
		return this;
	}

	@Override
	public ShardedSpringJDAContext setWebsocketFactory(WebSocketFactory factory) {
		builder.setWebsocketFactory(factory);
		return this;
	}

	@Override
	public ShardedSpringJDAContext setRateLimitScheduler(ScheduledExecutorService pool) {
		builder.setRateLimitScheduler(pool);
		return this;
	}

	@Override
	public ShardedSpringJDAContext setRateLimitScheduler(ScheduledExecutorService pool, boolean automaticShutdown) {
		builder.setRateLimitScheduler(pool, automaticShutdown);
		return this;
	}

	@Override
	public ShardedSpringJDAContext setRateLimitSchedulerProvider(
			@Nullable ThreadPoolProvider<? extends ScheduledExecutorService> provider) {
		builder.setRateLimitSchedulerProvider(provider);
		return this;
	}

	@Override
	public ShardedSpringJDAContext setRateLimitElastic(ExecutorService pool) {
		builder.setRateLimitElastic(pool);
		return this;
	}

	@Override
	public ShardedSpringJDAContext setRateLimitElastic(ExecutorService pool, boolean automaticShutdown) {
		builder.setRateLimitElastic(pool, automaticShutdown);
		return this;
	}

	@Override
	public ShardedSpringJDAContext setRateLimitElasticProvider(
			@Nullable ThreadPoolProvider<? extends ExecutorService> provider) {
		builder.setRateLimitElasticProvider(provider);
		return this;
	}

	@Override
	public ShardedSpringJDAContext setGatewayPool(ScheduledExecutorService pool) {
		builder.setGatewayPool(pool);
		return this;
	}

	@Override
	public ShardedSpringJDAContext setGatewayPool(ScheduledExecutorService pool, boolean automaticShutdown) {
		builder.setGatewayPool(pool, automaticShutdown);
		return this;
	}

	@Override
	public ShardedSpringJDAContext setGatewayPoolProvider(
			@Nullable ThreadPoolProvider<? extends ScheduledExecutorService> provider) {
		builder.setGatewayPoolProvider(provider);
		return this;
	}

	@Override
	public ShardedSpringJDAContext setCallbackPool(ExecutorService executor) {
		builder.setCallbackPool(executor);
		return this;
	}

	@Override
	public ShardedSpringJDAContext setCallbackPool(ExecutorService executor, boolean automaticShutdown) {
		builder.setCallbackPool(executor, automaticShutdown);
		return this;
	}

	@Override
	public ShardedSpringJDAContext setCallbackPoolProvider(@Nullable ThreadPoolProvider<? extends ExecutorService> provider) {
		builder.setCallbackPoolProvider(provider);
		return this;
	}

	@Override
	public ShardedSpringJDAContext setEventPool(ExecutorService executor) {
		builder.setEventPool(executor);
		return this;
	}

	@Override
	public ShardedSpringJDAContext setEventPool(ExecutorService executor, boolean automaticShutdown) {
		builder.setEventPool(executor, automaticShutdown);
		return this;
	}
	
	@Override
	public ShardedSpringJDAContext setEventPoolProvider(@Nullable ThreadPoolProvider<? extends ExecutorService> provider) {
		builder.setEventPoolProvider(provider);
		return this;
	}

	@Override
	public ShardedSpringJDAContext setAudioPool(ScheduledExecutorService pool) {
		builder.setAudioPool(pool);
		return this;
	}

	@Override
	public ShardedSpringJDAContext setAudioPool(ScheduledExecutorService pool, boolean automaticShutdown) {
		builder.setAudioPool(pool, automaticShutdown);
		return this;
	}
	
	@Override
	public ShardedSpringJDAContext setAudioPoolProvider(@Nullable ThreadPoolProvider<? extends ScheduledExecutorService> provider) {
		builder.setAudioPoolProvider(provider);
		return this;
	}

	@Override
	public ShardedSpringJDAContext setBulkDeleteSplittingEnabled(boolean enabled) {
		builder.setBulkDeleteSplittingEnabled(enabled);
		return this;
	}

	@Override
	public ShardedSpringJDAContext setEnableShutdownHook(boolean enable) {
		builder.setEnableShutdownHook(enable);
		return this;
	}

	@Override
	public ShardedSpringJDAContext setAutoReconnect(boolean autoReconnect) {
		builder.setAutoReconnect(autoReconnect);
		return this;
	}

	@Override
	public ShardedSpringJDAContext setEventManagerProvider(@NonNull final IntFunction<? extends IEventManager> eventManagerProvider) {
		builder.setEventManagerProvider(eventManagerProvider);
		return this;
	}

	@Override
	public ShardedSpringJDAContext setAudioSendFactory(IAudioSendFactory factory) {
		builder.setAudioSendFactory(factory);
		return this;
	}

	@Override
	public ShardedSpringJDAContext setIdle(boolean idle) {
		builder.setIdle(idle);
		return this;
	}
	
	@Override
	public ShardedSpringJDAContext setIdleProvider(@Nullable final IntFunction<Boolean> idleProvider) {
		builder.setIdleProvider(idleProvider);
		return this;
	}

	@Override
	public ShardedSpringJDAContext setActivity(Activity activity) {
		builder.setActivity(activity);
		return this;
	}
	
	@Override
	public ShardedSpringJDAContext setActivityProvider(@Nullable final IntFunction<? extends Activity> activityProvider) {
		builder.setActivityProvider(activityProvider);
		return this;
	}

	@Override
	public ShardedSpringJDAContext setStatus(OnlineStatus status) {
		builder.setStatus(status);
		return this;
	}
	
	@Override
	public ShardedSpringJDAContext setStatusProvider(@Nullable final IntFunction<OnlineStatus> statusProvider) {
		builder.setStatusProvider(statusProvider);
		return this;
	}

	@Override
	public ShardedSpringJDAContext addEventListeners(Object... listeners) {
		builder.addEventListeners(listeners);
		return this;
	}
	
	@Override
	public ShardedSpringJDAContext addEventListenerProvider(@NonNull final IntFunction<Object> listenerProvider) {
		builder.addEventListenerProvider(listenerProvider);
		return this;
	}
	
	@Override
	public ShardedSpringJDAContext addEventListenerProviders(@NonNull final Collection<IntFunction<Object>> listenerProviders) {
		builder.addEventListenerProviders(listenerProviders);
		return this;
	}

	@Override
	public ShardedSpringJDAContext removeEventListeners(Object... listeners) {
		builder.removeEventListeners(listeners);
		return this;
	}
	
	@Override
	public ShardedSpringJDAContext removeEventListenerProvider(@NonNull final IntFunction<Object> listenerProvider) {
		builder.removeEventListenerProvider(listenerProvider);
		return this;
	}
	
	@Override
	public ShardedSpringJDAContext removeEventListenerProviders(@NonNull final Collection<IntFunction<Object>> listenerProviders) {
		builder.removeEventListenerProviders(listenerProviders);
		return this;
	}

	@Override
	public ShardedSpringJDAContext setMaxReconnectDelay(int maxReconnectDelay) {
		builder.setMaxReconnectDelay(maxReconnectDelay);
		return this;
	}

	@Override
	public ShardedSpringJDAContext setVoiceDispatchInterceptor(VoiceDispatchInterceptor interceptor) {
		builder.setVoiceDispatchInterceptor(interceptor);
		return this;
	}

	@Override
	public ShardedSpringJDAContext setChunkingFilter(ChunkingFilter filter) {
		builder.setChunkingFilter(filter);
		return this;
	}

	@Override
	public ShardedSpringJDAContext setDisabledIntents(@NonNull GatewayIntent intent, @NonNull GatewayIntent... intents) {
		builder.setDisabledIntents(intent, intents);
		return this;
	}

	@Override
	public ShardedSpringJDAContext setDisabledIntents(@NonNull Collection<GatewayIntent> intents) {
		builder.setDisabledIntents(intents);
		return this;
	}

	@Override
	public ShardedSpringJDAContext disableIntents(@NonNull Collection<GatewayIntent> intents) {
		builder.disableIntents(intents);
		return this;
	}

	@Override
	public ShardedSpringJDAContext disableIntents(@NonNull GatewayIntent intent, @NonNull GatewayIntent... intents) {
		builder.disableIntents(intent, intents);
		return this;
	}

	@Override
	public ShardedSpringJDAContext setEnabledIntents(@NonNull GatewayIntent intent, @NonNull GatewayIntent... intents) {
		builder.setEnabledIntents(intent, intents);
		return this;
	}

	@Override
	public ShardedSpringJDAContext setEnabledIntents(@NonNull Collection<GatewayIntent> intents) {
		builder.setEnabledIntents(intents);
		return this;
	}

	@Override
	public ShardedSpringJDAContext enableIntents(@NonNull Collection<GatewayIntent> intents) {
		builder.enableIntents(intents);
		return this;
	}

	@Override
	public ShardedSpringJDAContext enableIntents(@NonNull GatewayIntent intent, @NonNull GatewayIntent... intents) {
		builder.enableIntents(intent, intents);
		return this;
	}

	@Override
	public ShardedSpringJDAContext setLargeThreshold(int threshold) {
		builder.setLargeThreshold(threshold);
		return this;
	}

	@Override
	public ShardedSpringJDAContext setMaxBufferSize(int bufferSize) {
		builder.setMaxBufferSize(bufferSize);
		return this;
	}

	@Override
	public ShardedSpringJDAContext setShards(final int... shardIds) {
		builder.setShards(shardIds);
		return this;
	}
	
	@Override
	public ShardedSpringJDAContext setShards(final int minShardId, final int maxShardId) {
		builder.setShards(minShardId, maxShardId);
		return this;
	}
	
	@Override
	public ShardedSpringJDAContext setShards(@NonNull Collection<Integer> shardIds) {
		builder.setShards(shardIds);
		return this;
	}
	
	@Override
	public ShardedSpringJDAContext setShardsTotal(final int shardsTotal) {
		builder.setShardsTotal(shardsTotal);
		return this;
	}

	@Override
	public ShardedSpringJDA getObject() throws BeansException {
		return new DefaultShardedSpringJDA(builder.build(false));
	}
}
