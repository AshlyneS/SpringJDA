package net.foxgenesis.springJDA.context.impl;

import java.util.Collection;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

import org.springframework.beans.BeansException;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import com.neovisionaries.ws.client.WebSocketFactory;

import net.dv8tion.jda.api.GatewayEncoding;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.audio.factory.IAudioSendFactory;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.hooks.IEventManager;
import net.dv8tion.jda.api.hooks.VoiceDispatchInterceptor;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.RestConfig;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.Compression;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.SessionController;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.foxgenesis.springJDA.context.SingleSpringJDAContext;
import okhttp3.OkHttpClient;
import okhttp3.OkHttpClient.Builder;

/**
 * Default implementation of a {@link SingleSpringJDAContext}.
 * 
 * @author Ashley
 * @see SingleSpringJDAContext
 */
public class DefaultSingleSpringJDAContext extends AbstractSpringJDAContext implements SingleSpringJDAContext {

	private final JDABuilder builder;

	public DefaultSingleSpringJDAContext(@NonNull String token) {
		super(token);
		this.builder = JDABuilder.createLight(token);
	}

	@Override
	public SingleSpringJDAContext setGatewayEncoding(@NonNull GatewayEncoding encoding) {
		builder.setGatewayEncoding(encoding);
		return this;
	}

	@Override
	public SingleSpringJDAContext setRawEventsEnabled(boolean enable) {
		builder.setRawEventsEnabled(enable);
		return this;
	}

	@Override
	public SingleSpringJDAContext setEventPassthrough(boolean enable) {
		builder.setEventPassthrough(enable);
		return this;
	}

	@Override
	public SingleSpringJDAContext setRestConfig(@NonNull RestConfig config) {
		builder.setRestConfig(config);
		return this;
	}

	@Override
	public SingleSpringJDAContext enableCache(@NonNull Collection<CacheFlag> flags) {
		builder.enableCache(flags);
		return this;
	}

	@Override
	public SingleSpringJDAContext enableCache(@NonNull CacheFlag flag, @NonNull CacheFlag... flags) {
		builder.enableCache(flag, flags);
		return this;
	}

	@Override
	public SingleSpringJDAContext disableCache(@NonNull Collection<CacheFlag> flags) {
		builder.disableCache(flags);
		return this;
	}

	@Override
	public SingleSpringJDAContext disableCache(@NonNull CacheFlag flag, @NonNull CacheFlag... flags) {
		builder.disableCache(flag, flags);
		return this;
	}

	@Override
	public SingleSpringJDAContext setMemberCachePolicy(@Nullable MemberCachePolicy policy) {
		builder.setMemberCachePolicy(policy);
		return this;
	}

	@Override
	public SingleSpringJDAContext setSessionController(@Nullable SessionController controller) {
		builder.setSessionController(controller);
		return this;
	}

	@Override
	public SingleSpringJDAContext setContextEnabled(boolean enable) {
		builder.setContextEnabled(enable);
		return this;
	}

	@Override
	public SingleSpringJDAContext setContextMap(@Nullable ConcurrentMap<String, String> map) {
		builder.setContextMap(map);
		return this;
	}

	@Override
	public SingleSpringJDAContext setCompression(@NonNull Compression compression) {
		builder.setCompression(compression);
		return this;
	}

	@Override
	public SingleSpringJDAContext setRequestTimeoutRetry(boolean retryOnTimeout) {
		builder.setRequestTimeoutRetry(retryOnTimeout);
		return this;
	}

	@Override
	public SingleSpringJDAContext setHttpClientBuilder(@Nullable Builder builder) {
		this.builder.setHttpClientBuilder(builder);
		return this;
	}

	@Override
	public SingleSpringJDAContext setHttpClient(@Nullable OkHttpClient client) {
		builder.setHttpClient(client);
		return this;
	}

	@Override
	public SingleSpringJDAContext setWebsocketFactory(@Nullable WebSocketFactory factory) {
		builder.setWebsocketFactory(factory);
		return this;
	}

	@Override
	public SingleSpringJDAContext setRateLimitScheduler(@Nullable ScheduledExecutorService pool) {
		builder.setRateLimitScheduler(pool);
		return this;
	}

	@Override
	public SingleSpringJDAContext setRateLimitScheduler(@Nullable ScheduledExecutorService pool,
			boolean automaticShutdown) {
		builder.setRateLimitScheduler(pool, automaticShutdown);
		return this;
	}

	@Override
	public SingleSpringJDAContext setRateLimitElastic(@Nullable ExecutorService pool) {
		builder.setRateLimitElastic(pool);
		return this;
	}

	@Override
	public SingleSpringJDAContext setRateLimitElastic(@Nullable ExecutorService pool, boolean automaticShutdown) {
		builder.setRateLimitElastic(pool, automaticShutdown);
		return this;
	}

	@Override
	public SingleSpringJDAContext setGatewayPool(@Nullable ScheduledExecutorService pool) {
		builder.setGatewayPool(pool);
		return this;
	}

	@Override
	public SingleSpringJDAContext setGatewayPool(@Nullable ScheduledExecutorService pool, boolean automaticShutdown) {
		builder.setGatewayPool(pool, automaticShutdown);
		return this;
	}

	@Override
	public SingleSpringJDAContext setCallbackPool(@Nullable ExecutorService executor) {
		builder.setCallbackPool(executor);
		return this;
	}

