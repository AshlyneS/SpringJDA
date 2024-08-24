package net.foxgenesis.springJDA.context;

import java.util.Collection;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

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
import okhttp3.OkHttpClient;

/**
 * Interface containing proxy methods for interacting with a wrapped
 * {@link JDABuilder} instance.
 * 
 * @see SpringJDAContext
 * @see SingleSpringJDAContext
 * @author Ashley
 */
public interface SingleSpringJDAContext extends SpringJDAContext {
	SingleSpringJDAContext setContextMap(@Nullable ConcurrentMap<String, String> map);

	SingleSpringJDAContext setEventManager(@Nullable IEventManager manager);

	SingleSpringJDAContext useSharding(int shardId, int shardTotal);

	// ================================================================================================
	// Override return type

	@Override
	SingleSpringJDAContext setGatewayEncoding(@NonNull GatewayEncoding encoding);

	@Override
	SingleSpringJDAContext setRawEventsEnabled(boolean enable);

	@Override
	SingleSpringJDAContext setEventPassthrough(boolean enable);

	@Override
	SingleSpringJDAContext setRestConfig(@NonNull RestConfig config);

	@Override
	SingleSpringJDAContext enableCache(@NonNull Collection<CacheFlag> flags);

	@Override
	SingleSpringJDAContext enableCache(@NonNull CacheFlag flag, @NonNull CacheFlag... flags);

	@Override
	SingleSpringJDAContext disableCache(@NonNull Collection<CacheFlag> flags);

	@Override
	SingleSpringJDAContext disableCache(@NonNull CacheFlag flag, @NonNull CacheFlag... flags);

	@Override
	SingleSpringJDAContext setMemberCachePolicy(@Nullable MemberCachePolicy policy);

	@Override
	SingleSpringJDAContext setSessionController(@Nullable SessionController controller);

	@Override
	SingleSpringJDAContext setContextEnabled(boolean enable);

	@Override
	SingleSpringJDAContext setCompression(@NonNull Compression compression);

	@Override
	SingleSpringJDAContext setRequestTimeoutRetry(boolean retryOnTimeout);

	@Override
	SingleSpringJDAContext setHttpClientBuilder(@Nullable OkHttpClient.Builder builder);

	@Override
	SingleSpringJDAContext setHttpClient(@Nullable OkHttpClient client);

	@Override
	SingleSpringJDAContext setWebsocketFactory(@Nullable WebSocketFactory factory);

	@Override
	SingleSpringJDAContext setRateLimitScheduler(@Nullable ScheduledExecutorService pool);

	@Override
	SingleSpringJDAContext setRateLimitScheduler(@Nullable ScheduledExecutorService pool, boolean automaticShutdown);

	@Override
	SingleSpringJDAContext setRateLimitElastic(@Nullable ExecutorService pool);

	@Override
	SingleSpringJDAContext setRateLimitElastic(@Nullable ExecutorService pool, boolean automaticShutdown);

	@Override
	SingleSpringJDAContext setGatewayPool(@Nullable ScheduledExecutorService pool);

	@Override
	SingleSpringJDAContext setGatewayPool(@Nullable ScheduledExecutorService pool, boolean automaticShutdown);

	@Override
	SingleSpringJDAContext setCallbackPool(@Nullable ExecutorService executor);

	@Override
	SingleSpringJDAContext setCallbackPool(@Nullable ExecutorService executor, boolean automaticShutdown);

	@Override
	SingleSpringJDAContext setEventPool(@Nullable ExecutorService executor);

	@Override
	SingleSpringJDAContext setEventPool(@Nullable ExecutorService executor, boolean automaticShutdown);

	@Override
	SingleSpringJDAContext setAudioPool(@Nullable ScheduledExecutorService pool);

	@Override
	SingleSpringJDAContext setAudioPool(@Nullable ScheduledExecutorService pool, boolean automaticShutdown);

	@Override
	SingleSpringJDAContext setBulkDeleteSplittingEnabled(boolean enabled);

	@Override
	SingleSpringJDAContext setEnableShutdownHook(boolean enable);

	@Override
	SingleSpringJDAContext setAutoReconnect(boolean autoReconnect);

	@Override
	SingleSpringJDAContext setAudioSendFactory(@Nullable IAudioSendFactory factory);

	@Override
	SingleSpringJDAContext setIdle(boolean idle);

	@Override
	SingleSpringJDAContext setActivity(@Nullable Activity activity);

	@Override
	SingleSpringJDAContext setStatus(@NonNull OnlineStatus status);

	@Override
	SingleSpringJDAContext addEventListeners(@NonNull Object... listeners);

	@Override
	default SingleSpringJDAContext addEventListeners(@NonNull final Collection<Object> listeners) {
		addEventListeners(listeners.toArray());
		return this;
	}

	@Override
	SingleSpringJDAContext removeEventListeners(@NonNull Object... listeners);

	@Override
	default SingleSpringJDAContext removeEventListeners(@NonNull final Collection<Object> listeners) {
		removeEventListeners(listeners.toArray());
		return this;
	}

	@Override
	SingleSpringJDAContext setMaxReconnectDelay(int maxReconnectDelay);

	@Override
	SingleSpringJDAContext setVoiceDispatchInterceptor(@Nullable VoiceDispatchInterceptor interceptor);

	@Override
	SingleSpringJDAContext setChunkingFilter(@Nullable ChunkingFilter filter);

	@Override
	SingleSpringJDAContext setDisabledIntents(@NonNull GatewayIntent intent, @NonNull GatewayIntent... intents);

	@Override
	SingleSpringJDAContext setDisabledIntents(@Nullable Collection<GatewayIntent> intents);

	@Override
	SingleSpringJDAContext disableIntents(@NonNull Collection<GatewayIntent> intents);

	@Override
	SingleSpringJDAContext disableIntents(@NonNull GatewayIntent intent, @NonNull GatewayIntent... intents);

	@Override
	SingleSpringJDAContext setEnabledIntents(@NonNull GatewayIntent intent, @NonNull GatewayIntent... intents);

	@Override
	SingleSpringJDAContext setEnabledIntents(@Nullable Collection<GatewayIntent> intents);

	@Override
	SingleSpringJDAContext enableIntents(@NonNull Collection<GatewayIntent> intents);

	@Override
	SingleSpringJDAContext enableIntents(@NonNull GatewayIntent intent, @NonNull GatewayIntent... intents);

	@Override
	SingleSpringJDAContext setLargeThreshold(int threshold);

	@Override
	SingleSpringJDAContext setMaxBufferSize(int bufferSize);
}
