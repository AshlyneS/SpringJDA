package net.foxgenesis.springJDA.context;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.IntFunction;
import java.util.stream.Collectors;

import org.springframework.lang.NonNull;

import com.neovisionaries.ws.client.WebSocketFactory;

import net.dv8tion.jda.api.GatewayEncoding;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.audio.factory.IAudioSendFactory;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.hooks.IEventManager;
import net.dv8tion.jda.api.hooks.VoiceDispatchInterceptor;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.RestConfig;
import net.dv8tion.jda.api.sharding.DefaultShardManager;
import net.dv8tion.jda.api.sharding.ThreadPoolProvider;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.Compression;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.SessionController;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.dv8tion.jda.internal.utils.Checks;
import net.dv8tion.jda.internal.utils.concurrent.CountingThreadFactory;
import net.dv8tion.jda.internal.utils.config.flags.ConfigFlag;
import net.dv8tion.jda.internal.utils.config.flags.ShardingConfigFlag;
import okhttp3.OkHttpClient;

public class SpringJDAContextBuilder {

	protected final List<Object> listeners = new ArrayList<>();
	protected final List<IntFunction<Object>> listenerProviders = new ArrayList<>();
	protected final EnumSet<CacheFlag> automaticallyDisabled = EnumSet.noneOf(CacheFlag.class);
	protected SessionController sessionController = null;
	protected VoiceDispatchInterceptor voiceDispatchInterceptor = null;
	protected EnumSet<CacheFlag> cacheFlags = EnumSet.allOf(CacheFlag.class);
	protected EnumSet<ConfigFlag> flags = ConfigFlag.getDefault();
	protected EnumSet<ShardingConfigFlag> shardingFlags = ShardingConfigFlag.getDefault();
	protected Compression compression = Compression.ZLIB;
	protected GatewayEncoding encoding = GatewayEncoding.JSON;
	protected int shardsTotal = -1;
	protected int maxReconnectDelay = 900;
	protected int largeThreshold = 250;
	protected int maxBufferSize = 2048;
	protected int intents = -1;
	protected IntFunction<Boolean> idleProvider = null;
	protected IntFunction<OnlineStatus> statusProvider = null;
	protected IntFunction<? extends Activity> activityProvider = null;
	protected IntFunction<? extends ConcurrentMap<String, String>> contextProvider = null;
	protected IntFunction<? extends IEventManager> eventManagerProvider = null;
	protected ThreadPoolProvider<? extends ScheduledExecutorService> rateLimitSchedulerProvider = ThreadPoolProvider
			.lazy((total) -> Executors.newScheduledThreadPool(Math.max(2, 2 * (int) Math.log(total)),
					new CountingThreadFactory(() -> "JDA", "RateLimit-Scheduler", true)));
	protected ThreadPoolProvider<? extends ExecutorService> rateLimitElasticProvider = ThreadPoolProvider
			.lazy((total) -> {
				ExecutorService pool = Executors
						.newCachedThreadPool(new CountingThreadFactory(() -> "JDA", "RateLimit-Elastic", true));
				if (pool instanceof ThreadPoolExecutor) {
					((ThreadPoolExecutor) pool).setCorePoolSize(Math.max(1, (int) Math.log(total)));
					((ThreadPoolExecutor) pool).setKeepAliveTime(2, TimeUnit.MINUTES);
				}
				return pool;
			});
	protected ThreadPoolProvider<? extends ScheduledExecutorService> gatewayPoolProvider = ThreadPoolProvider
			.lazy((total) -> Executors.newScheduledThreadPool(Math.max(1, (int) Math.log(total)),
					new CountingThreadFactory(() -> "JDA", "Gateway")));
	protected ThreadPoolProvider<? extends ExecutorService> callbackPoolProvider = null;
	protected ThreadPoolProvider<? extends ExecutorService> eventPoolProvider = null;
	protected ThreadPoolProvider<? extends ScheduledExecutorService> audioPoolProvider = null;
	protected IntFunction<? extends RestConfig> restConfigProvider = null;
	protected Collection<Integer> shards = null;
	protected OkHttpClient.Builder httpClientBuilder = null;
	protected OkHttpClient httpClient = null;
	protected WebSocketFactory wsFactory = null;
	protected IAudioSendFactory audioSendFactory = null;
	protected ThreadFactory threadFactory = null;
	protected ChunkingFilter chunkingFilter = ChunkingFilter.ALL;
	protected MemberCachePolicy memberCachePolicy = MemberCachePolicy.ALL;

	/**
	 * Choose which {@link GatewayEncoding} JDA should use.
	 *
	 * @param encoding The {@link GatewayEncoding} (default: JSON)
	 *
	 * @throws IllegalArgumentException If null is provided
	 *
	 * @return The SpringJDAContextBuilder instance. Useful for chaining.
	 *
	 * @since 4.2.1
	 */

	public SpringJDAContextBuilder setGatewayEncoding(GatewayEncoding encoding) {
		Checks.notNull(encoding, "GatewayEncoding");
		this.encoding = encoding;
		return this;
	}

	/**
	 * Whether JDA should fire {@link net.dv8tion.jda.api.events.RawGatewayEvent}
	 * for every discord event. <br>
	 * Default: {@code false}
	 *
	 * @param enable True, if JDA should fire
	 *               {@link net.dv8tion.jda.api.events.RawGatewayEvent}.
	 *
	 * @return The SpringJDAContextBuilder instance. Useful for chaining.
	 *
	 * @since 4.0.0
	 */

	public SpringJDAContextBuilder setRawEventsEnabled(boolean enable) {
		return setFlag(ConfigFlag.RAW_EVENTS, enable);
	}

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
	 * @return The SpringJDAContextBuilder instance. Useful for chaining.
	 *
	 * @see Event#getRawData()
	 */

	public SpringJDAContextBuilder setEventPassthrough(boolean enable) {
		return setFlag(ConfigFlag.EVENT_PASSTHROUGH, enable);
	}

