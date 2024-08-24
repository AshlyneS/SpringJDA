package net.foxgenesis.springJDA.context;

import java.util.Collection;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.IntFunction;

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
import okhttp3.OkHttpClient;

/**
 * Interface containing proxy methods for interacting with a wrapped
 * {@link DefaultShardManagerBuilder} instance.
 * 
 * @author Ashley
 * @see ShardedSpringJDAContext
 * @see SingleSpringJDAContext
 */
public interface ShardedSpringJDAContext extends SpringJDAContext {
	/**
	 * Custom {@link RestConfig} to use. <br>
	 * This can be used to customize how rate-limits are handled and configure a
	 * custom http proxy.
	 *
	 * @param provider The {@link RestConfig} provider to use
	 *
	 * @throws IllegalArgumentException If null is provided
	 *
	 * @return The ShardedSpringJDAContext instance. Useful for chaining.
	 */
	ShardedSpringJDAContext setRestConfigProvider(@NonNull IntFunction<? extends RestConfig> provider);

	ShardedSpringJDAContext setContextMap(@Nullable IntFunction<? extends ConcurrentMap<String, String>> provider);

	ShardedSpringJDAContext setRateLimitSchedulerProvider(
			@Nullable ThreadPoolProvider<? extends ScheduledExecutorService> provider);

	ShardedSpringJDAContext setRateLimitElasticProvider(
			@Nullable ThreadPoolProvider<? extends ExecutorService> provider);

	ShardedSpringJDAContext setGatewayPoolProvider(
			@Nullable ThreadPoolProvider<? extends ScheduledExecutorService> provider);

	ShardedSpringJDAContext setCallbackPoolProvider(@Nullable ThreadPoolProvider<? extends ExecutorService> provider);

	ShardedSpringJDAContext setEventPoolProvider(@Nullable ThreadPoolProvider<? extends ExecutorService> provider);

	ShardedSpringJDAContext setAudioPoolProvider(
			@Nullable ThreadPoolProvider<? extends ScheduledExecutorService> provider);

	ShardedSpringJDAContext setEventManagerProvider(
			@NonNull final IntFunction<? extends IEventManager> eventManagerProvider);

	ShardedSpringJDAContext setIdleProvider(@Nullable final IntFunction<Boolean> idleProvider);

	ShardedSpringJDAContext setActivityProvider(@Nullable final IntFunction<? extends Activity> activityProvider);

	ShardedSpringJDAContext setStatusProvider(@Nullable final IntFunction<OnlineStatus> statusProvider);

	ShardedSpringJDAContext addEventListenerProvider(@NonNull final IntFunction<Object> listenerProvider);

	ShardedSpringJDAContext addEventListenerProviders(@NonNull final Collection<IntFunction<Object>> listenerProviders);

	ShardedSpringJDAContext removeEventListenerProvider(@NonNull final IntFunction<Object> listenerProvider);

	ShardedSpringJDAContext removeEventListenerProviders(
			@NonNull final Collection<IntFunction<Object>> listenerProviders);

	ShardedSpringJDAContext setShards(final int... shardIds);

	ShardedSpringJDAContext setShards(final int minShardId, final int maxShardId);

	ShardedSpringJDAContext setShards(@NonNull Collection<Integer> shardIds);

	ShardedSpringJDAContext setShardsTotal(final int shardsTotal);

	// ============================================================================================================================
	// Override return type

	@Override
	ShardedSpringJDAContext setGatewayEncoding(@NonNull GatewayEncoding encoding);

	@Override
	ShardedSpringJDAContext setRawEventsEnabled(boolean enable);

	@Override
	ShardedSpringJDAContext setEventPassthrough(boolean enable);

	@Override
	ShardedSpringJDAContext setRestConfig(@NonNull RestConfig config);

	@Override
	ShardedSpringJDAContext enableCache(@NonNull Collection<CacheFlag> flags);

	@Override
	ShardedSpringJDAContext enableCache(@NonNull CacheFlag flag, @NonNull CacheFlag... flags);

	@Override
	ShardedSpringJDAContext disableCache(@NonNull Collection<CacheFlag> flags);

	@Override
	ShardedSpringJDAContext disableCache(@NonNull CacheFlag flag, @NonNull CacheFlag... flags);

	@Override
	ShardedSpringJDAContext setMemberCachePolicy(@Nullable MemberCachePolicy policy);

	@Override
	ShardedSpringJDAContext setSessionController(@Nullable SessionController controller);

	@Override
	ShardedSpringJDAContext setContextEnabled(boolean enable);

	@Override
	ShardedSpringJDAContext setCompression(@NonNull Compression compression);

