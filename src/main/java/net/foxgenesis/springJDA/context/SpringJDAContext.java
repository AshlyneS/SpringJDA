package net.foxgenesis.springJDA.context;

import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.IntFunction;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import com.neovisionaries.ws.client.WebSocketFactory;

import net.dv8tion.jda.api.GatewayEncoding;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.audio.factory.IAudioSendFactory;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.hooks.VoiceDispatchInterceptor;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.RestConfig;
import net.dv8tion.jda.api.sharding.DefaultShardManager;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.api.sharding.ThreadPoolProvider;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.Compression;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.SessionController;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import okhttp3.OkHttpClient;

/**
 * Interface containing common proxy methods for interacting with a wrapped
 * {@link JDABuilder} or {@link DefaultShardManagerBuilder}.
 * <p>
 * Further methods of {@link JDABuilder} or {@link DefaultShardManagerBuilder}
 * can be obtained through {@link SingleSpringJDAContext} or
 * {@link ShardedSpringJDAContext} instance respectively.
 * 
 * @see SingleSpringJDAContext
 * @see ShardedSpringJDAContext
 * @author Ashley
 */
public interface SpringJDAContext {
	/**
	 * Choose which {@link GatewayEncoding} JDA should use.
	 *
	 * @param encoding The {@link GatewayEncoding} (default: JSON)
	 *
	 * @throws IllegalArgumentException If null is provided
	 *
	 * @return The SpringJDAContext instance. Useful for chaining.
	 */
	SpringJDAContext setGatewayEncoding(@NonNull GatewayEncoding encoding);

	/**
	 * Whether JDA should fire {@link net.dv8tion.jda.api.events.RawGatewayEvent}
	 * for every discord event. <br>
	 * Default: {@code false}
	 *
	 * @param enable True, if JDA should fire
	 *               {@link net.dv8tion.jda.api.events.RawGatewayEvent}.
	 *
	 * @return The SpringJDAContext instance. Useful for chaining.
	 */
	SpringJDAContext setRawEventsEnabled(boolean enable);

	/**
	 * Whether JDA should store the raw
	 * {@link net.dv8tion.jda.api.utils.data.DataObject DataObject} for every
	 * discord event, accessible through
	 * {@link net.dv8tion.jda.api.events.GenericEvent#getRawData() getRawData()}.
	 * <br>
	 * You can expect to receive the full gateway message payload, including
	 * sequence, event name and dispatch type of the events <br>
	 * You can read more about payloads
	 * <a href="https://discord.com/developers/docs/topics/gateway" target=
	 * "_blank">here</a> and the different events <a href=
	 * "https://discord.com/developers/docs/topics/gateway#commands-and-events-gateway-events"
	 * target="_blank">here</a>. <br>
	 * Warning: be aware that enabling this could consume a lot of memory if your
	 * event objects have a long lifetime. <br>
	 * Default: {@code false}
	 *
	 * @param enable True, if JDA should add the raw
	 *               {@link net.dv8tion.jda.api.utils.data.DataObject DataObject} to
	 *               every discord event.
	 *
	 * @return The SpringJDAContext instance. Useful for chaining.
	 *
	 * @see Event#getRawData()
	 */
	SpringJDAContext setEventPassthrough(boolean enable);

	/**
	 * Custom {@link RestConfig} to use. <br>
	 * This can be used to customize how rate-limits are handled and configure a
	 * custom http proxy.
	 *
	 * @param config The {@link RestConfig} to use
	 *
	 * @throws IllegalArgumentException If null is provided
	 *
	 * @return The SpringJDAContext instance. Useful for chaining.
	 */
	SpringJDAContext setRestConfig(@NonNull RestConfig config);

	/**
	 * Enable specific cache flags. <br>
	 * This will not disable any currently set cache flags.
	 *
	 * @param flags The {@link CacheFlag CacheFlags} to enable
	 *
	 * @throws IllegalArgumentException If provided with null
	 *
	 * @return The SpringJDAContext instance. Useful for chaining.
	 *
	 * @see #enableCache(CacheFlag, CacheFlag...)
	 * @see #disableCache(Collection)
	 */
	SpringJDAContext enableCache(@NonNull Collection<CacheFlag> flags);

	/**
	 * Enable specific cache flags. <br>
	 * This will not disable any currently set cache flags.
	 *
	 * @param flag  {@link CacheFlag} to enable
	 * @param flags Other flags to enable
	 *
	 * @throws IllegalArgumentException If provided with null
	 *
	 * @return The SpringJDAContext instance. Useful for chaining.
	 *
	 * @see #enableCache(Collection)
	 * @see #disableCache(CacheFlag, CacheFlag...)
	 */
	SpringJDAContext enableCache(@NonNull CacheFlag flag, @NonNull CacheFlag... flags);

	/**
	 * Disable specific cache flags. <br>
	 * This will not enable any currently unset cache flags.
	 *
	 * @param flags The {@link CacheFlag CacheFlags} to disable
	 *
	 * @throws IllegalArgumentException If provided with null
	 *
	 * @return The SpringJDAContext instance. Useful for chaining.
	 *
	 * @see #disableCache(CacheFlag, CacheFlag...)
	 * @see #enableCache(Collection)
	 */
	SpringJDAContext disableCache(@NonNull Collection<CacheFlag> flags);

	/**
	 * Disable specific cache flags. <br>
	 * This will not enable any currently unset cache flags.
	 *
	 * @param flag  {@link CacheFlag} to disable
	 * @param flags Other flags to disable
	 *
	 * @throws IllegalArgumentException If provided with null
	 *
	 * @return The SpringJDAContext instance. Useful for chaining.
	 *
	 * @see #disableCache(Collection)
	 * @see #enableCache(CacheFlag, CacheFlag...)
	 */
	SpringJDAContext disableCache(@NonNull CacheFlag flag, @NonNull CacheFlag... flags);

