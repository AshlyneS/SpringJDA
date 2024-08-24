package net.foxgenesis.springJDA;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import net.dv8tion.jda.api.JDA.ShardInfo;
import net.dv8tion.jda.api.JDA.Status;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Entitlement;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Icon;
import net.dv8tion.jda.api.entities.ScheduledEvent;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;
import net.dv8tion.jda.api.entities.sticker.GuildSticker;
import net.dv8tion.jda.api.entities.sticker.RichSticker;
import net.dv8tion.jda.api.entities.sticker.StandardSticker;
import net.dv8tion.jda.api.entities.sticker.Sticker;
import net.dv8tion.jda.api.entities.sticker.StickerItem;
import net.dv8tion.jda.api.entities.sticker.StickerPack;
import net.dv8tion.jda.api.entities.sticker.StickerSnowflake;
import net.dv8tion.jda.api.entities.sticker.StickerUnion;
import net.dv8tion.jda.api.hooks.IEventManager;
import net.dv8tion.jda.api.managers.AudioManager;
import net.dv8tion.jda.api.managers.DirectAudioController;
import net.dv8tion.jda.api.managers.Presence;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.AuditableRestAction;
import net.dv8tion.jda.api.requests.restaction.CacheRestAction;
import net.dv8tion.jda.api.requests.restaction.GuildAction;
import net.dv8tion.jda.api.requests.restaction.TestEntitlementCreateAction;
import net.dv8tion.jda.api.requests.restaction.pagination.EntitlementPaginationAction;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.utils.MiscUtil;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.dv8tion.jda.api.utils.cache.CacheView;
import net.dv8tion.jda.api.utils.cache.SnowflakeCacheView;
import okhttp3.OkHttpClient;

/**
 * Interface containing proxy methods for interacting with a wrapped {@link JDA}
 * instance.
 * 
 * @see SpringJDA
 * @see ShardedSpringJDA
 * @author Ashley
 */
public interface SingleSpringJDA extends SpringJDA {

	/**
	 * Gets the current {@link net.dv8tion.jda.api.JDA.Status Status} of the JDA
	 * instance.
	 *
	 * @return Current JDA status.
	 */
	@NonNull
	Status getStatus();

	/**
	 * The {@link CacheFlag cache flags} that have been enabled for this JDA
	 * session.
	 *
	 * @return Copy of the EnumSet of cache flags for this session
	 */
	@NonNull
	EnumSet<CacheFlag> getCacheFlags();

	/**
	 * Attempts to remove the user with the provided id from the cache. <br>
	 * If you attempt to remove the {@link #getSelfUser() SelfUser} this will simply
	 * return {@code false}.
	 *
	 * <p>
	 * This should be used by an implementation of
	 * {@link net.dv8tion.jda.api.utils.MemberCachePolicy MemberCachePolicy} as an
	 * upstream request to remove a member.
	 *
	 * @param userId The target user id
	 *
	 * @return True, if the cache was changed
	 */
	boolean unloadUser(long userId);

	/**
	 * The time in milliseconds that discord took to respond to our last heartbeat
	 * <br>
	 * This roughly represents the WebSocket ping of this session
	 *
	 * <p>
	 * <b>{@link net.dv8tion.jda.api.requests.RestAction RestAction} request times
	 * do not correlate to this value!</b>
	 *
	 * <p>
	 * The {@link net.dv8tion.jda.api.events.GatewayPingEvent GatewayPingEvent}
	 * indicates an update to this value.
	 *
	 * @return time in milliseconds between heartbeat and the heartbeat ack response
	 *
	 * @see #getRestPing() Getting RestAction ping
	 */
	long getGatewayPing();

	/**
	 * The time in milliseconds that discord took to respond to a REST request. <br>
	 * This will request the current user from the API and calculate the time the
	 * response took.
	 *
	 * <p>
	 * <b>Example</b><br>
	 * 
	 * <pre><code>
	 * jda.getRestPing().queue( (time) {@literal ->}
	 *     channel.sendMessageFormat("Ping: %d ms", time).queue()
	 * );
	 * </code></pre>
	 *
	 * @return {@link net.dv8tion.jda.api.requests.RestAction RestAction} - Type:
	 *         long
	 *
	 * @since 4.0.0
	 *
	 * @see #getGatewayPing()
	 */
	@NonNull
	RestAction<Long> getRestPing();