	@Override
	public SingleSpringJDAContext setCallbackPool(@Nullable ExecutorService executor, boolean automaticShutdown) {
		builder.setCallbackPool(executor, automaticShutdown);
		return this;
	}

	@Override
	public SingleSpringJDAContext setEventPool(@Nullable ExecutorService executor) {
		builder.setEventPool(executor);
		return this;
	}

	@Override
	public SingleSpringJDAContext setEventPool(@Nullable ExecutorService executor, boolean automaticShutdown) {
		builder.setEventPool(executor, automaticShutdown);
		return this;
	}

	@Override
	public SingleSpringJDAContext setAudioPool(@Nullable ScheduledExecutorService pool) {
		builder.setAudioPool(pool);
		return this;
	}

	@Override
	public SingleSpringJDAContext setAudioPool(@Nullable ScheduledExecutorService pool, boolean automaticShutdown) {
		builder.setAudioPool(pool, automaticShutdown);
		return this;
	}

	@Override
	public SingleSpringJDAContext setBulkDeleteSplittingEnabled(boolean enabled) {
		builder.setBulkDeleteSplittingEnabled(enabled);
		return this;
	}

	@Override
	public SingleSpringJDAContext setEnableShutdownHook(boolean enable) {
		builder.setEnableShutdownHook(enable);
		return this;
	}

	@Override
	public SingleSpringJDAContext setAutoReconnect(boolean autoReconnect) {
		builder.setAutoReconnect(autoReconnect);
		return this;
	}

	@Override
	public SingleSpringJDAContext setEventManager(@Nullable IEventManager manager) {
		builder.setEventManager(manager);
		return this;
	}

	@Override
	public SingleSpringJDAContext setAudioSendFactory(@Nullable IAudioSendFactory factory) {
		builder.setAudioSendFactory(factory);
		return this;
	}

	@Override
	public SingleSpringJDAContext setIdle(boolean idle) {
		builder.setIdle(idle);
		return this;
	}

	@Override
	public SingleSpringJDAContext setActivity(@Nullable Activity activity) {
		builder.setActivity(activity);
		return this;
	}

	@Override
	public SingleSpringJDAContext setStatus(@NonNull OnlineStatus status) {
		builder.setStatus(status);
		return this;
	}

	@Override
	public SingleSpringJDAContext addEventListeners(@NonNull Object... listeners) {
		builder.addEventListeners(listeners);
		return this;
	}

	@Override
	public SingleSpringJDAContext removeEventListeners(@NonNull Object... listeners) {
		builder.removeEventListeners(listeners);
		return this;
	}

	@Override
	public SingleSpringJDAContext setMaxReconnectDelay(int maxReconnectDelay) {
		builder.setMaxReconnectDelay(maxReconnectDelay);
		return this;
	}

	@Override
	public SingleSpringJDAContext useSharding(int shardId, int shardTotal) {
		builder.useSharding(shardId, shardTotal);
		return this;
	}

	@Override
	public SingleSpringJDAContext setVoiceDispatchInterceptor(@Nullable VoiceDispatchInterceptor interceptor) {
		builder.setVoiceDispatchInterceptor(interceptor);
		return this;
	}

	@Override
	public SingleSpringJDAContext setChunkingFilter(@Nullable ChunkingFilter filter) {
		builder.setChunkingFilter(filter);
		return this;
	}

	@Override
	public SingleSpringJDAContext setDisabledIntents(@NonNull GatewayIntent intent, @NonNull GatewayIntent... intents) {
		builder.setDisabledIntents(intent, intents);
		return this;
	}

	@Override
	public SingleSpringJDAContext setDisabledIntents(@Nullable Collection<GatewayIntent> intents) {
		builder.setDisabledIntents(intents);
		return this;
	}

	@Override
	public SingleSpringJDAContext disableIntents(@NonNull Collection<GatewayIntent> intents) {
		builder.disableIntents(intents);
		return this;
	}

	@Override
	public SingleSpringJDAContext disableIntents(@NonNull GatewayIntent intent, @NonNull GatewayIntent... intents) {
		builder.disableIntents(intent, intents);
		return this;
	}

	@Override
	public SingleSpringJDAContext setEnabledIntents(@NonNull GatewayIntent intent, @NonNull GatewayIntent... intents) {
		builder.setEnabledIntents(intent, intents);
		return this;
	}

	@Override
	public SingleSpringJDAContext setEnabledIntents(@Nullable Collection<GatewayIntent> intents) {
		builder.setEnabledIntents(intents);
		return this;
	}

	@Override
	public SingleSpringJDAContext enableIntents(@NonNull Collection<GatewayIntent> intents) {
		builder.setEnabledIntents(intents);
		return this;
	}

	@Override
	public SingleSpringJDAContext enableIntents(@NonNull GatewayIntent intent, @NonNull GatewayIntent... intents) {
		builder.setEnabledIntents(intent, intents);
		return this;
	}

	@Override
	public SingleSpringJDAContext setLargeThreshold(int threshold) {
		builder.setLargeThreshold(threshold);
		return this;
	}

	@Override
	public SingleSpringJDAContext setMaxBufferSize(int bufferSize) {
		builder.setMaxBufferSize(bufferSize);
		return this;
	}

	public JDABuilder build() throws BeansException {
		return builder;
	}
}