	/**
	 * Configure the member caching policy. This will decide whether to cache a
	 * member (and its respective user). <br>
	 * All members are cached by default. If a guild is enabled for chunking, all
	 * members will be cached for it.
	 *
	 * <p>
	 * You can use this to define a custom caching policy that will greatly improve
	 * memory usage.
	 * <p>
	 * It is not recommended to disable {@link GatewayIntent#GUILD_MEMBERS
	 * GatewayIntent.GUILD_MEMBERS} when using {@link MemberCachePolicy#ALL
	 * MemberCachePolicy.ALL} as the members cannot be removed from cache by a leave
	 * event without this intent.
	 *
	 * <p>
	 * <b>Example</b><br>
	 * 
	 * <pre>{@code
	 * public void configureCache(SpringJDAContext builder) {
	 * 	// Cache members who are in a voice channel
	 * 	MemberCachePolicy policy = MemberCachePolicy.VOICE;
	 * 	// Cache members who are in a voice channel
	 * 	// AND are also online
	 * 	policy = policy.and(MemberCachePolicy.ONLINE);
	 * 	// Cache members who are in a voice channel
	 * 	// AND are also online
	 * 	// OR are the owner of the guild
	 * 	policy = policy.or(MemberCachePolicy.OWNER);
	 * 	// Cache members who have a role with the name "Moderator"
	 * 	policy = (member) -> member.getRoles().stream().map(Role::getName).anyMatch("Moderator"::equals);
	 *
	 * 	builder.setMemberCachePolicy(policy);
	 * }
	 * }</pre>
	 *
	 * @param policy The {@link MemberCachePolicy} or null to use default
	 *               {@link MemberCachePolicy#ALL}
	 *
	 * @return The SpringJDAContext instance. Useful for chaining.
	 *
	 * @see MemberCachePolicy
	 * @see #setEnabledIntents(Collection)
	 *
	 * @since 4.2.0
	 */
	SpringJDAContext setMemberCachePolicy(@Nullable MemberCachePolicy policy);

	/**
	 * Sets the {@link net.dv8tion.jda.api.utils.SessionController
	 * SessionController} for the resulting ShardManager instance. This can be used
	 * to sync behaviour and state between shards of a bot and should be one and the
	 * same instance on all builders for the shards.
	 *
	 * @param controller The {@link net.dv8tion.jda.api.utils.SessionController
	 *                   SessionController} to use
	 *
	 * @return The SpringJDAContext instance. Useful for chaining.
	 *
	 * @see net.dv8tion.jda.api.utils.SessionControllerAdapter
	 *      SessionControllerAdapter
	 */
	SpringJDAContext setSessionController(@Nullable SessionController controller);

	/**
	 * Whether JDA should use a synchronized MDC context for all of its controlled
	 * threads. <br>
	 * Default: {@code true}
	 *
	 * @param enable True, if JDA should provide an MDC context map
	 *
	 * @return The SpringJDAContext instance. Useful for chaining.
	 *
	 * @see <a href="https://www.slf4j.org/api/org/slf4j/MDC.html" target=
	 *      "_blank">MDC Javadoc</a>
	 * @see #setContextMap(java.util.function.IntFunction)
	 */
	SpringJDAContext setContextEnabled(boolean enable);

	/**
	 * Sets the compression algorithm used with the gateway connection, this will
	 * decrease the amount of used bandwidth for the running bot instance for the
	 * cost of a few extra cycles for decompression. Compression can be entirely
	 * disabled by setting this to
	 * {@link net.dv8tion.jda.api.utils.Compression#NONE}. <br>
	 * <b>Default: {@link net.dv8tion.jda.api.utils.Compression#ZLIB}</b>
	 *
	 * <p>
	 * <b>We recommend to keep this on the default unless you have issues with the
	 * decompression</b> <br>
	 * This mode might become obligatory in a future version, do not rely on this
	 * switch to stay.
	 *
	 * @param compression The compression algorithm to use for the gateway
	 *                    connection
	 *
	 * @throws java.lang.IllegalArgumentException If provided with null
	 *
	 * @return The SpringJDAContext instance. Useful for chaining.
	 *
	 * @see <a href=
	 *      "https://discord.com/developers/docs/topics/gateway#transport-compression"
	 *      target="_blank">Official Discord Documentation - Transport
	 *      Compression</a>
	 */
	SpringJDAContext setCompression(@NonNull Compression compression);

	/**
	 * Whether the Requester should retry when a
	 * {@link java.net.SocketTimeoutException SocketTimeoutException} occurs. <br>
	 * <b>Default</b>: {@code true}
	 *
	 * <p>
	 * This value can be changed at any time with
	 * {@link net.dv8tion.jda.api.JDA#setRequestTimeoutRetry(boolean)
	 * JDA.setRequestTimeoutRetry(boolean)}!
	 *
	 * @param retryOnTimeout True, if the Request should retry once on a socket
	 *                       timeout
	 *
	 * @return The SpringJDAContext instance. Useful for chaining.
	 */
	SpringJDAContext setRequestTimeoutRetry(boolean retryOnTimeout);

	/**
	 * Sets the {@link okhttp3.OkHttpClient.Builder Builder} that will be used by
	 * JDA's requester. This can be used to set things such as connection timeout
	 * and proxy.
	 *
	 * @param builder The new {@link okhttp3.OkHttpClient.Builder
	 *                OkHttpClient.Builder} to use.
	 *
	 * @return The SpringJDAContext instance. Useful for chaining.
	 */
	SpringJDAContext setHttpClientBuilder(@Nullable OkHttpClient.Builder builder);