	/**
	 * Custom {@link RestConfig} to use. <br>
	 * This can be used to customize how rate-limits are handled and configure a
	 * custom http proxy.
	 *
	 * @param provider The {@link RestConfig} provider to use
	 *
	 * @throws IllegalArgumentException If null is provided
	 *
	 * @return The SpringJDAContextBuilder instance. Useful for chaining.
	 */

	public SpringJDAContextBuilder setRestConfigProvider(IntFunction<? extends RestConfig> provider) {
		Checks.notNull(provider, "RestConfig Provider");
		this.restConfigProvider = provider;
		return this;
	}

	/**
	 * Custom {@link RestConfig} to use. <br>
	 * This can be used to customize how rate-limits are handled and configure a
	 * custom http proxy.
	 *
	 * @param config The {@link RestConfig} to use
	 *
	 * @throws IllegalArgumentException If null is provided
	 *
	 * @return The SpringJDAContextBuilder instance. Useful for chaining.
	 */

	public SpringJDAContextBuilder setRestConfig(RestConfig config) {
		Checks.notNull(config, "RestConfig");
		return setRestConfigProvider(ignored -> config);
	}

	/**
	 * Enable specific cache flags. <br>
	 * This will not disable any currently set cache flags.
	 *
	 * @param flags The {@link CacheFlag CacheFlags} to enable
	 *
	 * @throws IllegalArgumentException If provided with null
	 *
	 * @return The SpringJDAContextBuilder instance. Useful for chaining.
	 *
	 * @see #enableCache(CacheFlag, CacheFlag...)
	 * @see #disableCache(Collection)
	 */

	public SpringJDAContextBuilder enableCache(Collection<CacheFlag> flags) {
		Checks.noneNull(flags, "CacheFlags");
		cacheFlags.addAll(flags);
		return this;
	}

	/**
	 * Enable specific cache flags. <br>
	 * This will not disable any currently set cache flags.
	 *
	 * @param flag  {@link CacheFlag} to enable
	 * @param flags Other flags to enable
	 *
	 * @throws IllegalArgumentException If provided with null
	 *
	 * @return The SpringJDAContextBuilder instance. Useful for chaining.
	 *
	 * @see #enableCache(Collection)
	 * @see #disableCache(CacheFlag, CacheFlag...)
	 */

	public SpringJDAContextBuilder enableCache(CacheFlag flag, CacheFlag... flags) {
		Checks.notNull(flag, "CacheFlag");
		Checks.noneNull(flags, "CacheFlag");
		cacheFlags.addAll(EnumSet.of(flag, flags));
		return this;
	}

	/**
	 * Disable specific cache flags. <br>
	 * This will not enable any currently unset cache flags.
	 *
	 * @param flags The {@link CacheFlag CacheFlags} to disable
	 *
	 * @throws IllegalArgumentException If provided with null
	 *
	 * @return The SpringJDAContextBuilder instance. Useful for chaining.
	 *
	 * @see #disableCache(CacheFlag, CacheFlag...)
	 * @see #enableCache(Collection)
	 */

	public SpringJDAContextBuilder disableCache(Collection<CacheFlag> flags) {
		Checks.noneNull(flags, "CacheFlags");
		automaticallyDisabled.removeAll(flags);
		cacheFlags.removeAll(flags);
		return this;
	}

	/**
	 * Disable specific cache flags. <br>
	 * This will not enable any currently unset cache flags.
	 *
	 * @param flag  {@link CacheFlag} to disable
	 * @param flags Other flags to disable
	 *
	 * @throws IllegalArgumentException If provided with null
	 *
	 * @return The SpringJDAContextBuilder instance. Useful for chaining.
	 *
	 * @see #disableCache(Collection)
	 * @see #enableCache(CacheFlag, CacheFlag...)
	 */