	/**
	 * Cancels all currently scheduled {@link RestAction} requests. <br>
	 * When a {@link RestAction} is cancelled, a
	 * {@link java.util.concurrent.CancellationException} will be provided to the
	 * failure callback. This means {@link RestAction#queue(Consumer, Consumer)}
	 * will invoke the second callback and {@link RestAction#complete()} will throw
	 * an exception.
	 *
	 * <p>
	 * <b>This is only recommended as an extreme last measure to avoid
	 * backpressure.</b> If you want to stop requests on shutdown you should use
	 * {@link #shutdownNow()} instead of this method.
	 *
	 * @return how many requests were cancelled
	 *
	 * @see RestAction#setCheck(BooleanSupplier)
	 */
	int cancelRequests();

	/**
	 * {@link ScheduledExecutorService} used to handle rate-limits for
	 * {@link RestAction} executions. This is also used in other parts of JDA
	 * related to http requests.
	 *
	 * @return The {@link ScheduledExecutorService} used for http request handling
	 *
	 * @since 4.0.0
	 */
	@NonNull
	ScheduledExecutorService getRateLimitPool();

	/**
	 * {@link ScheduledExecutorService} used to send WebSocket messages to discord.
	 * <br>
	 * This involves initial setup of guilds as well as keeping the connection
	 * alive.
	 *
	 * @return The {@link ScheduledExecutorService} used for WebSocket transmissions
	 *
	 * @since 4.0.0
	 */
	@NonNull
	ScheduledExecutorService getGatewayPool();

	/**
	 * {@link ExecutorService} used to handle {@link RestAction} callbacks and
	 * completions. This is also used for handling
	 * {@link net.dv8tion.jda.api.entities.Message.Attachment} downloads when
	 * needed. <br>
	 * By default this uses the {@link ForkJoinPool#commonPool() CommonPool} of the
	 * runtime.
	 *
	 * @return The {@link ExecutorService} used for callbacks
	 *
	 * @since 4.0.0
	 */
	@NonNull
	ExecutorService getCallbackPool();

	/**
	 * The {@link OkHttpClient} used for handling http requests from
	 * {@link RestAction RestActions}.
	 *
	 * @return The http client
	 *
	 * @since 4.0.0
	 */
	@NonNull
	OkHttpClient getHttpClient();

	/**
	 * Direct access to audio (dis-)connect requests. <br>
	 * This should not be used when normal audio operation is desired.
	 *
	 * <p>
	 * The correct way to open and close an audio connection is through the
	 * {@link Guild Guild's} {@link AudioManager}.
	 *
	 * @throws IllegalStateException If {@link GatewayIntent#GUILD_VOICE_STATES} is
	 *                               disabled
	 *
	 * @return The {@link DirectAudioController} for this JDA instance
	 *
	 * @since 4.0.0
	 */
	@NonNull
	DirectAudioController getDirectAudioController();

	/**
	 * Changes the internal EventManager.
	 *
	 * <p>
	 * The default EventManager is
	 * {@link net.dv8tion.jda.api.hooks.InterfacedEventManager
	 * InterfacedEventListener}. <br>
	 * There is also an {@link net.dv8tion.jda.api.hooks.AnnotatedEventManager
	 * AnnotatedEventManager} available.
	 *
	 * @param manager The new EventManager to use
	 */
	void setEventManager(@Nullable IEventManager manager);

	/**
	 * Immutable List of Objects that have been registered as EventListeners.
	 *
	 * @return List of currently registered Objects acting as EventListeners.
	 */
	@NonNull
	List<Object> getRegisteredListeners();