	@Override
	ShardedSpringJDAContext setRequestTimeoutRetry(boolean retryOnTimeout);

	@Override
	ShardedSpringJDAContext setHttpClientBuilder(@Nullable OkHttpClient.Builder builder);

	@Override
	ShardedSpringJDAContext setHttpClient(@Nullable OkHttpClient client);

	@Override
	ShardedSpringJDAContext setWebsocketFactory(@Nullable WebSocketFactory factory);

	@Override
	ShardedSpringJDAContext setRateLimitScheduler(@Nullable ScheduledExecutorService pool);

	@Override
	ShardedSpringJDAContext setRateLimitScheduler(@Nullable ScheduledExecutorService pool, boolean automaticShutdown);

	@Override
	ShardedSpringJDAContext setRateLimitElastic(@Nullable ExecutorService pool);

	@Override
	ShardedSpringJDAContext setRateLimitElastic(@Nullable ExecutorService pool, boolean automaticShutdown);

	@Override
	ShardedSpringJDAContext setGatewayPool(@Nullable ScheduledExecutorService pool);

	@Override
	ShardedSpringJDAContext setGatewayPool(@Nullable ScheduledExecutorService pool, boolean automaticShutdown);

	@Override
	ShardedSpringJDAContext setCallbackPool(@Nullable ExecutorService executor);

	@Override
	ShardedSpringJDAContext setCallbackPool(@Nullable ExecutorService executor, boolean automaticShutdown);

	@Override
	ShardedSpringJDAContext setEventPool(@Nullable ExecutorService executor);

	@Override
	ShardedSpringJDAContext setEventPool(@Nullable ExecutorService executor, boolean automaticShutdown);

	@Override
	ShardedSpringJDAContext setAudioPool(@Nullable ScheduledExecutorService pool);

	@Override
	ShardedSpringJDAContext setAudioPool(@Nullable ScheduledExecutorService pool, boolean automaticShutdown);

	@Override
	ShardedSpringJDAContext setBulkDeleteSplittingEnabled(boolean enabled);

	@Override
	ShardedSpringJDAContext setEnableShutdownHook(boolean enable);

	@Override
	ShardedSpringJDAContext setAutoReconnect(boolean autoReconnect);

	@Override
	ShardedSpringJDAContext setAudioSendFactory(@Nullable IAudioSendFactory factory);

	@Override
	ShardedSpringJDAContext setIdle(boolean idle);

	@Override
	ShardedSpringJDAContext setActivity(@Nullable Activity activity);

	@Override
	ShardedSpringJDAContext setStatus(@NonNull OnlineStatus status);

	@Override
	ShardedSpringJDAContext addEventListeners(@NonNull Object... listeners);

	@Override
	default ShardedSpringJDAContext addEventListeners(@NonNull final Collection<Object> listeners) {
		addEventListeners(listeners.toArray());
		return this;
	}

	@Override
	ShardedSpringJDAContext removeEventListeners(@NonNull Object... listeners);

	@Override
	default ShardedSpringJDAContext removeEventListeners(@NonNull final Collection<Object> listeners) {
		removeEventListeners(listeners.toArray());
		return this;
	}

	@Override
	ShardedSpringJDAContext setMaxReconnectDelay(int maxReconnectDelay);

	@Override
	ShardedSpringJDAContext setVoiceDispatchInterceptor(@Nullable VoiceDispatchInterceptor interceptor);

	@Override
	ShardedSpringJDAContext setChunkingFilter(@Nullable ChunkingFilter filter);

	@Override
	ShardedSpringJDAContext setDisabledIntents(@NonNull GatewayIntent intent, @NonNull GatewayIntent... intents);

	@Override
	ShardedSpringJDAContext setDisabledIntents(@Nullable Collection<GatewayIntent> intents);

	@Override
	ShardedSpringJDAContext disableIntents(@NonNull Collection<GatewayIntent> intents);

	@Override
	ShardedSpringJDAContext disableIntents(@NonNull GatewayIntent intent, @NonNull GatewayIntent... intents);

	@Override
	ShardedSpringJDAContext setEnabledIntents(@NonNull GatewayIntent intent, @NonNull GatewayIntent... intents);

	@Override
	ShardedSpringJDAContext setEnabledIntents(@Nullable Collection<GatewayIntent> intents);

	@Override
	ShardedSpringJDAContext enableIntents(@NonNull Collection<GatewayIntent> intents);

	@Override
	ShardedSpringJDAContext enableIntents(@NonNull GatewayIntent intent, @NonNull GatewayIntent... intents);

	@Override
	ShardedSpringJDAContext setLargeThreshold(int threshold);

	@Override
	ShardedSpringJDAContext setMaxBufferSize(int bufferSize);
}