	/**
	 * Sets the {@link okhttp3.OkHttpClient OkHttpClient} that will be used by JDAs
	 * requester. <br>
	 * This can be used to set things such as connection timeout and proxy.
	 *
	 * @param client The new {@link okhttp3.OkHttpClient OkHttpClient} to use
	 *
	 * @return The SpringJDAContext instance. Useful for chaining.
	 */
	SpringJDAContext setHttpClient(@Nullable OkHttpClient client);

	/**
	 * Sets the {@link com.neovisionaries.ws.client.WebSocketFactory
	 * WebSocketFactory} that will be used by JDA's websocket client. This can be
	 * used to set things such as connection timeout and proxy.
	 *
	 * @param factory The new {@link com.neovisionaries.ws.client.WebSocketFactory
	 *                WebSocketFactory} to use.
	 *
	 * @return The SpringJDAContext instance. Useful for chaining.
	 */
	SpringJDAContext setWebsocketFactory(@Nullable WebSocketFactory factory);

	/**
	 * Sets the {@link ScheduledExecutorService ScheduledExecutorService} that
	 * should be used in the JDA rate-limit handler. Changing this can drastically
	 * change the JDA behavior for RestAction execution and should be handled
	 * carefully. <b>Only change this pool if you know what you're doing.</b> <br>
	 * This will override the rate-limit pool provider set from
	 * {@link #setRateLimitPoolProvider(ThreadPoolProvider)}. <br>
	 * <b>This automatically disables the automatic shutdown of the rate-limit pool,
	 * you can enable it using
	 * {@link #setRateLimitPool(ScheduledExecutorService, boolean)
	 * setRateLimiPool(executor, true)}</b>
	 *
	 * <p>
	 * This is used mostly by the Rate-Limiter to handle backoff delays by using
	 * scheduled executions. Besides that it is also used by planned execution for
	 * {@link net.dv8tion.jda.api.requests.RestAction#queueAfter(long, TimeUnit)}
	 * and similar methods. Requests are handed off to the
	 * {@link #setRateLimitElastic(ExecutorService) elastic pool} for blocking
	 * execution.
	 *
	 * <p>
	 * Default: Shared {@link ScheduledThreadPoolExecutor} with ({@code 2 * }
	 * log({@link #setShardsTotal(int) shard_total})) threads.
	 *
	 * @param pool The thread-pool to use for rate-limit handling
	 *
	 * @return The SpringJDAContext instance. Useful for chaining.
	 */
	SpringJDAContext setRateLimitScheduler(@Nullable ScheduledExecutorService pool);

	/**
	 * Sets the {@link ScheduledExecutorService ScheduledExecutorService} that
	 * should be used in the JDA rate-limit handler. Changing this can drastically
	 * change the JDA behavior for RestAction execution and should be handled
	 * carefully. <b>Only change this pool if you know what you're doing.</b> <br>
	 * This will override the rate-limit pool provider set from
	 * {@link #setRateLimitPoolProvider(ThreadPoolProvider)}.
	 *
	 * <p>
	 * This is used mostly by the Rate-Limiter to handle backoff delays by using
	 * scheduled executions. Besides that it is also used by planned execution for
	 * {@link net.dv8tion.jda.api.requests.RestAction#queueAfter(long, TimeUnit)}
	 * and similar methods. Requests are handed off to the
	 * {@link #setRateLimitElastic(ExecutorService) elastic pool} for blocking
	 * execution.
	 *
	 * <p>
	 * Default: Shared {@link ScheduledThreadPoolExecutor} with ({@code 2 * }
	 * log({@link #setShardsTotal(int) shard_total})) threads.
	 *
	 * @param pool              The thread-pool to use for rate-limit handling
	 * @param automaticShutdown Whether {@link net.dv8tion.jda.api.JDA#shutdown()}
	 *                          should automatically shutdown this pool
	 *
	 * @return The SpringJDAContext instance. Useful for chaining.
	 */
	SpringJDAContext setRateLimitScheduler(@Nullable ScheduledExecutorService pool, boolean automaticShutdown);

	/**
	 * Sets the {@link ExecutorService} that should be used in the JDA request
	 * handler. Changing this can drastically change the JDA behavior for RestAction
	 * execution and should be handled carefully. <b>Only change this pool if you
	 * know what you're doing.</b> <br>
	 * This will override the rate-limit pool provider set from
	 * {@link #setRateLimitElasticProvider(ThreadPoolProvider)}. <br>
	 * <b>This automatically disables the automatic shutdown of the rate-limit
	 * elastic pool, you can enable it using
	 * {@link #setRateLimitElastic(ExecutorService, boolean)
	 * setRateLimitElastic(executor, true)}</b>
	 *
	 * <p>
	 * This is used mostly by the Rate-Limiter to execute the blocking HTTP requests
	 * at runtime.
	 *
	 * <p>
	 * Default: {@link Executors#newCachedThreadPool()} shared between all shards.
	 *
	 * @param pool The thread-pool to use for executing http requests
	 *
	 * @return The SpringJDAContext instance. Useful for chaining.
	 */
	SpringJDAContext setRateLimitElastic(@Nullable ExecutorService pool);