	/**
	 * Constructs a new {@link Guild Guild} with the specified name <br>
	 * Use the returned {@link GuildAction GuildAction} to provide further details
	 * and settings for the resulting Guild!
	 *
	 * <p>
	 * This RestAction does not provide the resulting Guild! It will be in a
	 * following {@link net.dv8tion.jda.api.events.guild.GuildJoinEvent
	 * GuildJoinEvent}.
	 *
	 * @param name The name of the resulting guild
	 *
	 * @throws java.lang.IllegalStateException    If the currently logged in account
	 *                                            is in 10 or more guilds
	 * @throws java.lang.IllegalArgumentException If the provided name is empty,
	 *                                            {@code null} or not between 2-100
	 *                                            characters
	 *
	 * @return {@link GuildAction GuildAction} <br>
	 *         Allows for setting various details for the resulting Guild
	 */
	@NonNull
	GuildAction createGuild(@NonNull String name);

	/**
	 * Constructs a new {@link Guild Guild} from the specified template code.
	 *
	 * <p>
	 * This RestAction does not provide the resulting Guild! It will be in a
	 * following {@link net.dv8tion.jda.api.events.guild.GuildJoinEvent
	 * GuildJoinEvent}.
	 *
	 * <p>
	 * Possible {@link net.dv8tion.jda.api.requests.ErrorResponse ErrorResponses}
	 * include:
	 * <ul>
	 * <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#UNKNOWN_GUILD_TEMPLATE
	 * Unknown Guild Template} <br>
	 * The template doesn't exist.</li>
	 * </ul>
	 *
	 * @param code The template code to use to create a guild
	 * @param name The name of the resulting guild
	 * @param icon The {@link net.dv8tion.jda.api.entities.Icon Icon} to use, or
	 *             null to use no icon
	 *
	 * @throws java.lang.IllegalStateException    If the currently logged in account
	 *                                            is in 10 or more guilds
	 * @throws java.lang.IllegalArgumentException If the provided name is empty,
	 *                                            {@code null} or not between 2-100
	 *                                            characters
	 *
	 * @return {@link net.dv8tion.jda.api.requests.RestAction RestAction}
	 */
	@NonNull
	RestAction<Void> createGuildFromTemplate(@NonNull String code, @NonNull String name, @Nullable Icon icon);

	/**
	 * {@link net.dv8tion.jda.api.utils.cache.CacheView CacheView} of all cached
	 * {@link net.dv8tion.jda.api.managers.AudioManager AudioManagers} created for
	 * this JDA instance. <br>
	 * AudioManagers are created when first retrieved via
	 * {@link Guild#getAudioManager() Guild.getAudioManager()}. <u>Using this will
	 * perform better than calling {@code Guild.getAudioManager()} iteratively as
	 * that would cause many useless audio managers to be created!</u>
	 *
	 * <p>
	 * AudioManagers are cross-session persistent!
	 *
	 * @return {@link net.dv8tion.jda.api.utils.cache.CacheView CacheView}
	 */
	@NonNull
	CacheView<AudioManager> getAudioManagerCache();

	/**
	 * Set of {@link Guild} IDs for guilds that were marked unavailable by the
	 * gateway. <br>
	 * When a guild becomes unavailable a
	 * {@link net.dv8tion.jda.api.events.guild.GuildUnavailableEvent
	 * GuildUnavailableEvent} is emitted and a
	 * {@link net.dv8tion.jda.api.events.guild.GuildAvailableEvent
	 * GuildAvailableEvent} is emitted when it becomes available again. During the
	 * time a guild is unavailable it its not reachable through cache such as
	 * {@link #getGuildById(long)}.
	 *
	 * @return Possibly-empty set of guild IDs for unavailable guilds
	 */
	@NonNull
	Set<String> getUnavailableGuilds();

	/**
	 * Whether the guild is unavailable. If this returns true, the guild id should
	 * be in {@link #getUnavailableGuilds()}.
	 *
	 * @param guildId The guild id
	 *
	 * @return True, if this guild is unavailable
	 */
	boolean isUnavailable(long guildId);

	/**
	 * {@link SnowflakeCacheView} of all cached {@link ScheduledEvent
	 * ScheduledEvents} visible to this JDA session.
	 *
	 * <p>
	 * This requires {@link CacheFlag#SCHEDULED_EVENTS} to be enabled.
	 *
	 * @return {@link SnowflakeCacheView}
	 */
	@NonNull
	SnowflakeCacheView<ScheduledEvent> getScheduledEventCache();