	public SpringJDAContextBuilder disableCache(CacheFlag flag, CacheFlag... flags) {
		Checks.notNull(flag, "CacheFlag");
		Checks.noneNull(flags, "CacheFlag");
		return disableCache(EnumSet.of(flag, flags));
	}

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
	 * public void configureCache(SpringJDAContextBuilder builder) {
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
	 * @return The SpringJDAContextBuilder instance. Useful for chaining.
	 *
	 * @see MemberCachePolicy
	 * @see #setEnabledIntents(Collection)
	 *
	 * @since 4.2.0
	 */

	public SpringJDAContextBuilder setMemberCachePolicy(MemberCachePolicy policy) {
		if (policy == null)
			this.memberCachePolicy = MemberCachePolicy.ALL;
		else
			this.memberCachePolicy = policy;
		return this;
	}

	/**
	 * Sets the {@link net.dv8tion.jda.api.utils.SessionController
	 * SessionController} for the resulting ShardManager instance. This can be used
	 * to sync behaviour and state between shards of a bot and should be one and the
	 * same instance on all builders for the shards.
	 *
	 * @param controller The {@link net.dv8tion.jda.api.utils.SessionController
	 *                   SessionController} to use
	 *
	 * @return The SpringJDAContextBuilder instance. Useful for chaining.
	 *
	 * @see net.dv8tion.jda.api.utils.SessionControllerAdapter
	 *      SessionControllerAdapter
	 */

	public SpringJDAContextBuilder setSessionController(SessionController controller) {
		this.sessionController = controller;
		return this;
	}

	/**
	 * Configures a custom voice dispatch handler which handles audio connections.
	 *
	 * @param interceptor The new voice dispatch handler, or null to use the default
	 *
	 * @return The SpringJDAContextBuilder instance. Useful for chaining.
	 *
	 * @since 4.0.0
	 *
	 * @see VoiceDispatchInterceptor
	 */

	public SpringJDAContextBuilder setVoiceDispatchInterceptor(VoiceDispatchInterceptor interceptor) {
		this.voiceDispatchInterceptor = interceptor;
		return this;
	}

	/**
	 * Sets the {@link org.slf4j.MDC MDC} mappings provider to use in JDA. <br>
	 * If sharding is enabled JDA will automatically add a {@code jda.shard} context
	 * with the format {@code [SHARD_ID / TOTAL]} where {@code SHARD_ID} and
	 * {@code TOTAL} are the shard configuration. Additionally it will provide
	 * context for the id via {@code jda.shard.id} and the total via
	 * {@code jda.shard.total}.
	 *
	 * <p>
	 * <b>The manager will call this with a shardId and it is recommended to provide
	 * a different context map for each shard!</b> <br>
	 * This automatically switches {@link #setContextEnabled(boolean)} to true if
	 * the provided function is not null!
	 *
	 * @param provider The provider for <b>modifiable</b> context maps to use in
	 *                 JDA, or {@code null} to reset
	 *
	 * @return The SpringJDAContextBuilder instance. Useful for chaining.
	 *
	 * @see <a href="https://www.slf4j.org/api/org/slf4j/MDC.html" target=
	 *      "_blank">MDC Javadoc</a>
	 */

	public SpringJDAContextBuilder setContextMap(IntFunction<? extends ConcurrentMap<String, String>> provider) {
		this.contextProvider = provider;
		if (provider != null)
			setContextEnabled(true);
		return this;
	}

	/**
	 * Whether JDA should use a synchronized MDC context for all of its controlled
	 * threads. <br>
	 * Default: {@code true}
	 *
	 * @param enable True, if JDA should provide an MDC context map
	 *
	 * @return The SpringJDAContextBuilder instance. Useful for chaining.
	 *
	 * @see <a href="https://www.slf4j.org/api/org/slf4j/MDC.html" target=
	 *      "_blank">MDC Javadoc</a>
	 * @see #setContextMap(java.util.function.IntFunction)
	 */

	public SpringJDAContextBuilder setContextEnabled(boolean enable) {
		return setFlag(ConfigFlag.MDC_CONTEXT, enable);
	}

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
	 * @return The SpringJDAContextBuilder instance. Useful for chaining.
	 *
	 * @see <a href=
	 *      "https://discord.com/developers/docs/topics/gateway#transport-compression"
	 *      target="_blank">Official Discord Documentation - Transport
	 *      Compression</a>
	 */

	public SpringJDAContextBuilder setCompression(Compression compression) {
		Checks.notNull(compression, "Compression");
		this.compression = compression;
		return this;
	}

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
	 * @return The SpringJDAContextBuilder instance. Useful for chaining.
	 *
	 * @see DefaultShardManager#addEventListener(Object...)
	 *      JDA.addEventListeners(Object...)
	 */

	public SpringJDAContextBuilder addEventListeners(final Object... listeners) {
		return this.addEventListeners(Arrays.asList(listeners));
	}

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
	 * @return The SpringJDAContextBuilder instance. Useful for chaining.
	 *
	 * @see DefaultShardManager#addEventListener(Object...)
	 *      JDA.addEventListeners(Object...)
	 */

	public SpringJDAContextBuilder addEventListeners(final Collection<Object> listeners) {
		Checks.noneNull(listeners, "listeners");

		this.listeners.addAll(listeners);
		return this;
	}

	/**
	 * Removes all provided listeners from the list of listeners.
	 *
	 * @param listeners The listener(s) to remove from the list.
	 *
	 * @return The SpringJDAContextBuilder instance. Useful for chaining.
	 *
	 * @see net.dv8tion.jda.api.JDA#removeEventListener(Object...)
	 *      JDA.removeEventListeners(Object...)
	 */

	public SpringJDAContextBuilder removeEventListeners(final Object... listeners) {
		return this.removeEventListeners(Arrays.asList(listeners));
	}

	/**
	 * Removes all provided listeners from the list of listeners.
	 *
	 * @param listeners The listener(s) to remove from the list.
	 *
	 * @return The SpringJDAContextBuilder instance. Useful for chaining.
	 *
	 * @see net.dv8tion.jda.api.JDA#removeEventListener(Object...)
	 *      JDA.removeEventListeners(Object...)
	 */

	public SpringJDAContextBuilder removeEventListeners(final Collection<Object> listeners) {
		Checks.noneNull(listeners, "listeners");

		this.listeners.removeAll(listeners);
		return this;
	}

	/**
	 * Adds the provided listener provider to the list of listener providers that
	 * will be used to create listeners. On shard creation (including shard
	 * restarts) the provider will have the shard id applied and must return a
	 * listener, which will be used, along all other listeners, to populate the
	 * listeners of the JDA object of that shard.
	 *
	 * <br>
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
	 * @param listenerProvider The listener provider to add to the list of listener
	 *                         providers.
	 *
	 * @return The SpringJDAContextBuilder instance. Useful for chaining.
	 */

	public SpringJDAContextBuilder addEventListenerProvider(final IntFunction<Object> listenerProvider) {
		return this.addEventListenerProviders(Collections.singleton(listenerProvider));
	}

	/**
	 * Adds the provided listener providers to the list of listener providers that
	 * will be used to create listeners. On shard creation (including shard
	 * restarts) each provider will have the shard id applied and must return a
	 * listener, which will be used, along all other listeners, to populate the
	 * listeners of the JDA object of that shard.
	 *
	 * <br>
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
	 * @param listenerProviders The listener provider to add to the list of listener
	 *                          providers.
	 *
	 * @return The SpringJDAContextBuilder instance. Useful for chaining.
	 */

	public SpringJDAContextBuilder addEventListenerProviders(final Collection<IntFunction<Object>> listenerProviders) {
		Checks.noneNull(listenerProviders, "listener providers");

		this.listenerProviders.addAll(listenerProviders);
		return this;
	}

	/**
	 * Removes the provided listener provider from the list of listener providers.
	 *
	 * @param listenerProvider The listener provider to remove from the list of
	 *                         listener providers.
	 *
	 * @return The SpringJDAContextBuilder instance. Useful for chaining.
	 */

	public SpringJDAContextBuilder removeEventListenerProvider(final IntFunction<Object> listenerProvider) {
		return this.removeEventListenerProviders(Collections.singleton(listenerProvider));
	}

	/**
	 * Removes all provided listener providers from the list of listener providers.
	 *
	 * @param listenerProviders The listener provider(s) to remove from the list of
	 *                          listener providers.
	 *
	 * @return The SpringJDAContextBuilder instance. Useful for chaining.
	 */

	public SpringJDAContextBuilder removeEventListenerProviders(
			final Collection<IntFunction<Object>> listenerProviders) {
		Checks.noneNull(listenerProviders, "listener providers");

		this.listenerProviders.removeAll(listenerProviders);
		return this;
	}

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
	 * @return The SpringJDAContextBuilder instance. Useful for chaining.
	 */

	public SpringJDAContextBuilder setAudioSendFactory(final IAudioSendFactory factory) {
		this.audioSendFactory = factory;
		return this;
	}

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
	 * @return The SpringJDAContextBuilder instance. Useful for chaining.
	 */

	public SpringJDAContextBuilder setAutoReconnect(final boolean autoReconnect) {
		return setFlag(ConfigFlag.AUTO_RECONNECT, autoReconnect);
	}

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
	 * @return The SpringJDAContextBuilder instance. Useful for chaining.
	 */

	public SpringJDAContextBuilder setBulkDeleteSplittingEnabled(final boolean enabled) {
		return setFlag(ConfigFlag.BULK_DELETE_SPLIT, enabled);
	}

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
	 * @return The SpringJDAContextBuilder instance. Useful for chaining.
	 */

	public SpringJDAContextBuilder setEnableShutdownHook(final boolean enable) {
		return setFlag(ConfigFlag.SHUTDOWN_HOOK, enable);
	}

	/**
	 * Sets a provider to change the internally used EventManager. <br>
	 * There are 2 provided Implementations:
	 * <ul>
	 * <li>{@link net.dv8tion.jda.api.hooks.InterfacedEventManager
	 * InterfacedEventManager} which uses the Interface
	 * {@link net.dv8tion.jda.api.hooks.EventListener EventListener} (tip: use the
	 * {@link net.dv8tion.jda.api.hooks.ListenerAdapter ListenerAdapter}). <br>
	 * This is the default EventManager.</li>
	 *
	 * <li>{@link net.dv8tion.jda.api.hooks.AnnotatedEventManager
	 * AnnotatedEventManager} which uses the Annotation
	 * {@link net.dv8tion.jda.api.hooks.SubscribeEvent @SubscribeEvent} to mark the
	 * methods that listen for events.</li>
	 * </ul>
	 * <br>
	 * You can also create your own EventManager (See
	 * {@link net.dv8tion.jda.api.hooks.IEventManager}).
	 *
	 * @param eventManagerProvider A supplier for the new
	 *                             {@link net.dv8tion.jda.api.hooks.IEventManager}
	 *                             to use.
	 *
	 * @return The SpringJDAContextBuilder instance. Useful for chaining.
	 */

	public SpringJDAContextBuilder setEventManagerProvider(
			final IntFunction<? extends IEventManager> eventManagerProvider) {
		Checks.notNull(eventManagerProvider, "eventManagerProvider");
		this.eventManagerProvider = eventManagerProvider;
		return this;
	}

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
	 * @return The SpringJDAContextBuilder instance. Useful for chaining.
	 *
	 * @see net.dv8tion.jda.api.managers.Presence#setActivity(net.dv8tion.jda.api.entities.Activity)
	 */

	public SpringJDAContextBuilder setActivity(final Activity activity) {
		return this.setActivityProvider(id -> activity);
	}

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
	 * {@link net.dv8tion.jda.api.entities.Activity#streaming(String, String)
	 * Activity.streaming(String, String)}.
	 *
	 * @param activityProvider An instance of
	 *                         {@link net.dv8tion.jda.api.entities.Activity
	 *                         Activity} (null allowed)
	 *
	 * @return The SpringJDAContextBuilder instance. Useful for chaining.
	 *
	 * @see net.dv8tion.jda.api.managers.Presence#setActivity(net.dv8tion.jda.api.entities.Activity)
	 */

	public SpringJDAContextBuilder setActivityProvider(final IntFunction<? extends Activity> activityProvider) {
		this.activityProvider = activityProvider;
		return this;
	}

	/**
	 * Sets whether or not we should mark our sessions as afk <br>
	 * This value can be changed at any time using
	 * {@link DefaultShardManager#setIdle(boolean)
	 * DefaultShardManager#setIdleProvider(boolean)}.
	 *
	 * @param idle boolean value that will be provided with our IDENTIFY packages to
	 *             mark our sessions as afk or not. <b>(default false)</b>
	 *
	 * @return The SpringJDAContextBuilder instance. Useful for chaining.
	 *
	 * @see net.dv8tion.jda.api.managers.Presence#setIdle(boolean)
	 */

	public SpringJDAContextBuilder setIdle(final boolean idle) {
		return this.setIdleProvider(id -> idle);
	}

	/**
	 * Sets whether or not we should mark our sessions as afk <br>
	 * This value can be changed at any time using
	 * {@link DefaultShardManager#setIdle(boolean)
	 * DefaultShardManager#setIdleProvider(boolean)}.
	 *
	 * @param idleProvider boolean value that will be provided with our IDENTIFY
	 *                     packages to mark our sessions as afk or not. <b>(default
	 *                     false)</b>
	 *
	 * @return The SpringJDAContextBuilder instance. Useful for chaining.
	 *
	 * @see net.dv8tion.jda.api.managers.Presence#setIdle(boolean)
	 */

	public SpringJDAContextBuilder setIdleProvider(final IntFunction<Boolean> idleProvider) {
		this.idleProvider = idleProvider;
		return this;
	}

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
	 * @return The SpringJDAContextBuilder instance. Useful for chaining.
	 *
	 * @see net.dv8tion.jda.api.managers.Presence#setStatus(OnlineStatus)
	 *      Presence.setStatusProvider(OnlineStatus)
	 */

	public SpringJDAContextBuilder setStatus(final OnlineStatus status) {
		Checks.notNull(status, "status");
		Checks.check(status != OnlineStatus.UNKNOWN, "OnlineStatus cannot be unknown!");

		return this.setStatusProvider(id -> status);
	}

	/**
	 * Sets the {@link net.dv8tion.jda.api.OnlineStatus OnlineStatus} our connection
	 * will display. <br>
	 * This value can be changed at any time in the
	 * {@link net.dv8tion.jda.api.managers.Presence Presence} from a JDA instance.
	 *
	 * @param statusProvider Not-null OnlineStatus (default online)
	 *
	 * @throws IllegalArgumentException if the provided OnlineStatus is null or
	 *                                  {@link net.dv8tion.jda.api.OnlineStatus#UNKNOWN
	 *                                  UNKNOWN}
	 *
	 * @return The SpringJDAContextBuilder instance. Useful for chaining.
	 *
	 * @see net.dv8tion.jda.api.managers.Presence#setStatus(OnlineStatus)
	 *      Presence.setStatusProvider(OnlineStatus)
	 */

	public SpringJDAContextBuilder setStatusProvider(final IntFunction<OnlineStatus> statusProvider) {
		this.statusProvider = statusProvider;
		return this;
	}

	/**
	 * Sets the {@link java.util.concurrent.ThreadFactory ThreadFactory} that will
	 * be used by the internal executor of the ShardManager.
	 * <p>
	 * Note: This will not affect Threads created by any JDA instance.
	 *
	 * @param threadFactory The ThreadFactory or {@code null} to reset to the
	 *                      default value.
	 *
	 * @return The SpringJDAContextBuilder instance. Useful for chaining.
	 */

	public SpringJDAContextBuilder setThreadFactory(final ThreadFactory threadFactory) {
		this.threadFactory = threadFactory;
		return this;
	}

	/**
	 * Sets the {@link okhttp3.OkHttpClient.Builder Builder} that will be used by
	 * JDA's requester. This can be used to set things such as connection timeout
	 * and proxy.
	 *
	 * @param builder The new {@link okhttp3.OkHttpClient.Builder
	 *                OkHttpClient.Builder} to use.
	 *
	 * @return The SpringJDAContextBuilder instance. Useful for chaining.
	 */

	public SpringJDAContextBuilder setHttpClientBuilder(OkHttpClient.Builder builder) {
		this.httpClientBuilder = builder;
		return this;
	}

	/**
	 * Sets the {@link okhttp3.OkHttpClient OkHttpClient} that will be used by JDAs
	 * requester. <br>
	 * This can be used to set things such as connection timeout and proxy.
	 *
	 * @param client The new {@link okhttp3.OkHttpClient OkHttpClient} to use
	 *
	 * @return The SpringJDAContextBuilder instance. Useful for chaining.
	 */

	public SpringJDAContextBuilder setHttpClient(OkHttpClient client) {
		this.httpClient = client;
		return this;
	}

	/**
	 * Sets the {@link ScheduledExecutorService ScheduledExecutorService} that
	 * should be used in the JDA rate-limit handler. Changing this can drastically
	 * change the JDA behavior for RestAction execution and should be handled
	 * carefully. <b>Only change this pool if you know what you're doing.</b> <br>
	 * This will override the rate-limit pool provider set from
	 * {@link #setRateLimitSchedulerProvider(ThreadPoolProvider)}. <br>
	 * <b>This automatically disables the automatic shutdown of the rate-limit pool,
	 * you can enable it using
	 * {@link #setRateLimitScheduler(ScheduledExecutorService, boolean)
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
	 * @return The SpringJDAContextBuilder instance. Useful for chaining.
	 */

	public SpringJDAContextBuilder setRateLimitScheduler(ScheduledExecutorService pool) {
		return setRateLimitScheduler(pool, pool == null);
	}

	/**
	 * Sets the {@link ScheduledExecutorService ScheduledExecutorService} that
	 * should be used in the JDA rate-limit handler. Changing this can drastically
	 * change the JDA behavior for RestAction execution and should be handled
	 * carefully. <b>Only change this pool if you know what you're doing.</b> <br>
	 * This will override the rate-limit pool provider set from
	 * {@link #setRateLimitSchedulerProvider(ThreadPoolProvider)}.
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
	 * @return The SpringJDAContextBuilder instance. Useful for chaining.
	 */

	public SpringJDAContextBuilder setRateLimitScheduler(ScheduledExecutorService pool, boolean automaticShutdown) {
		return setRateLimitSchedulerProvider(
				pool == null ? null : new ThreadPoolProviderImpl<>(pool, automaticShutdown));
	}

	/**
	 * Sets the {@link ScheduledExecutorService ScheduledExecutorService} provider
	 * that should be used in the JDA rate-limit handler. Changing this can
	 * drastically change the JDA behavior for RestAction execution and should be
	 * handled carefully. <b>Only change this pool if you know what you're
	 * doing.</b>
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
	 * @param provider The thread-pool provider to use for rate-limit handling
	 *
	 * @return The SpringJDAContextBuilder instance. Useful for chaining.
	 */

	public SpringJDAContextBuilder setRateLimitSchedulerProvider(
			ThreadPoolProvider<? extends ScheduledExecutorService> provider) {
		this.rateLimitSchedulerProvider = provider;
		return this;
	}

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
	 * @return The SpringJDAContextBuilder instance. Useful for chaining.
	 */

	public SpringJDAContextBuilder setRateLimitElastic(ExecutorService pool) {
		return setRateLimitElastic(pool, pool == null);
	}

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
	 * @return The SpringJDAContextBuilder instance. Useful for chaining.
	 */

	public SpringJDAContextBuilder setRateLimitElastic(ExecutorService pool, boolean automaticShutdown) {
		return setRateLimitElasticProvider(pool == null ? null : new ThreadPoolProviderImpl<>(pool, automaticShutdown));
	}

	/**
	 * Sets the {@link ExecutorService} that should be used in the JDA request
	 * handler. Changing this can drastically change the JDA behavior for RestAction
	 * execution and should be handled carefully. <b>Only change this pool if you
	 * know what you're doing.</b>
	 *
	 * <p>
	 * This is used mostly by the Rate-Limiter to execute the blocking HTTP requests
	 * at runtime.
	 *
	 * <p>
	 * Default: {@link Executors#newCachedThreadPool()} shared between all shards.
	 *
	 * @param provider The thread-pool provider to use for executing http requests
	 *
	 * @return The SpringJDAContextBuilder instance. Useful for chaining.
	 */

	public SpringJDAContextBuilder setRateLimitElasticProvider(ThreadPoolProvider<? extends ExecutorService> provider) {
		this.rateLimitElasticProvider = provider;
		return this;
	}

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
	 * @return The SpringJDAContextBuilder instance. Useful for chaining.
	 */

	public SpringJDAContextBuilder setGatewayPool(ScheduledExecutorService pool) {
		return setGatewayPool(pool, pool == null);
	}

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
	 * @return The SpringJDAContextBuilder instance. Useful for chaining.
	 */

	public SpringJDAContextBuilder setGatewayPool(ScheduledExecutorService pool, boolean automaticShutdown) {
		return setGatewayPoolProvider(pool == null ? null : new ThreadPoolProviderImpl<>(pool, automaticShutdown));
	}

	/**
	 * Sets the {@link ScheduledExecutorService ScheduledExecutorService} that
	 * should be used for the JDA main WebSocket workers. <br>
	 * <b>Only change this pool if you know what you're doing.</b>
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
	 * @param provider The thread-pool provider to use for main WebSocket workers
	 *
	 * @return The SpringJDAContextBuilder instance. Useful for chaining.
	 */

	public SpringJDAContextBuilder setGatewayPoolProvider(
			ThreadPoolProvider<? extends ScheduledExecutorService> provider) {
		this.gatewayPoolProvider = provider;
		return this;
	}

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
	 * @return The SpringJDAContextBuilder instance. Useful for chaining.
	 */

	public SpringJDAContextBuilder setCallbackPool(ExecutorService executor) {
		return setCallbackPool(executor, executor == null);
	}

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
	 * @return The SpringJDAContextBuilder instance. Useful for chaining.
	 */

	public SpringJDAContextBuilder setCallbackPool(ExecutorService executor, boolean automaticShutdown) {
		return setCallbackPoolProvider(
				executor == null ? null : new ThreadPoolProviderImpl<>(executor, automaticShutdown));
	}

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
	 * @param provider The thread-pool provider to use for callback handling
	 *
	 * @return The SpringJDAContextBuilder instance. Useful for chaining.
	 */

	public SpringJDAContextBuilder setCallbackPoolProvider(ThreadPoolProvider<? extends ExecutorService> provider) {
		this.callbackPoolProvider = provider;
		return this;
	}

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
	 * @return The SpringJDAContextBuilder instance. Useful for chaining.
	 *
	 * @since 4.2.0
	 */

	public SpringJDAContextBuilder setEventPool(ExecutorService executor) {
		return setEventPool(executor, executor == null);
	}

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
	 * @return The SpringJDAContextBuilder instance. Useful for chaining.
	 *
	 * @since 4.2.0
	 */

	public SpringJDAContextBuilder setEventPool(ExecutorService executor, boolean automaticShutdown) {
		return setEventPoolProvider(
				executor == null ? null : new ThreadPoolProviderImpl<>(executor, automaticShutdown));
	}

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
	 * Default: Disabled
	 *
	 * @param provider The thread-pool provider to use for callback handling
	 *
	 * @return The SpringJDAContextBuilder instance. Useful for chaining.
	 *
	 * @since 4.2.0
	 */

	public SpringJDAContextBuilder setEventPoolProvider(ThreadPoolProvider<? extends ExecutorService> provider) {
		this.eventPoolProvider = provider;
		return this;
	}

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
	 * @return The SpringJDAContextBuilder instance. Useful for chaining.
	 *
	 * @since 4.2.1
	 */

	public SpringJDAContextBuilder setAudioPool(ScheduledExecutorService pool) {
		return setAudioPool(pool, pool == null);
	}

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
	 * @return The SpringJDAContextBuilder instance. Useful for chaining.
	 *
	 * @since 4.2.1
	 */

	public SpringJDAContextBuilder setAudioPool(ScheduledExecutorService pool, boolean automaticShutdown) {
		return setAudioPoolProvider(pool == null ? null : new ThreadPoolProviderImpl<>(pool, automaticShutdown));
	}

	/**
	 * Sets the {@link ScheduledExecutorService ScheduledExecutorService} used by
	 * the audio WebSocket connection. Used for sending keepalives and closing the
	 * connection. <br>
	 * <b>Only change this pool if you know what you're doing.</b>
	 *
	 * <p>
	 * Default: {@link ScheduledThreadPoolExecutor} with 1 thread
	 *
	 * @param provider The thread-pool provider to use for the audio WebSocket
	 *
	 * @return The SpringJDAContextBuilder instance. Useful for chaining.
	 *
	 * @since 4.2.1
	 */

	public SpringJDAContextBuilder setAudioPoolProvider(
			ThreadPoolProvider<? extends ScheduledExecutorService> provider) {
		this.audioPoolProvider = provider;
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
	 * @return The SpringJDAContextBuilder instance. Useful for chaining.
	 */

	public SpringJDAContextBuilder setMaxReconnectDelay(final int maxReconnectDelay) {
		Checks.check(maxReconnectDelay >= 32, "Max reconnect delay must be 32 seconds or greater. You provided %d.",
				maxReconnectDelay);

		this.maxReconnectDelay = maxReconnectDelay;
		return this;
	}

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
	 * @return The SpringJDAContextBuilder instance. Useful for chaining.
	 */

	public SpringJDAContextBuilder setRequestTimeoutRetry(boolean retryOnTimeout) {
		return setFlag(ConfigFlag.RETRY_TIMEOUT, retryOnTimeout);
	}

	/**
	 * Sets the list of shards the {@link DefaultShardManager DefaultShardManager}
	 * should contain.
	 *
	 * <p>
	 * <b>This does not have any effect if the total shard count is set to
	 * {@code -1} (get recommended shards from discord).</b>
	 *
	 * @param shardIds The list of shard ids
	 *
	 * @return The SpringJDAContextBuilder instance. Useful for chaining.
	 */

	public SpringJDAContextBuilder setShards(final int... shardIds) {
		Checks.notNull(shardIds, "shardIds");
		for (int id : shardIds) {
			Checks.notNegative(id, "minShardId");
			Checks.check(id < this.shardsTotal, "maxShardId must be lower than shardsTotal");
		}

		this.shards = Arrays.stream(shardIds).boxed().collect(Collectors.toSet());

		return this;
	}

	/**
	 * Sets the range of shards the {@link DefaultShardManager DefaultShardManager}
	 * should contain. This is useful if you want to split your shards between
	 * multiple JVMs or servers.
	 *
	 * <p>
	 * <b>This does not have any effect if the total shard count is set to
	 * {@code -1} (get recommended shards from discord).</b>
	 *
	 * @param minShardId The lowest shard id the DefaultShardManager should contain
	 *
	 * @param maxShardId The highest shard id the DefaultShardManager should contain
	 *
	 * @throws IllegalArgumentException If either minShardId is negative, maxShardId
	 *                                  is lower than shardsTotal or minShardId is
	 *                                  lower than or equal to maxShardId
	 *
	 * @return The SpringJDAContextBuilder instance. Useful for chaining.
	 */

	public SpringJDAContextBuilder setShards(final int minShardId, final int maxShardId) {
		Checks.notNegative(minShardId, "minShardId");
		Checks.check(maxShardId < this.shardsTotal, "maxShardId must be lower than shardsTotal");
		Checks.check(minShardId <= maxShardId, "minShardId must be lower than or equal to maxShardId");

		List<Integer> shards = new ArrayList<>(maxShardId - minShardId + 1);
		for (int i = minShardId; i <= maxShardId; i++)
			shards.add(i);

		this.shards = shards;

		return this;
	}

	/**
	 * Sets the range of shards the {@link DefaultShardManager DefaultShardManager}
	 * should contain. This is useful if you want to split your shards between
	 * multiple JVMs or servers.
	 *
	 * <p>
	 * <b>This does not have any effect if the total shard count is set to
	 * {@code -1} (get recommended shards from discord).</b>
	 *
	 * @param shardIds The list of shard ids
	 *
	 * @throws IllegalArgumentException If either minShardId is negative, maxShardId
	 *                                  is lower than shardsTotal or minShardId is
	 *                                  lower than or equal to maxShardId
	 *
	 * @return The SpringJDAContextBuilder instance. Useful for chaining.
	 */

	public SpringJDAContextBuilder setShards(Collection<Integer> shardIds) {
		Checks.notNull(shardIds, "shardIds");
		for (Integer id : shardIds) {
			Checks.notNegative(id, "minShardId");
			Checks.check(id < this.shardsTotal, "maxShardId must be lower than shardsTotal");
		}

		this.shards = new ArrayList<>(shardIds);

		return this;
	}

	/**
	 * This will set the total amount of shards the {@link DefaultShardManager
	 * DefaultShardManager} should use.
	 * <p>
	 * If this is set to {@code -1} JDA will automatically retrieve the recommended
	 * amount of shards from discord (default behavior).
	 *
	 * @param shardsTotal The number of overall shards or {@code -1} if JDA should
	 *                    use the recommended amount from discord.
	 *
	 * @return The SpringJDAContextBuilder instance. Useful for chaining.
	 *
	 * @see #setShards(int, int)
	 */

	public SpringJDAContextBuilder setShardsTotal(final int shardsTotal) {
		Checks.check(shardsTotal == -1 || shardsTotal > 0, "shardsTotal must either be -1 or greater than 0");
		this.shardsTotal = shardsTotal;

		return this;
	}

	/**
	 * Whether the {@link net.dv8tion.jda.api.sharding.ShardManager ShardManager}
	 * should use {@link net.dv8tion.jda.api.JDA#shutdownNow() JDA#shutdownNow()}
	 * instead of {@link net.dv8tion.jda.api.JDA#shutdown() JDA#shutdown()} to
	 * shutdown it's shards. <br>
	 * <b>Default</b>: {@code false}
	 *
	 * @param useShutdownNow Whether the ShardManager should use JDA#shutdown() or
	 *                       not
	 *
	 * @return The SpringJDAContextBuilder instance. Useful for chaining.
	 *
	 * @see net.dv8tion.jda.api.JDA#shutdown()
	 * @see net.dv8tion.jda.api.JDA#shutdownNow()
	 */

	public SpringJDAContextBuilder setUseShutdownNow(final boolean useShutdownNow) {
		return setFlag(ShardingConfigFlag.SHUTDOWN_NOW, useShutdownNow);
	}

	/**
	 * Sets the {@link com.neovisionaries.ws.client.WebSocketFactory
	 * WebSocketFactory} that will be used by JDA's websocket client. This can be
	 * used to set things such as connection timeout and proxy.
	 *
	 * @param factory The new {@link com.neovisionaries.ws.client.WebSocketFactory
	 *                WebSocketFactory} to use.
	 *
	 * @return The SpringJDAContextBuilder instance. Useful for chaining.
	 */

	public SpringJDAContextBuilder setWebsocketFactory(WebSocketFactory factory) {
		this.wsFactory = factory;
		return this;
	}

	/**
	 * The {@link ChunkingFilter} to filter which guilds should use member chunking.
	 *
	 * <p>
	 * Use {@link #setMemberCachePolicy(MemberCachePolicy)} to configure which
	 * members to keep in cache from chunking.
	 *
	 * @param filter The filter to apply
	 *
	 * @return The SpringJDAContextBuilder instance. Useful for chaining.
	 *
	 * @since 4.0.0
	 *
	 * @see ChunkingFilter#NONE
	 * @see ChunkingFilter#include(long...)
	 * @see ChunkingFilter#exclude(long...)
	 */

	public SpringJDAContextBuilder setChunkingFilter(ChunkingFilter filter) {
		this.chunkingFilter = filter;
		return this;
	}

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
	 * @return The SpringJDAContextBuilder instance. Useful for chaining.
	 *
	 * @see #setMemberCachePolicy(MemberCachePolicy)
	 *
	 * @since 4.2.0
	 */

	public SpringJDAContextBuilder setDisabledIntents(GatewayIntent intent, GatewayIntent... intents) {
		Checks.notNull(intent, "Intent");
		Checks.noneNull(intents, "Intent");
		EnumSet<GatewayIntent> set = EnumSet.of(intent, intents);
		return setDisabledIntents(set);
	}

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
	 * @return The SpringJDAContextBuilder instance. Useful for chaining.
	 *
	 * @see #setMemberCachePolicy(MemberCachePolicy)
	 *
	 * @since 4.2.0
	 */

	public SpringJDAContextBuilder setDisabledIntents(Collection<GatewayIntent> intents) {
		this.intents = GatewayIntent.ALL_INTENTS;
		if (intents != null)
			this.intents &= ~GatewayIntent.getRaw(intents);
		return this;
	}

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
	 * @return The SpringJDAContextBuilder instance. Useful for chaining.
	 *
	 * @see #enableIntents(Collection)
	 */

	public SpringJDAContextBuilder disableIntents(@NonNull Collection<GatewayIntent> intents) {
		Checks.noneNull(intents, "GatewayIntent");
		int raw = GatewayIntent.getRaw(intents);
		this.intents &= ~raw;
		return this;
	}

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
	 * @return The SpringJDAContextBuilder instance. Useful for chaining.
	 *
	 * @see #enableIntents(GatewayIntent, GatewayIntent...)
	 */

	public SpringJDAContextBuilder disableIntents(@NonNull GatewayIntent intent, @NonNull GatewayIntent... intents) {
		Checks.notNull(intent, "GatewayIntent");
		Checks.noneNull(intents, "GatewayIntent");
		int raw = GatewayIntent.getRaw(intent, intents);
		this.intents &= ~raw;
		return this;
	}

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
	 * @param intent  The intent to enable
	 * @param intents Any other intents to enable
	 *
	 * @throws IllegalArgumentException If null is provided
	 *
	 * @return The SpringJDAContextBuilder instance. Useful for chaining.
	 *
	 * @see #setMemberCachePolicy(MemberCachePolicy)
	 *
	 * @since 4.2.0
	 */

	public SpringJDAContextBuilder setEnabledIntents(GatewayIntent intent, GatewayIntent... intents) {
		Checks.notNull(intent, "Intent");
		Checks.noneNull(intents, "Intent");
		EnumSet<GatewayIntent> set = EnumSet.of(intent, intents);
		return setDisabledIntents(EnumSet.complementOf(set));
	}

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
	 * @return The SpringJDAContextBuilder instance. Useful for chaining.
	 *
	 * @see #setMemberCachePolicy(MemberCachePolicy)
	 *
	 * @since 4.2.0
	 */

	public SpringJDAContextBuilder setEnabledIntents(Collection<GatewayIntent> intents) {
		if (intents == null || intents.isEmpty())
			setDisabledIntents(EnumSet.allOf(GatewayIntent.class));
		else if (intents instanceof EnumSet)
			setDisabledIntents(EnumSet.complementOf((EnumSet<GatewayIntent>) intents));
		else
			setDisabledIntents(EnumSet.complementOf(EnumSet.copyOf(intents)));
		return this;
	}

	/**
	 * Enable the specified {@link GatewayIntent GatewayIntents}. <br>
	 * This will not disable any currently set intents.
	 *
	 * @param intents The intents to enable
	 *
	 * @throws IllegalArgumentException If provided with null
	 *
	 * @return The SpringJDAContextBuilder instance. Useful for chaining.
	 *
	 * @see #disableIntents(Collection)
	 */

	public SpringJDAContextBuilder enableIntents(@NonNull Collection<GatewayIntent> intents) {
		Checks.noneNull(intents, "GatewayIntent");
		int raw = GatewayIntent.getRaw(intents);
		this.intents |= raw;
		return this;
	}

	/**
	 * Enable the specified {@link GatewayIntent GatewayIntents}. <br>
	 * This will not disable any currently set intents.
	 *
	 * @param intent  The intent to enable
	 * @param intents Other intents to enable
	 *
	 * @throws IllegalArgumentException If provided with null
	 *
	 * @return The SpringJDAContextBuilder instance. Useful for chaining.
	 *
	 * @see #enableIntents(GatewayIntent, GatewayIntent...)
	 */

	public SpringJDAContextBuilder enableIntents(@NonNull GatewayIntent intent, @NonNull GatewayIntent... intents) {
		Checks.notNull(intent, "GatewayIntent");
		Checks.noneNull(intents, "GatewayIntent");
		int raw = GatewayIntent.getRaw(intent, intents);
		this.intents |= raw;
		return this;
	}

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
	 * @return The SpringJDAContextBuilder instance. Useful for chaining.
	 *
	 * @since 4.0.0
	 */

	public SpringJDAContextBuilder setLargeThreshold(int threshold) {
		this.largeThreshold = Math.max(50, Math.min(250, threshold)); // enforce 50 <= t <= 250
		return this;
	}

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
	 * @return The SpringJDAContextBuilder instance. Useful for chaining.
	 */

	public SpringJDAContextBuilder setMaxBufferSize(int bufferSize) {
		Checks.notNegative(bufferSize, "The buffer size");
		this.maxBufferSize = bufferSize;
		return this;
	}

	private SpringJDAContextBuilder setFlag(ConfigFlag flag, boolean enable) {
		if (enable)
			this.flags.add(flag);
		else
			this.flags.remove(flag);
		return this;
	}

	private SpringJDAContextBuilder setFlag(ShardingConfigFlag flag, boolean enable) {
		if (enable)
			this.shardingFlags.add(flag);
		else
			this.shardingFlags.remove(flag);
		return this;
	}

	// Avoid having multiple anonymous classes
	private static class ThreadPoolProviderImpl<T extends ExecutorService> implements ThreadPoolProvider<T> {
		private final boolean autoShutdown;
		private final T pool;

		public ThreadPoolProviderImpl(T pool, boolean autoShutdown) {
			this.autoShutdown = autoShutdown;
			this.pool = pool;
		}

		@Override
		public T provide(int shardId) {
			return pool;
		}

		@Override
		public boolean shouldShutdownAutomatically(int shardId) {
			return autoShutdown;
		}
	}
}