	/**
	 * Sets the {@link ExecutorService} that should be used in the JDA request
	 * handler. Changing this can drastically change the JDA behavior for RestAction
	 * execution and should be handled carefully. <b>Only change this pool if you
	 * know what you're doing.</b> <br>
	 * This will override the rate-limit pool provider set from
	 * {@link #setRateLimitElasticProvider(ThreadPoolProvider)}. <br>
	 * <b>This automatically disables the automatic shutdown of the rate-limit
	 * elastic pool, you can enable it using
	 * {@link #setRateLimitElastic(ExecutorService, boolean)
	 * setRateLimitElastic(executor, true)}</b>
	 *
	 * <p>
	 * This is used mostly by the Rate-Limiter to execute the blocking HTTP requests
	 * at runtime.
	 *
	 * <p>
	 * Default: {@link Executors#newCachedThreadPool()} shared between all shards.
	 *
	 * @param pool              The thread-pool to use for executing http requests
	 * @param automaticShutdown Whether {@link net.dv8tion.jda.api.JDA#shutdown()}
	 *                          should automatically shutdown this pool
	 *
	 * @return The SpringJDAContext instance. Useful for chaining.
	 */
	SpringJDAContext setRateLimitElastic(@Nullable ExecutorService pool, boolean automaticShutdown);

	/**
	 * Sets the {@link ScheduledExecutorService ScheduledExecutorService} that
	 * should be used for the JDA main WebSocket workers. <br>
	 * <b>Only change this pool if you know what you're doing.</b> <br>
	 * This will override the worker pool provider set from
	 * {@link #setGatewayPoolProvider(ThreadPoolProvider)}. <br>
	 * <b>This automatically disables the automatic shutdown of the main-ws pools,
	 * you can enable it using
	 * {@link #setGatewayPool(ScheduledExecutorService, boolean)
	 * setGatewayPoolProvider(pool, true)}</b>
	 *
	 * <p>
	 * This is used to send various forms of session updates such as:
	 * <ul>
	 * <li>Voice States - (Dis-)Connecting from channels</li>
	 * <li>Presence - Changing current activity or online status</li>
	 * <li>Guild Setup - Requesting Members of newly joined guilds</li>
	 * <li>Heartbeats - Regular updates to keep the connection alive (usually once a
	 * minute)</li>
	 * </ul>
	 * When nothing has to be sent the pool will only be used every 500 milliseconds
	 * to check the queue for new payloads. Once a new payload is sent we switch to
	 * "rapid mode" which means more tasks will be submitted until no more payloads
	 * have to be sent.
	 *
	 * <p>
	 * Default: Shared {@link ScheduledThreadPoolExecutor} with
	 * ({@code log}({@link #setShardsTotal(int) shard_total})) threads.
	 *
	 * @param pool The thread-pool to use for main WebSocket workers
	 *
	 * @return The SpringJDAContext instance. Useful for chaining.
	 */
	SpringJDAContext setGatewayPool(@Nullable ScheduledExecutorService pool);

	/**
	 * Sets the {@link ScheduledExecutorService ScheduledExecutorService} that
	 * should be used for the JDA main WebSocket workers. <br>
	 * <b>Only change this pool if you know what you're doing.</b> <br>
	 * This will override the worker pool provider set from
	 * {@link #setGatewayPoolProvider(ThreadPoolProvider)}.
	 *
	 * <p>
	 * This is used to send various forms of session updates such as:
	 * <ul>
	 * <li>Voice States - (Dis-)Connecting from channels</li>
	 * <li>Presence - Changing current activity or online status</li>
	 * <li>Guild Setup - Requesting Members of newly joined guilds</li>
	 * <li>Heartbeats - Regular updates to keep the connection alive (usually once a
	 * minute)</li>
	 * </ul>
	 * When nothing has to be sent the pool will only be used every 500 milliseconds
	 * to check the queue for new payloads. Once a new payload is sent we switch to
	 * "rapid mode" which means more tasks will be submitted until no more payloads
	 * have to be sent.
	 *
	 * <p>
	 * Default: Shared {@link ScheduledThreadPoolExecutor} with
	 * ({@code log}({@link #setShardsTotal(int) shard_total})) threads.
	 *
	 * @param pool              The thread-pool to use for main WebSocket workers
	 * @param automaticShutdown Whether {@link net.dv8tion.jda.api.JDA#shutdown()}
	 *                          should automatically shutdown this pool
	 *
	 * @return The SpringJDAContext instance. Useful for chaining.
	 */
	SpringJDAContext setGatewayPool(@Nullable ScheduledExecutorService pool, boolean automaticShutdown);

	/**
	 * Sets the {@link ExecutorService ExecutorService} that should be used in the
	 * JDA callback handler which mostly consists of
	 * {@link net.dv8tion.jda.api.requests.RestAction RestAction} callbacks. By
	 * default JDA will use {@link ForkJoinPool#commonPool()} <br>
	 * <b>Only change this pool if you know what you're doing. <br>
	 * This automatically disables the automatic shutdown of the callback pools, you
	 * can enable it using {@link #setCallbackPool(ExecutorService, boolean)
	 * setCallbackPool(executor, true)}</b>
	 *
	 * <p>
	 * This is used to handle callbacks of {@link RestAction#queue()}, similarly it
	 * is used to finish {@link RestAction#submit()} and
	 * {@link RestAction#complete()} tasks which build on queue.
	 *
	 * <p>
	 * Default: {@link ForkJoinPool#commonPool()}
	 *
	 * @param executor The thread-pool to use for callback handling
	 *
	 * @return The SpringJDAContext instance. Useful for chaining.
	 */
	SpringJDAContext setCallbackPool(@Nullable ExecutorService executor);

	/**
	 * Sets the {@link ExecutorService ExecutorService} that should be used in the
	 * JDA callback handler which mostly consists of
	 * {@link net.dv8tion.jda.api.requests.RestAction RestAction} callbacks. By
	 * default JDA will use {@link ForkJoinPool#commonPool()} <br>
	 * <b>Only change this pool if you know what you're doing.</b>
	 *
	 * <p>
	 * This is used to handle callbacks of {@link RestAction#queue()}, similarly it
	 * is used to finish {@link RestAction#submit()} and
	 * {@link RestAction#complete()} tasks which build on queue.
	 *
	 * <p>
	 * Default: {@link ForkJoinPool#commonPool()}
	 *
	 * @param executor          The thread-pool to use for callback handling
	 * @param automaticShutdown Whether {@link net.dv8tion.jda.api.JDA#shutdown()}
	 *                          should automatically shutdown this pool
	 *
	 * @return The SpringJDAContext instance. Useful for chaining.
	 */
	SpringJDAContext setCallbackPool(@Nullable ExecutorService executor, boolean automaticShutdown);