	/**
	 * Opens a {@link PrivateChannel} with the provided user by id. <br>
	 * This will fail with
	 * {@link net.dv8tion.jda.api.requests.ErrorResponse#UNKNOWN_USER UNKNOWN_USER}
	 * if the user does not exist.
	 *
	 * <p>
	 * If the channel is cached, this will directly return the channel in a
	 * completed {@link RestAction} without making a request. You can use
	 * {@link CacheRestAction#useCache(boolean) action.useCache(false)} to force an
	 * update.
	 *
	 * <p>
	 * <b>Example</b><br>
	 * 
	 * <pre>{@code
	 * default void sendMessage(JDA jda, long userId, String content) {
	 * 	jda.openPrivateChannelById(userId).flatMap(channel -> channel.sendMessage(content)).queue();
	 * }
	 * }</pre>
	 *
	 * @param userId The id of the target user
	 *
	 * @throws UnsupportedOperationException If the target user is the currently
	 *                                       logged in account
	 *
	 * @return {@link CacheRestAction} - Type: {@link PrivateChannel}
	 *
	 * @see User#openPrivateChannel()
	 */
	@NonNull
	CacheRestAction<PrivateChannel> openPrivateChannelById(long userId);

	/**
	 * Attempts to retrieve a {@link Sticker} object based on the provided snowflake
	 * reference. <br>
	 * This works for both {@link StandardSticker} and {@link GuildSticker}, and you
	 * can resolve them using the provided {@link StickerUnion}.
	 *
	 * <p>
	 * If the sticker is not one of the supported {@link Sticker.Type Types}, the
	 * request fails with {@link IllegalArgumentException}.
	 *
	 * <p>
	 * The returned {@link net.dv8tion.jda.api.requests.RestAction RestAction} can
	 * encounter the following Discord errors:
	 * <ul>
	 * <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#UNKNOWN_STICKER
	 * UNKNOWN_STICKER} <br>
	 * Occurs when the provided id does not refer to a sticker known by
	 * Discord.</li>
	 * </ul>
	 *
	 * @param sticker The reference of the requested {@link Sticker}. <br>
	 *                Can be {@link RichSticker}, {@link StickerItem}, or
	 *                {@link Sticker#fromId(long)}.
	 *
	 * @throws IllegalArgumentException If the provided sticker is null
	 *
	 * @return {@link net.dv8tion.jda.api.requests.RestAction RestAction} - Type:
	 *         {@link StickerUnion} <br>
	 *         On request, gets the sticker with id matching provided id from
	 *         Discord.
	 */
	@NonNull
	RestAction<StickerUnion> retrieveSticker(@NonNull StickerSnowflake sticker);

	/**
	 * Retrieves a list of all the default {@link StickerPack StickerPacks} used for
	 * nitro.
	 *
	 * @return {@link net.dv8tion.jda.api.requests.RestAction RestAction} - Type:
	 *         List of {@link StickerPack}
	 */
	@NonNull
	RestAction<List<StickerPack>> retrieveNitroStickerPacks();

	/**
	 * The EventManager used by this JDA instance.
	 *
	 * @return The {@link net.dv8tion.jda.api.hooks.IEventManager}
	 */
	@NonNull
	IEventManager getEventManager();

	/**
	 * The {@link net.dv8tion.jda.api.managers.Presence Presence} controller for the
	 * current session. <br>
	 * Used to set {@link net.dv8tion.jda.api.entities.Activity} and
	 * {@link net.dv8tion.jda.api.OnlineStatus} information.
	 *
	 * @return The never-null {@link net.dv8tion.jda.api.managers.Presence Presence}
	 *         for this session.
	 */
	@NonNull
	Presence getPresence();

	/**
	 * The shard information used when creating this instance of JDA. <br>
	 * Represents the information provided to
	 * {@link net.dv8tion.jda.api.JDABuilder#useSharding(int, int)}.
	 *
	 * @return The shard information for this shard
	 */
	@NonNull
	ShardInfo getShardInfo();

	/**
	 * This value is the total amount of JSON responses that discord has sent. <br>
	 * This value resets every time the websocket has to perform a full reconnect
	 * (not resume).
	 *
	 * @return Never-negative long containing total response amount.
	 */
	long getResponseTotal();

	/**
	 * This value is the maximum amount of time, in seconds, that JDA will wait
	 * between reconnect attempts. <br>
	 * Can be set using
	 * {@link net.dv8tion.jda.api.JDABuilder#setMaxReconnectDelay(int)
	 * JDABuilder.setMaxReconnectDelay(int)}.
	 *
	 * @return The maximum amount of time JDA will wait between reconnect attempts
	 *         in seconds.
	 */
	int getMaxReconnectDelay();

	/**
	 * Sets whether or not JDA should try to automatically reconnect if a
	 * connection-error is encountered. <br>
	 * This will use an incremental reconnect (timeouts are increased each time an
	 * attempt fails).
	 *
	 * <p>
	 * default is <b>true</b>.
	 *
	 * @param reconnect If true - enables autoReconnect
	 */
	void setAutoReconnect(boolean reconnect);

	/**
	 * Whether the Requester should retry when a
	 * {@link java.net.SocketTimeoutException SocketTimeoutException} occurs.
	 *
	 * @param retryOnTimeout True, if the Request should retry once on a socket
	 *                       timeout
	 */
	void setRequestTimeoutRetry(boolean retryOnTimeout);

	/**
	 * USed to determine whether or not autoReconnect is enabled for JDA.
	 *
	 * @return True if JDA will attempt to automatically reconnect when a
	 *         connection-error is encountered.
	 */
	boolean isAutoReconnect();

	/**
	 * Used to determine if JDA will process MESSAGE_DELETE_BULK messages received
	 * from Discord as a single
	 * {@link net.dv8tion.jda.api.events.message.MessageBulkDeleteEvent
	 * MessageBulkDeleteEvent} or split the deleted messages up and fire multiple
	 * {@link net.dv8tion.jda.api.events.message.MessageDeleteEvent
	 * MessageDeleteEvents}, one for each deleted message.
	 *
	 * <p>
	 * By default, JDA will separate the bulk delete event into individual delete
	 * events, but this isn't as efficient as handling a single event would be. It
	 * is recommended that BulkDelete Splitting be disabled and that the developer
	 * should instead handle the
	 * {@link net.dv8tion.jda.api.events.message.MessageBulkDeleteEvent
	 * MessageBulkDeleteEvent}
	 *
	 * @return Whether or not JDA currently handles the BULK_MESSAGE_DELETE event by
	 *         splitting it into individual MessageDeleteEvents or not.
	 */
	boolean isBulkDeleteSplittingEnabled();

	/**
	 * A {@link net.dv8tion.jda.api.requests.restaction.pagination.PaginationAction
	 * PaginationAction} implementation which allows you to {@link Iterable iterate}
	 * over {@link Entitlement}s that are applicable to the logged in application.
	 *
	 * @return {@link EntitlementPaginationAction EntitlementPaginationAction}
	 */
	@NonNull
	EntitlementPaginationAction retrieveEntitlements();

	/**
	 * Retrieves an {@link Entitlement} by its id.
	 *
	 * @param entitlementId The id of the entitlement to retrieve
	 *
	 * @throws IllegalArgumentException If the provided id is not a valid snowflake
	 *
	 * @return {@link RestAction} - Type: {@link Entitlement} <br>
	 *         The entitlement with the provided id
	 */
	@NonNull
	default RestAction<Entitlement> retrieveEntitlementById(@NonNull String entitlementId) {
		return retrieveEntitlementById(MiscUtil.parseSnowflake(entitlementId));
	}

	/**
	 * Retrieves an {@link Entitlement} by its id.
	 *
	 * @param entitlementId The id of the entitlement to retrieve
	 *
	 * @return {@link RestAction} - Type: {@link Entitlement} <br>
	 *         The entitlement with the provided id
	 */
	@NonNull
	RestAction<Entitlement> retrieveEntitlementById(long entitlementId);