	/**
	 * Sets the {@link ExecutorService ExecutorService} that should be used by the
	 * event proxy to schedule events. This will be done on the calling thread by
	 * default.
	 *
	 * <p>
	 * The executor will not be shutdown automatically when the shard is shutdown.
	 * To shut it down automatically use
	 * {@link #setEventPool(ExecutorService, boolean)}.
	 *
	 * <p>
	 * Default: Disabled
	 *
	 * @param executor The executor for the event proxy, or null to use calling
	 *                 thread
	 *
	 * @return The SpringJDAContext instance. Useful for chaining.
	 *
	 * @since 4.2.0
	 */
	SpringJDAContext setEventPool(@Nullable ExecutorService executor);

	/**
	 * Sets the {@link ExecutorService ExecutorService} that should be used by the
	 * event proxy to schedule events. This will be done on the calling thread by
	 * default.
	 *
	 * <p>
	 * Default: Disabled
	 *
	 * @param executor          The executor for the event proxy, or null to use
	 *                          calling thread
	 * @param automaticShutdown True, if the executor should be shutdown when JDA
	 *                          shuts down
	 *
	 * @return The SpringJDAContext instance. Useful for chaining.
	 *
	 * @since 4.2.0
	 */
	SpringJDAContext setEventPool(@Nullable ExecutorService executor, boolean automaticShutdown);

	/**
	 * Sets the {@link ScheduledExecutorService ScheduledExecutorService} used by
	 * the audio WebSocket connection. Used for sending keepalives and closing the
	 * connection. <br>
	 * <b>Only change this pool if you know what you're doing.</b>
	 *
	 * <p>
	 * Default: {@link ScheduledThreadPoolExecutor} with 1 thread
	 *
	 * @param pool The thread-pool to use for the audio WebSocket
	 *
	 * @return The SpringJDAContext instance. Useful for chaining.
	 *
	 * @since 4.2.1
	 */
	SpringJDAContext setAudioPool(@Nullable ScheduledExecutorService pool);

	/**
	 * Sets the {@link ScheduledExecutorService ScheduledExecutorService} used by
	 * the audio WebSocket connection. Used for sending keepalives and closing the
	 * connection. <br>
	 * <b>Only change this pool if you know what you're doing.</b>
	 *
	 * <p>
	 * Default: {@link ScheduledThreadPoolExecutor} with 1 thread
	 *
	 * @param pool              The thread-pool to use for the audio WebSocket
	 * @param automaticShutdown True, if the executor should be shutdown when JDA
	 *                          shuts down
	 *
	 * @return The SpringJDAContext instance. Useful for chaining.
	 *
	 * @since 4.2.1
	 */
	SpringJDAContext setAudioPool(@Nullable ScheduledExecutorService pool, boolean automaticShutdown);

	/**
	 * If enabled, JDA will separate the bulk delete event into individual delete
	 * events, but this isn't as efficient as handling a single event would be. It
	 * is recommended that BulkDelete Splitting be disabled and that the developer
	 * should instead handle the
	 * {@link net.dv8tion.jda.api.events.message.MessageBulkDeleteEvent
	 * MessageBulkDeleteEvent}.
	 *
	 * <p>
	 * Default: <b>true (enabled)</b>
	 *
	 * @param enabled True - The MESSAGE_DELETE_BULK will be split into multiple
	 *                individual MessageDeleteEvents.
	 *
	 * @return The SpringJDAContext instance. Useful for chaining.
	 */
	SpringJDAContext setBulkDeleteSplittingEnabled(boolean enabled);

	/**
	 * Enables/Disables the use of a Shutdown hook to clean up the ShardManager and
	 * it's JDA instances. <br>
	 * When the Java program closes shutdown hooks are run. This is used as a
	 * last-second cleanup attempt by JDA to properly close connections.
	 *
	 * <p>
	 * Default: <b>true (enabled)</b>
	 *
	 * @param enable True (default) - use shutdown hook to clean up the ShardManager
	 *               and it's JDA instances if the Java program is closed.
	 *
	 * @return The SpringJDAContext instance. Useful for chaining.
	 */
	SpringJDAContext setEnableShutdownHook(boolean enable);

	/**
	 * Sets whether or not JDA should try to reconnect if a connection-error is
	 * encountered. <br>
	 * This will use an incremental reconnect (timeouts are increased each time an
	 * attempt fails).
	 *
	 * <p>
	 * Default: <b>true (enabled)</b>
	 *
	 * @param autoReconnect If true - enables autoReconnect
	 *
	 * @return The SpringJDAContext instance. Useful for chaining.
	 */
	SpringJDAContext setAutoReconnect(boolean autoReconnect);

	/**
	 * Changes the factory used to create
	 * {@link net.dv8tion.jda.api.audio.factory.IAudioSendSystem IAudioSendSystem}
	 * objects which handle the sending loop for audio packets. <br>
	 * By default, JDA uses
	 * {@link net.dv8tion.jda.api.audio.factory.DefaultSendFactory
	 * DefaultSendFactory}.
	 *
	 * @param factory The new
	 *                {@link net.dv8tion.jda.api.audio.factory.IAudioSendFactory
	 *                IAudioSendFactory} to be used when creating new
	 *                {@link net.dv8tion.jda.api.audio.factory.IAudioSendSystem}
	 *                objects.
	 *
	 * @return The SpringJDAContext instance. Useful for chaining.
	 */
	SpringJDAContext setAudioSendFactory(@Nullable IAudioSendFactory factory);

	/**
	 * Sets whether or not we should mark our sessions as afk <br>
	 * This value can be changed at any time using
	 * {@link DefaultShardManager#setIdle(boolean)
	 * DefaultShardManager#setIdleProvider(boolean)}.
	 *
	 * @param idle boolean value that will be provided with our IDENTIFY packages to
	 *             mark our sessions as afk or not. <b>(default false)</b>
	 *
	 * @return The SpringJDAContext instance. Useful for chaining.
	 *
	 * @see net.dv8tion.jda.api.managers.Presence#setIdle(boolean)
	 */
	SpringJDAContext setIdle(boolean idle);

	/**
	 * Sets the {@link net.dv8tion.jda.api.entities.Activity Activity} for our
	 * session. <br>
	 * This value can be changed at any time in the
	 * {@link net.dv8tion.jda.api.managers.Presence Presence} from a JDA instance.
	 *
	 * <p>
	 * <b>Hint:</b> You can create an {@link net.dv8tion.jda.api.entities.Activity
	 * Activity} object using
	 * {@link net.dv8tion.jda.api.entities.Activity#playing(String)
	 * Activity.playing(String)} or
	 * {@link net.dv8tion.jda.api.entities.Activity#streaming(String, String)}
	 * Activity.streaming(String, String)}.
	 *
	 * @param activity An instance of {@link net.dv8tion.jda.api.entities.Activity
	 *                 Activity} (null allowed)
	 *
	 * @return The SpringJDAContext instance. Useful for chaining.
	 *
	 * @see net.dv8tion.jda.api.managers.Presence#setActivity(net.dv8tion.jda.api.entities.Activity)
	 */
	SpringJDAContext setActivity(@Nullable Activity activity);

	/**
	 * Sets the {@link net.dv8tion.jda.api.OnlineStatus OnlineStatus} our connection
	 * will display. <br>
	 * This value can be changed at any time in the
	 * {@link net.dv8tion.jda.api.managers.Presence Presence} from a JDA instance.
	 *
	 * @param status Not-null OnlineStatus (default online)
	 *
	 * @throws IllegalArgumentException if the provided OnlineStatus is null or
	 *                                  {@link net.dv8tion.jda.api.OnlineStatus#UNKNOWN
	 *                                  UNKNOWN}
	 *
	 * @return The SpringJDAContext instance. Useful for chaining.
	 *
	 * @see net.dv8tion.jda.api.managers.Presence#setStatus(OnlineStatus)
	 *      Presence.setStatusProvider(OnlineStatus)
	 */
	SpringJDAContext setStatus(@NonNull OnlineStatus status);

	/**
	 * Adds all provided listeners to the list of listeners that will be used to
	 * populate the {@link DefaultShardManager DefaultShardManager} object. <br>
	 * This uses the {@link net.dv8tion.jda.api.hooks.InterfacedEventManager
	 * InterfacedEventListener} by default. <br>
	 * To switch to the {@link net.dv8tion.jda.api.hooks.AnnotatedEventManager
	 * AnnotatedEventManager}, use {@link #setEventManagerProvider(IntFunction)
	 * setEventManagerProvider(id -> new AnnotatedEventManager())}.
	 *
	 * <p>
	 * <b>Note:</b> When using the
	 * {@link net.dv8tion.jda.api.hooks.InterfacedEventManager
	 * InterfacedEventListener} (default), given listener(s) <b>must</b> be instance
	 * of {@link net.dv8tion.jda.api.hooks.EventListener EventListener}!
	 *
	 * @param listeners The listener(s) to add to the list.
	 *
	 * @return The SpringJDAContext instance. Useful for chaining.
	 *
	 * @see DefaultShardManager#addEventListener(Object...)
	 *      JDA.addEventListeners(Object...)
	 */
	SpringJDAContext addEventListeners(@NonNull Object... listeners);

	/**
	 * Adds all provided listeners to the list of listeners that will be used to
	 * populate the {@link DefaultShardManager DefaultShardManager} object. <br>
	 * This uses the {@link net.dv8tion.jda.api.hooks.InterfacedEventManager
	 * InterfacedEventListener} by default. <br>
	 * To switch to the {@link net.dv8tion.jda.api.hooks.AnnotatedEventManager
	 * AnnotatedEventManager}, use {@link #setEventManagerProvider(IntFunction)
	 * setEventManager(id -> new AnnotatedEventManager())}.
	 *
	 * <p>
	 * <b>Note:</b> When using the
	 * {@link net.dv8tion.jda.api.hooks.InterfacedEventManager
	 * InterfacedEventListener} (default), given listener(s) <b>must</b> be instance
	 * of {@link net.dv8tion.jda.api.hooks.EventListener EventListener}!
	 *
	 * @param listeners The listener(s) to add to the list.
	 *
	 * @return The SpringJDAContext instance. Useful for chaining.
	 *
	 * @see DefaultShardManager#addEventListener(Object...)
	 *      JDA.addEventListeners(Object...)
	 */
	default SpringJDAContext addEventListeners(@NonNull final Collection<Object> listeners) {
		addEventListeners(listeners.toArray());
		return this;
	}

	/**
	 * Removes all provided listeners from the list of listeners.
	 *
	 * @param listeners The listener(s) to remove from the list.
	 *
	 * @return The SpringJDAContext instance. Useful for chaining.
	 *
	 * @see net.dv8tion.jda.api.JDA#removeEventListener(Object...)
	 *      JDA.removeEventListeners(Object...)
	 */
	SpringJDAContext removeEventListeners(@NonNull Object... listeners);