	/**
	 * Constructs a new {@link Entitlement Entitlement} with the skuId and the type.
	 * <br>
	 * Use the returned {@link TestEntitlementCreateAction
	 * TestEntitlementCreateAction} to provide more details.
	 *
	 * @param skuId     The id of the SKU the entitlement is for
	 *
	 * @param ownerId   The id of the owner of the entitlement
	 *
	 * @param ownerType The type of the owner of the entitlement
	 *
	 * @throws IllegalArgumentException If the provided skuId or ownerId is not a
	 *                                  valid snowflake
	 *
	 * @return {@link TestEntitlementCreateAction TestEntitlementCreateAction} <br>
	 *         Allows for setting various details for the resulting Entitlement
	 */
	@NonNull
	default TestEntitlementCreateAction createTestEntitlement(@NonNull String skuId, @NonNull String ownerId,
			@NonNull TestEntitlementCreateAction.OwnerType ownerType) {
		return createTestEntitlement(MiscUtil.parseSnowflake(skuId), MiscUtil.parseSnowflake(ownerId), ownerType);
	}

	/**
	 * Constructs a new {@link Entitlement Entitlement} with the skuId and the type.
	 * <br>
	 * Use the returned {@link TestEntitlementCreateAction
	 * TestEntitlementCreateAction} to provide more details.
	 *
	 * @param skuId     The id of the SKU the entitlement is for
	 *
	 * @param ownerId   The id of the owner of the entitlement
	 *
	 * @param ownerType The type of the owner of the entitlement
	 *
	 * @throws IllegalArgumentException If the provided ownerType is null
	 *
	 * @return {@link TestEntitlementCreateAction TestEntitlementCreateAction} <br>
	 *         Allows for setting various details for the resulting Entitlement
	 */
	@NonNull
	TestEntitlementCreateAction createTestEntitlement(long skuId, long ownerId,
			@NonNull TestEntitlementCreateAction.OwnerType ownerType);

	/**
	 * Deletes a test entitlement by its id.
	 *
	 * @param entitlementId The id of the entitlement to delete
	 *
	 * @throws IllegalArgumentException If the provided id is not a valid snowflake
	 *
	 * @return {@link RestAction} - Type: Void
	 */
	@NonNull
	default RestAction<Void> deleteTestEntitlement(@NonNull String entitlementId) {
		return deleteTestEntitlement(MiscUtil.parseSnowflake(entitlementId));
	}

	/**
	 * Deletes a test entitlement by its id.
	 *
	 * @param entitlementId The id of the entitlement to delete
	 *
	 * @return {@link RestAction} - Type: Void
	 */
	@NonNull
	RestAction<Void> deleteTestEntitlement(long entitlementId);

	/**
	 * Returns the {@link net.dv8tion.jda.api.sharding.ShardManager ShardManager}
	 * that manages this JDA instances or null if this instance is not managed by
	 * any {@link net.dv8tion.jda.api.sharding.ShardManager ShardManager}.
	 *
	 * @return The corresponding ShardManager or {@code null} if there is no such
	 *         manager
	 */
	@Nullable
	ShardManager getShardManager();

	/**
	 * Installs an auxiliary port for audio transfer.
	 *
	 * @throws IllegalStateException If this is a headless environment or no port is
	 *                               available
	 *
	 * @return {@link AuditableRestAction} - Type: int Provides the resulting used
	 *         port
	 */
	@NonNull
	AuditableRestAction<Integer> installAuxiliaryPort();

	@Override
	default void setActivity(Activity activity) {
		getPresence().setActivity(activity);
	}

	@Override
	default void setIdle(boolean idle) {
		getPresence().setIdle(idle);
	}

	@Override
	default void setPresence(OnlineStatus status, Activity activity) {
		getPresence().setPresence(status, activity);
	}

	@Override
	default void setStatus(OnlineStatus status) {
		getPresence().setStatus(status);
	}
	
	@Override
	default boolean isShuttingDown() {
		Status status = getStatus();
		return status == Status.SHUTDOWN || status == Status.SHUTTING_DOWN;
	}
}