	/**
	 * Removes all provided listeners from the list of listeners.
	 *
	 * @param listeners The listener(s) to remove from the list.
	 *
	 * @return The SpringJDAContext instance. Useful for chaining.
	 *
	 * @see net.dv8tion.jda.api.JDA#removeEventListener(Object...)
	 *      JDA.removeEventListeners(Object...)
	 */
	default SpringJDAContext removeEventListeners(@NonNull final Collection<Object> listeners) {
		removeEventListeners(listeners.toArray());
		return this;
	}

	/**
	 * Sets the maximum amount of time that JDA will back off to wait when
	 * attempting to reconnect the MainWebsocket. <br>
	 * Provided value must be 32 or greater.
	 *
	 * <p>
	 * Default: {@code 900}
	 *
	 * @param maxReconnectDelay The maximum amount of time that JDA will wait
	 *                          between reconnect attempts in seconds.
	 *
	 * @throws java.lang.IllegalArgumentException Thrown if the provided
	 *                                            {@code maxReconnectDelay} is less
	 *                                            than 32.
	 *
	 * @return The SpringJDAContext instance. Useful for chaining.
	 */
	SpringJDAContext setMaxReconnectDelay(int maxReconnectDelay);

	/**
	 * Configures a custom voice dispatch handler which handles audio connections.
	 *
	 * @param interceptor The new voice dispatch handler, or null to use the default
	 *
	 * @return The SpringJDAContext instance. Useful for chaining.
	 *
	 * @since 4.0.0
	 *
	 * @see VoiceDispatchInterceptor
	 */
	SpringJDAContext setVoiceDispatchInterceptor(@Nullable VoiceDispatchInterceptor interceptor);

	/**
	 * The {@link ChunkingFilter} to filter which guilds should use member chunking.
	 *
	 * <p>
	 * Use {@link #setMemberCachePolicy(MemberCachePolicy)} to configure which
	 * members to keep in cache from chunking.
	 *
	 * @param filter The filter to apply
	 *
	 * @return The SpringJDAContext instance. Useful for chaining.
	 *
	 * @since 4.0.0
	 *
	 * @see ChunkingFilter#NONE
	 * @see ChunkingFilter#include(long...)
	 * @see ChunkingFilter#exclude(long...)
	 */
	SpringJDAContext setChunkingFilter(@Nullable ChunkingFilter filter);

	/**
	 * Configures which events will be disabled. Bots which did not enable
	 * presence/member updates in the developer dashboard are required to disable
	 * {@link GatewayIntent#GUILD_PRESENCES} and
	 * {@link GatewayIntent#GUILD_MEMBERS}!
	 *
	 * <p>
	 * It is not recommended to disable {@link GatewayIntent#GUILD_MEMBERS
	 * GatewayIntent.GUILD_MEMBERS} when using {@link MemberCachePolicy#ALL
	 * MemberCachePolicy.ALL} as the members cannot be removed from cache by a leave
	 * event without this intent.
	 *
	 * <p>
	 * If you disable certain intents you also have to disable related
	 * {@link CacheFlag CacheFlags}. This can be achieved using
	 * {@link #disableCache(CacheFlag, CacheFlag...)}. The required intents for each
	 * flag are documented in the {@link CacheFlag} enum.
	 *
	 * @param intent  The first intent to disable
	 * @param intents Any other intents to disable
	 *
	 * @throws IllegalArgumentException If null is provided
	 *
	 * @return The SpringJDAContext instance. Useful for chaining.
	 *
	 * @see #setMemberCachePolicy(MemberCachePolicy)
	 *
	 * @since 4.2.0
	 */
	SpringJDAContext setDisabledIntents(@NonNull GatewayIntent intent, @NonNull GatewayIntent... intents);

	/**
	 * Configures which events will be disabled. Bots which did not enable
	 * presence/member updates in the developer dashboard are required to disable
	 * {@link GatewayIntent#GUILD_PRESENCES} and
	 * {@link GatewayIntent#GUILD_MEMBERS}!
	 *
	 * <p>
	 * It is not recommended to disable {@link GatewayIntent#GUILD_MEMBERS
	 * GatewayIntent.GUILD_MEMBERS} when using {@link MemberCachePolicy#ALL
	 * MemberCachePolicy.ALL} as the members cannot be removed from cache by a leave
	 * event without this intent.
	 *
	 * <p>
	 * If you disable certain intents you also have to disable related
	 * {@link CacheFlag CacheFlags}. This can be achieved using
	 * {@link #disableCache(CacheFlag, CacheFlag...)}. The required intents for each
	 * flag are documented in the {@link CacheFlag} enum.
	 *
	 * @param intents The intents to disable, or null to disable all intents
	 *                (default: none)
	 *
	 * @return The SpringJDAContext instance. Useful for chaining.
	 *
	 * @see #setMemberCachePolicy(MemberCachePolicy)
	 *
	 * @since 4.2.0
	 */
	SpringJDAContext setDisabledIntents(@Nullable Collection<GatewayIntent> intents);

	/**
	 * Disable the specified {@link GatewayIntent GatewayIntents}. <br>
	 * This will not enable any currently unset intents.
	 *
	 * <p>
	 * If you disable certain intents you also have to disable related
	 * {@link CacheFlag CacheFlags}. This can be achieved using
	 * {@link #disableCache(CacheFlag, CacheFlag...)}. The required intents for each
	 * flag are documented in the {@link CacheFlag} enum.
	 *
	 * @param intents The intents to disable
	 *
	 * @throws IllegalArgumentException If provided with null
	 *
	 * @return The SpringJDAContext instance. Useful for chaining.
	 *
	 * @see #enableIntents(Collection)
	 */
	SpringJDAContext disableIntents(@NonNull Collection<GatewayIntent> intents);

	/**
	 * Disable the specified {@link GatewayIntent GatewayIntents}. <br>
	 * This will not enable any currently unset intents.
	 *
	 * <p>
	 * If you disable certain intents you also have to disable related
	 * {@link CacheFlag CacheFlags}. This can be achieved using
	 * {@link #disableCache(CacheFlag, CacheFlag...)}. The required intents for each
	 * flag are documented in the {@link CacheFlag} enum.
	 *
	 * @param intent  The intent to disable
	 * @param intents Other intents to disable
	 *
	 * @throws IllegalArgumentException If provided with null
	 *
	 * @return The SpringJDAContext instance. Useful for chaining.
	 *
	 * @see #enableIntents(GatewayIntent, GatewayIntent...)
	 */
	SpringJDAContext disableIntents(@NonNull GatewayIntent intent, @NonNull GatewayIntent... intents);

	/**
	 * Disable the specified {@link GatewayIntent GatewayIntents}. <br>
	 * This will not enable any currently unset intents.
	 *
	 * <p>
	 * If you disable certain intents you also have to disable related
	 * {@link CacheFlag CacheFlags}. This can be achieved using
	 * {@link #disableCache(CacheFlag, CacheFlag...)}. The required intents for each
	 * flag are documented in the {@link CacheFlag} enum.
	 *
	 * @param intent  The intent to disable
	 * @param intents Other intents to disable
	 *
	 * @throws IllegalArgumentException If provided with null
	 *
	 * @return The SpringJDAContext instance. Useful for chaining.
	 *
	 * @see #enableIntents(GatewayIntent, GatewayIntent...)
	 */
	SpringJDAContext setEnabledIntents(@NonNull GatewayIntent intent, @NonNull GatewayIntent... intents);

	/**
	 * Configures which events will be enabled. Bots which did not enable
	 * presence/member updates in the developer dashboard are required to disable
	 * {@link GatewayIntent#GUILD_PRESENCES} and
	 * {@link GatewayIntent#GUILD_MEMBERS}!
	 *
	 * <p>
	 * It is not recommended to disable {@link GatewayIntent#GUILD_MEMBERS
	 * GatewayIntent.GUILD_MEMBERS} when using {@link MemberCachePolicy#ALL
	 * MemberCachePolicy.ALL} as the members cannot be removed from cache by a leave
	 * event without this intent.
	 *
	 * <p>
	 * If you disable certain intents you also have to disable related
	 * {@link CacheFlag CacheFlags}. This can be achieved using
	 * {@link #disableCache(CacheFlag, CacheFlag...)}. The required intents for each
	 * flag are documented in the {@link CacheFlag} enum.
	 *
	 * @param intents The intents to enable, or null to enable no intents (default:
	 *                all)
	 *
	 * @return The SpringJDAContext instance. Useful for chaining.
	 *
	 * @see #setMemberCachePolicy(MemberCachePolicy)
	 *
	 * @since 4.2.0
	 */
	SpringJDAContext setEnabledIntents(@Nullable Collection<GatewayIntent> intents);

	/**
	 * Enable the specified {@link GatewayIntent GatewayIntents}. <br>
	 * This will not disable any currently set intents.
	 *
	 * @param intents The intents to enable
	 *
	 * @throws IllegalArgumentException If provided with null
	 *
	 * @return The SpringJDAContext instance. Useful for chaining.
	 *
	 * @see #disableIntents(Collection)
	 */
	SpringJDAContext enableIntents(@NonNull Collection<GatewayIntent> intents);

	/**
	 * Enable the specified {@link GatewayIntent GatewayIntents}. <br>
	 * This will not disable any currently set intents.
	 *
	 * @param intent  The intent to enable
	 * @param intents Other intents to enable
	 *
	 * @throws IllegalArgumentException If provided with null
	 *
	 * @return The SpringJDAContext instance. Useful for chaining.
	 *
	 * @see #enableIntents(GatewayIntent, GatewayIntent...)
	 */
	SpringJDAContext enableIntents(@NonNull GatewayIntent intent, @NonNull GatewayIntent... intents);

	/**
	 * Decides the total number of members at which a guild should start to use lazy
	 * loading. <br>
	 * This is limited to a number between 50 and 250 (inclusive). If the
	 * {@link #setChunkingFilter(ChunkingFilter) chunking filter} is set to
	 * {@link ChunkingFilter#ALL} this should be set to {@code 250} (default) to
	 * minimize the amount of guilds that need to request members.
	 *
	 * @param threshold The threshold in {@code [50, 250]}
	 *
	 * @return The SpringJDAContext instance. Useful for chaining.
	 *
	 * @since 4.0.0
	 */
	SpringJDAContext setLargeThreshold(int threshold);

	/**
	 * The maximum size, in bytes, of the buffer used for decompressing discord
	 * payloads. <br>
	 * If the maximum buffer size is exceeded a new buffer will be allocated
	 * instead. <br>
	 * Setting this to {@link Integer#MAX_VALUE} would imply the buffer will never
	 * be resized unless memory starvation is imminent. <br>
	 * Setting this to {@code 0} would imply the buffer would need to be allocated
	 * again for every payload (not recommended).
	 *
	 * <p>
	 * Default: {@code 2048}
	 *
	 * @param bufferSize The maximum size the buffer should allow to retain
	 *
	 * @throws IllegalArgumentException If the provided buffer size is negative
	 *
	 * @return The SpringJDAContext instance. Useful for chaining.
	 */
	SpringJDAContext setMaxBufferSize(int bufferSize);
}
