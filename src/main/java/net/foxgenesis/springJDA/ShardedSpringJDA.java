package net.foxgenesis.springJDA;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.Collectors;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDA.Status;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.ApplicationInfo;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.RoleConnectionMetadata;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.Webhook;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.ForumChannel;
import net.dv8tion.jda.api.entities.channel.concrete.MediaChannel;
import net.dv8tion.jda.api.entities.channel.concrete.NewsChannel;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;
import net.dv8tion.jda.api.entities.channel.concrete.StageChannel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.emoji.RichCustomEmoji;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.Route;
import net.dv8tion.jda.api.requests.restaction.CommandEditAction;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import net.dv8tion.jda.api.sharding.DefaultShardManager;
import net.dv8tion.jda.api.utils.cache.CacheView;
import net.dv8tion.jda.api.utils.cache.ChannelCacheView;
import net.dv8tion.jda.api.utils.cache.ShardCacheView;
import net.dv8tion.jda.api.utils.cache.SnowflakeCacheView;
import net.dv8tion.jda.internal.JDAImpl;
import net.dv8tion.jda.internal.requests.CompletedRestAction;
import net.dv8tion.jda.internal.requests.RestActionImpl;
import net.dv8tion.jda.internal.utils.Checks;
import net.dv8tion.jda.internal.utils.cache.UnifiedChannelCacheView;

/**
 * Interface containing proxy methods for interacting with a wrapped {@link ShardManager}
 * instance.
 * @author Ashley
 * @see SpringJDA
 * @see SingleSpringJDA
 */
public interface ShardedSpringJDA extends SpringJDA {

	/**
	 * Adds all provided listeners to the event-listeners that will be used to
	 * handle events.
	 *
	 * <p>
	 * Note: when using the {@link net.dv8tion.jda.api.hooks.InterfacedEventManager
	 * InterfacedEventListener} (default), the given listener <b>must</b> be an
	 * instance of {@link net.dv8tion.jda.api.hooks.EventListener EventListener}!
	 *
	 * @param listeners The listener(s) which will react to events.
	 *
	 * @throws java.lang.IllegalArgumentException If either listeners or one of it's
	 *                                            objects is {@code null}.
	 */
	@Override
	default void addEventListener(@NonNull final Object... listeners) {
		Checks.noneNull(listeners, "listeners");
		this.getShardCache().forEach(jda -> jda.addEventListener(listeners));
	}

	/**
	 * Removes all provided listeners from the event-listeners and no longer uses
	 * them to handle events.
	 *
	 * @param listeners The listener(s) to be removed.
	 *
	 * @throws java.lang.IllegalArgumentException If either listeners or one of it's
	 *                                            objects is {@code null}.
	 */
	@Override
	default void removeEventListener(@NonNull final Object... listeners) {
		Checks.noneNull(listeners, "listeners");
		this.getShardCache().forEach(jda -> jda.removeEventListener(listeners));
	}
	
	@Override
	default User getSelfUser() {
		return anyShard().getSelfUser();
	}

	/**
	 * Adds listeners provided by the listener provider to each shard to the
	 * event-listeners that will be used to handle events. The listener provider
	 * gets a shard id applied and is expected to return a listener.
	 *
	 * <p>
	 * Note: when using the {@link net.dv8tion.jda.api.hooks.InterfacedEventManager
	 * InterfacedEventListener} (default), the given listener <b>must</b> be an
	 * instance of {@link net.dv8tion.jda.api.hooks.EventListener EventListener}!
	 *
	 * @param eventListenerProvider The provider of listener(s) which will react to
	 *                              events.
	 *
	 * @throws java.lang.IllegalArgumentException If the provided listener provider
	 *                                            or any of the listeners or
	 *                                            provides are {@code null}.
	 */
	default void addEventListeners(@NonNull final IntFunction<Object> eventListenerProvider) {
		Checks.notNull(eventListenerProvider, "event listener provider");
		this.getShardCache().forEach(jda -> {
			Object listener = eventListenerProvider.apply(jda.getShardInfo().getShardId());
			if (listener != null)
				jda.addEventListener(listener);
		});
	}

	/**
	 * Remove listeners from shards by their id. The provider takes shard ids, and
	 * returns a collection of listeners that shall be removed from the respective
	 * shards.
	 *
	 * @param eventListenerProvider Gets shard ids applied and is expected to return
	 *                              a collection of listeners that shall be removed
	 *                              from the respective shards
	 *
	 * @throws java.lang.IllegalArgumentException If the provided event listeners
	 *                                            provider is {@code null}.
	 */
	default void removeEventListeners(@NonNull final IntFunction<Collection<Object>> eventListenerProvider) {
		Checks.notNull(eventListenerProvider, "event listener provider");
		this.getShardCache()
				.forEach(jda -> jda.removeEventListener(eventListenerProvider.apply(jda.getShardInfo().getShardId())));
	}

	/**
	 * Remove a listener provider. This will stop further created / restarted shards
	 * from getting a listener added by that provider.
	 *
	 * <p>
	 * Default is a no-op for backwards compatibility, see implementations like
	 * {@link DefaultShardManager#removeEventListenerProvider(IntFunction)} for
	 * actual code
	 *
	 * @param eventListenerProvider The provider of listeners that shall be removed.
	 *
	 * @throws java.lang.IllegalArgumentException If the provided listener provider
	 *                                            is {@code null}.
	 */
	void removeEventListenerProvider(@NonNull IntFunction<Object> eventListenerProvider);

	/**
	 * Returns the amount of shards queued for (re)connecting.
	 *
	 * @return The amount of shards queued for (re)connecting.
	 */
	int getShardsQueued();

	/**
	 * Returns the amount of running shards.
	 *
	 * @return The amount of running shards.
	 */
	default int getShardsRunning() {
		return (int) this.getShardCache().size();
	}

	/**
	 * Returns the amount of shards managed by this
	 * {@link net.dv8tion.jda.api.sharding.ShardManager ShardManager}. This includes
	 * shards currently queued for a restart.
	 *
	 * @return The managed amount of shards.
	 */
	default int getShardsTotal() {
		return this.getShardsQueued() + this.getShardsRunning();
	}

	/**
	 * The {@link GatewayIntent GatewayIntents} for the JDA sessions of this shard
	 * manager.
	 *
	 * @return {@link EnumSet} of active gateway intents
	 */
	@NonNull
	@Override
	default EnumSet<GatewayIntent> getGatewayIntents() {
		// noinspection ConstantConditions
		return getShardCache().applyStream(
				(stream) -> stream.map(JDA::getGatewayIntents).findAny().orElse(EnumSet.noneOf(GatewayIntent.class)));
	}

	/**
	 * Used to access application details of this bot. <br>
	 * Since this is the same for every shard it picks
	 * {@link JDA#retrieveApplicationInfo()} from any shard.
	 *
	 * @throws java.lang.IllegalStateException If there is no running shard
	 *
	 * @return The Application registry for this bot.
	 */
	@NonNull
	@Override
	default RestAction<ApplicationInfo> retrieveApplicationInfo() {
		return anyShard().retrieveApplicationInfo();
	}

	/**
	 * The average time in milliseconds between all shards that discord took to
	 * respond to our last heartbeat. This roughly represents the WebSocket ping of
	 * this session. If there are no shards running, this will return {@code -1}.
	 *
	 * <p>
	 * <b>{@link net.dv8tion.jda.api.requests.RestAction RestAction} request times
	 * do not correlate to this value!</b>
	 *
	 * @return The average time in milliseconds between heartbeat and the heartbeat
	 *         ack response
	 */
	default double getAverageGatewayPing() {
		return this.getShardCache().stream().mapToLong(JDA::getGatewayPing).filter(ping -> ping != -1).average()
				.orElse(-1D);
	}

	/**
	 * {@link net.dv8tion.jda.api.utils.cache.SnowflakeCacheView SnowflakeCacheView}
	 * of all cached {@link net.dv8tion.jda.api.entities.channel.concrete.Category
	 * Categories} visible to this ShardManager instance.
	 *
	 * @return {@link net.dv8tion.jda.api.utils.cache.SnowflakeCacheView
	 *         SnowflakeCacheView}
	 */
	@NonNull
	@Override
	default SnowflakeCacheView<Category> getCategoryCache() {
		return CacheView.allSnowflakes(() -> this.getShardCache().stream().map(JDA::getCategoryCache));
	}

	/**
	 * Unified {@link net.dv8tion.jda.api.utils.cache.SnowflakeCacheView
	 * SnowflakeCacheView} of all cached {@link RichCustomEmoji RichCustomEmojis}
	 * visible to this ShardManager instance.
	 *
	 * @return Unified {@link net.dv8tion.jda.api.utils.cache.SnowflakeCacheView
	 *         SnowflakeCacheView}
	 */
	@NonNull
	@Override
	default SnowflakeCacheView<RichCustomEmoji> getEmojiCache() {
		return CacheView.allSnowflakes(() -> this.getShardCache().stream().map(JDA::getEmojiCache));
	}

	/**
	 * {@link net.dv8tion.jda.api.utils.cache.SnowflakeCacheView SnowflakeCacheView}
	 * of all cached {@link net.dv8tion.jda.api.entities.Guild Guilds} visible to
	 * this ShardManager instance.
	 *
	 * @return {@link net.dv8tion.jda.api.utils.cache.SnowflakeCacheView
	 *         SnowflakeCacheView}
	 */
	@NonNull
	@Override
	default SnowflakeCacheView<Guild> getGuildCache() {
		return CacheView.allSnowflakes(() -> this.getShardCache().stream().map(JDA::getGuildCache));
	}

	/**
	 * Attempts to retrieve a {@link net.dv8tion.jda.api.entities.User User} object
	 * based on the provided id. <br>
	 * This first calls {@link #getUserById(long)}, and if the return is
	 * {@code null} then a request is made to the Discord servers.
	 *
	 * <p>
	 * The returned {@link net.dv8tion.jda.api.requests.RestAction RestAction} can
	 * encounter the following Discord errors:
	 * <ul>
	 * <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#UNKNOWN_USER
	 * ErrorResponse.UNKNOWN_USER} <br>
	 * Occurs when the provided id does not refer to a
	 * {@link net.dv8tion.jda.api.entities.User User} known by Discord. Typically
	 * occurs when developers provide an incomplete id (cut short).</li>
	 * </ul>
	 *
	 * @param id The id of the requested {@link net.dv8tion.jda.api.entities.User
	 *           User}.
	 *
	 * @throws java.lang.IllegalStateException If there isn't any active shards.
	 *
	 * @return {@link net.dv8tion.jda.api.requests.RestAction RestAction} - Type:
	 *         {@link net.dv8tion.jda.api.entities.User User} <br>
	 *         On request, gets the User with id matching provided id from Discord.
	 */
	@NonNull
	@Override
	default RestAction<User> retrieveUserById(long id) {
		JDA api = null;
		for (JDA shard : getShardCache()) {
			api = shard;
			EnumSet<GatewayIntent> intents = shard.getGatewayIntents();
			User user = shard.getUserById(id);
			boolean isUpdated = intents.contains(GatewayIntent.GUILD_PRESENCES)
					|| intents.contains(GatewayIntent.GUILD_MEMBERS);
			if (user != null && isUpdated)
				return new CompletedRestAction<>(shard, user);
		}

		if (api == null)
			throw new IllegalStateException("no shards active");

		JDAImpl jda = (JDAImpl) api;
		Route.CompiledRoute route = Route.Users.GET_USER.compile(Long.toUnsignedString(id));
		return new RestActionImpl<>(jda, route,
				(response, request) -> jda.getEntityBuilder().createUser(response.getObject()));
	}

	/**
	 * Searches for the first user that has the matching Discord Tag. <br>
	 * Format has to be in the form {@code Username#Discriminator} where the
	 * username must be between 2 and 32 characters (inclusive) matching the exact
	 * casing and the discriminator must be exactly 4 digits.
	 *
	 * <p>
	 * This will only check cached users!
	 *
	 * <p>
	 * This only checks users that are known to the currently logged in account
	 * (shards). If a user exists with the tag that is not available in the
	 * {@link #getUserCache() User-Cache} it will not be detected. <br>
	 * Currently Discord does not offer a way to retrieve a user by their discord
	 * tag.
	 *
	 * @param tag The Discord Tag in the format {@code Username#Discriminator}
	 *
	 * @throws java.lang.IllegalArgumentException If the provided tag is null or not
	 *                                            in the described format
	 *
	 * @return The {@link net.dv8tion.jda.api.entities.User} for the discord tag or
	 *         null if no user has the provided tag
	 */
	@Nullable
	@Override
	default User getUserByTag(@NonNull String tag) {
		return getShardCache().applyStream(
				stream -> stream.map(jda -> jda.getUserByTag(tag)).filter(Objects::nonNull).findFirst().orElse(null));
	}

	/**
	 * Searches for the first user that has the matching Discord Tag. <br>
	 * Format has to be in the form {@code Username#Discriminator} where the
	 * username must be between 2 and 32 characters (inclusive) matching the exact
	 * casing and the discriminator must be exactly 4 digits.
	 *
	 * <p>
	 * This will only check cached users!
	 *
	 * <p>
	 * This only checks users that are known to the currently logged in account
	 * (shards). If a user exists with the tag that is not available in the
	 * {@link #getUserCache() User-Cache} it will not be detected. <br>
	 * Currently Discord does not offer a way to retrieve a user by their discord
	 * tag.
	 *
	 * @param username      The name of the user
	 * @param discriminator The discriminator of the user
	 *
	 * @throws java.lang.IllegalArgumentException If the provided arguments are null
	 *                                            or not in the described format
	 *
	 * @return The {@link net.dv8tion.jda.api.entities.User} for the discord tag or
	 *         null if no user has the provided tag
	 */
	@Nullable
	@Override
	default User getUserByTag(@NonNull String username, @NonNull String discriminator) {
		return getShardCache().applyStream(stream -> stream.map(jda -> jda.getUserByTag(username, discriminator))
				.filter(Objects::nonNull).findFirst().orElse(null));
	}

	/**
	 * Unified {@link net.dv8tion.jda.api.utils.cache.SnowflakeCacheView
	 * SnowflakeCacheView} of all cached {@link net.dv8tion.jda.api.entities.Role
	 * Roles} visible to this ShardManager instance.
	 *
	 * @return Unified {@link net.dv8tion.jda.api.utils.cache.SnowflakeCacheView
	 *         SnowflakeCacheView}
	 */
	@NonNull
	default SnowflakeCacheView<Role> getRoleCache() {
		return CacheView.allSnowflakes(() -> this.getShardCache().stream().map(JDA::getRoleCache));
	}

	/**
	 * {@link net.dv8tion.jda.api.utils.cache.SnowflakeCacheView SnowflakeCacheView}
	 * of all cached
	 * {@link net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel
	 * PrivateChannels} visible to this ShardManager instance.
	 *
	 * @return {@link net.dv8tion.jda.api.utils.cache.SnowflakeCacheView
	 *         SnowflakeCacheView}
	 */
	@NonNull
	@Override
	default SnowflakeCacheView<PrivateChannel> getPrivateChannelCache() {
		return CacheView.allSnowflakes(() -> this.getShardCache().stream().map(JDA::getPrivateChannelCache));
	}

	@Nullable
	@Override
	default GuildChannel getGuildChannelById(long id) {
		GuildChannel channel;
		for (JDA shard : getShards()) {
			channel = shard.getGuildChannelById(id);
			if (channel != null)
				return channel;
		}

		return null;
	}

	@Nullable
	@Override
	default GuildChannel getGuildChannelById(@NonNull ChannelType type, long id) {
		Checks.notNull(type, "ChannelType");
		GuildChannel channel;
		for (JDA shard : getShards()) {
			channel = shard.getGuildChannelById(type, id);
			if (channel != null)
				return channel;
		}

		return null;
	}

	@NonNull
	@Override
	default SnowflakeCacheView<TextChannel> getTextChannelCache() {
		return CacheView.allSnowflakes(() -> this.getShardCache().stream().map(JDA::getTextChannelCache));
	}

	@NonNull
	@Override
	default SnowflakeCacheView<VoiceChannel> getVoiceChannelCache() {
		return CacheView.allSnowflakes(() -> this.getShardCache().stream().map(JDA::getVoiceChannelCache));
	}

	@NonNull
	@Override
	default SnowflakeCacheView<StageChannel> getStageChannelCache() {
		return CacheView.allSnowflakes(() -> this.getShardCache().stream().map(JDA::getStageChannelCache));
	}

	@NonNull
	@Override
	default SnowflakeCacheView<ThreadChannel> getThreadChannelCache() {
		return CacheView.allSnowflakes(() -> this.getShardCache().stream().map(JDA::getThreadChannelCache));
	}

	@NonNull
	@Override
	default SnowflakeCacheView<NewsChannel> getNewsChannelCache() {
		return CacheView.allSnowflakes(() -> this.getShardCache().stream().map(JDA::getNewsChannelCache));
	}

	@NonNull
	@Override
	default SnowflakeCacheView<ForumChannel> getForumChannelCache() {
		return CacheView.allSnowflakes(() -> this.getShardCache().stream().map(JDA::getForumChannelCache));
	}

	@NonNull
	@Override
	default SnowflakeCacheView<MediaChannel> getMediaChannelCache() {
		return CacheView.allSnowflakes(() -> this.getShardCache().stream().map(JDA::getMediaChannelCache));
	}

	@NonNull
	@Override
	default ChannelCacheView<Channel> getChannelCache() {
		return new UnifiedChannelCacheView<>(() -> this.getShardCache().stream().map(JDA::getChannelCache));
	}

	/**
	 * This returns the {@link net.dv8tion.jda.api.JDA JDA} instance which has the
	 * same id as the one provided. <br>
	 * If there is no shard with an id that matches the provided one, this will
	 * return {@code null}.
	 *
	 * @param id The id of the shard.
	 *
	 * @return The {@link net.dv8tion.jda.api.JDA JDA} instance with the given
	 *         shardId or {@code null} if no shard has the given id
	 */
	@Nullable
	default JDA getShardById(final int id) {
		return this.getShardCache().getElementById(id);
	}

	/**
	 * This returns the {@link net.dv8tion.jda.api.JDA JDA} instance which has the
	 * same id as the one provided. <br>
	 * If there is no shard with an id that matches the provided one, this will
	 * return {@code null}.
	 *
	 * @param id The id of the shard.
	 *
	 * @return The {@link net.dv8tion.jda.api.JDA JDA} instance with the given
	 *         shardId or {@code null} if no shard has the given id
	 */
	@Nullable
	default JDA getShardById(@NonNull final String id) {
		return this.getShardCache().getElementById(id);
	}

	/**
	 * Unified {@link ShardCacheView ShardCacheView} of all cached
	 * {@link net.dv8tion.jda.api.JDA JDA} bound to this ShardManager instance.
	 *
	 * @return Unified {@link ShardCacheView ShardCacheView}
	 */
	@NonNull
	ShardCacheView getShardCache();

	/**
	 * Gets all {@link net.dv8tion.jda.api.JDA JDA} instances bound to this
	 * ShardManager.
	 *
	 * <p>
	 * This copies the backing store into a list. This means every call creates a
	 * new list with O(n) complexity. It is recommended to store this into a local
	 * variable or use {@link #getShardCache()} and use its more efficient versions
	 * of handling these values.
	 *
	 * @return An immutable list of all managed {@link net.dv8tion.jda.api.JDA JDA}
	 *         instances.
	 */
	@NonNull
	default List<JDA> getShards() {
		return this.getShardCache().asList();
	}

	/**
	 * This returns the {@link net.dv8tion.jda.api.JDA.Status JDA.Status} of the
	 * shard which has the same id as the one provided. <br>
	 * If there is no shard with an id that matches the provided one, this will
	 * return {@code null}.
	 *
	 * @param shardId The id of the shard.
	 *
	 * @return The {@link net.dv8tion.jda.api.JDA.Status JDA.Status} of the shard
	 *         with the given shardId or {@code null} if no shard has the given id
	 */
	@Nullable
	default JDA.Status getStatus(final int shardId) {
		final JDA jda = this.getShardCache().getElementById(shardId);
		return jda == null ? null : jda.getStatus();
	}

	/**
	 * Gets the current {@link net.dv8tion.jda.api.JDA.Status Status} of all shards.
	 *
	 * @return All current shard statuses.
	 */
	@NonNull
	default Map<JDA, Status> getStatuses() {
		return Collections.unmodifiableMap(
				this.getShardCache().stream().collect(Collectors.toMap(Function.identity(), JDA::getStatus)));
	}

	/**
	 * {@link net.dv8tion.jda.api.utils.cache.SnowflakeCacheView SnowflakeCacheView}
	 * of all cached {@link net.dv8tion.jda.api.entities.User Users} visible to this
	 * ShardManager instance.
	 *
	 * @return {@link net.dv8tion.jda.api.utils.cache.SnowflakeCacheView
	 *         SnowflakeCacheView}
	 */
	@NonNull
	@Override
	default SnowflakeCacheView<User> getUserCache() {
		return CacheView.allSnowflakes(() -> this.getShardCache().stream().map(JDA::getUserCache));
	}

	/**
	 * Sets the {@link net.dv8tion.jda.api.entities.Activity Activity} for all
	 * shards. <br>
	 * An Activity can be retrieved via
	 * {@link net.dv8tion.jda.api.entities.Activity#playing(String)}. For streams
	 * you provide a valid streaming url as second parameter.
	 *
	 * <p>
	 * This will also change the activity for shards that are created in the future.
	 *
	 * @param activity A {@link net.dv8tion.jda.api.entities.Activity Activity}
	 *                 instance or null to reset
	 *
	 * @see net.dv8tion.jda.api.entities.Activity#playing(String)
	 * @see net.dv8tion.jda.api.entities.Activity#streaming(String, String)
	 */
	@Override
	default void setActivity(@Nullable final Activity activity) {
		this.setActivityProvider(id -> activity);
	}

	/**
	 * Sets provider that provider the {@link net.dv8tion.jda.api.entities.Activity
	 * Activity} for all shards. <br>
	 * A Activity can be retrieved via
	 * {@link net.dv8tion.jda.api.entities.Activity#playing(String)}. For streams
	 * you provide a valid streaming url as second parameter.
	 *
	 * <p>
	 * This will also change the provider for shards that are created in the future.
	 *
	 * @param activityProvider Provider for an
	 *                         {@link net.dv8tion.jda.api.entities.Activity
	 *                         Activity} instance or null to reset
	 *
	 * @see net.dv8tion.jda.api.entities.Activity#playing(String)
	 * @see net.dv8tion.jda.api.entities.Activity#streaming(String, String)
	 */
	default void setActivityProvider(@Nullable final IntFunction<? extends Activity> activityProvider) {
		this.getShardCache().forEach(jda -> jda.getPresence().setActivity(
				activityProvider == null ? null : activityProvider.apply(jda.getShardInfo().getShardId())));
	}

	/**
	 * Sets whether all instances should be marked as afk or not
	 *
	 * <p>
	 * This is relevant to client accounts to monitor whether new messages should
	 * trigger mobile push-notifications.
	 *
	 * <p>
	 * This will also change the value for shards that are created in the future.
	 *
	 * @param idle boolean
	 */
	@Override
	default void setIdle(final boolean idle) {
		this.setIdleProvider(id -> idle);
	}

	/**
	 * Sets the provider that decides for all shards whether they should be marked
	 * as afk or not.
	 *
	 * <p>
	 * This will also change the provider for shards that are created in the future.
	 *
	 * @param idleProvider Provider for a boolean
	 */
	default void setIdleProvider(@NonNull final IntFunction<Boolean> idleProvider) {
		this.getShardCache()
				.forEach(jda -> jda.getPresence().setIdle(idleProvider.apply(jda.getShardInfo().getShardId())));
	}

	/**
	 * Sets the {@link net.dv8tion.jda.api.OnlineStatus OnlineStatus} and
	 * {@link net.dv8tion.jda.api.entities.Activity Activity} for all shards.
	 *
	 * <p>
	 * This will also change the status for shards that are created in the future.
	 *
	 * @param status   The {@link net.dv8tion.jda.api.OnlineStatus OnlineStatus} to
	 *                 be used (OFFLINE/null {@literal ->} INVISIBLE)
	 * @param activity A {@link net.dv8tion.jda.api.entities.Activity Activity}
	 *                 instance or null to reset
	 *
	 * @throws java.lang.IllegalArgumentException If the provided OnlineStatus is
	 *                                            {@link net.dv8tion.jda.api.OnlineStatus#UNKNOWN
	 *                                            UNKNOWN}
	 *
	 * @see net.dv8tion.jda.api.entities.Activity#playing(String)
	 * @see net.dv8tion.jda.api.entities.Activity#streaming(String, String)
	 */
	@Override
	default void setPresence(@Nullable final OnlineStatus status, @Nullable final Activity activity) {
		this.setPresenceProvider(id -> status, id -> activity);
	}

	/**
	 * Sets the provider that provides the {@link net.dv8tion.jda.api.OnlineStatus
	 * OnlineStatus} and {@link net.dv8tion.jda.api.entities.Activity Activity} for
	 * all shards.
	 *
	 * <p>
	 * This will also change the status for shards that are created in the future.
	 *
	 * @param statusProvider   The {@link net.dv8tion.jda.api.OnlineStatus
	 *                         OnlineStatus} to be used (OFFLINE/null {@literal ->}
	 *                         INVISIBLE)
	 * @param activityProvider A {@link net.dv8tion.jda.api.entities.Activity
	 *                         Activity} instance or null to reset
	 *
	 * @throws java.lang.IllegalArgumentException If the provided OnlineStatus is
	 *                                            {@link net.dv8tion.jda.api.OnlineStatus#UNKNOWN
	 *                                            UNKNOWN}
	 *
	 * @see net.dv8tion.jda.api.entities.Activity#playing(String)
	 * @see net.dv8tion.jda.api.entities.Activity#streaming(String, String)
	 */
	default void setPresenceProvider(@Nullable final IntFunction<OnlineStatus> statusProvider,
			@Nullable final IntFunction<? extends Activity> activityProvider) {
		this.getShardCache()
				.forEach(jda -> jda.getPresence().setPresence(
						statusProvider == null ? null : statusProvider.apply(jda.getShardInfo().getShardId()),
						activityProvider == null ? null : activityProvider.apply(jda.getShardInfo().getShardId())));
	}

	/**
	 * Sets the {@link net.dv8tion.jda.api.OnlineStatus OnlineStatus} for all
	 * shards.
	 *
	 * <p>
	 * This will also change the status for shards that are created in the future.
	 *
	 * @param status The {@link net.dv8tion.jda.api.OnlineStatus OnlineStatus} to be
	 *               used (OFFLINE/null {@literal ->} INVISIBLE)
	 *
	 * @throws java.lang.IllegalArgumentException If the provided OnlineStatus is
	 *                                            {@link net.dv8tion.jda.api.OnlineStatus#UNKNOWN
	 *                                            UNKNOWN}
	 */
	@Override
	default void setStatus(@Nullable final OnlineStatus status) {
		this.setStatusProvider(id -> status);
	}

	/**
	 * Sets the provider that provides the {@link net.dv8tion.jda.api.OnlineStatus
	 * OnlineStatus} for all shards.
	 *
	 * <p>
	 * This will also change the provider for shards that are created in the future.
	 *
	 * @param statusProvider The {@link net.dv8tion.jda.api.OnlineStatus
	 *                       OnlineStatus} to be used (OFFLINE/null {@literal ->}
	 *                       INVISIBLE)
	 *
	 * @throws java.lang.IllegalArgumentException If the provided OnlineStatus is
	 *                                            {@link net.dv8tion.jda.api.OnlineStatus#UNKNOWN
	 *                                            UNKNOWN}
	 */
	default void setStatusProvider(@Nullable final IntFunction<OnlineStatus> statusProvider) {
		this.getShardCache().forEach(jda -> jda.getPresence()
				.setStatus(statusProvider == null ? null : statusProvider.apply(jda.getShardInfo().getShardId())));
	}

	@Override
	default RestAction<List<RoleConnectionMetadata>> retrieveRoleConnectionMetadata() {
		return anyShard().retrieveRoleConnectionMetadata();
	}

	@Override
	default RestAction<List<RoleConnectionMetadata>> updateRoleConnectionMetadata(
			Collection<? extends RoleConnectionMetadata> records) {
		return anyShard().updateRoleConnectionMetadata(records);
	}

	@Override
	default RestAction<Webhook> retrieveWebhookById(String webhookId) {
		return anyShard().retrieveWebhookById(getShardsQueued());
	}
	
	@Override
	default RestAction<List<Command>> retrieveCommands(boolean withLocalizations) {
		return anyShard().retrieveCommands(withLocalizations);
	}

	@Override
	default RestAction<Command> retrieveCommandById(String id) {
		return anyShard().retrieveCommandById(id);
	}

	@Override
	default RestAction<Command> upsertCommand(CommandData command) {
		return anyShard().upsertCommand(command);
	}

	@Override
	default CommandListUpdateAction updateCommands() {
		return anyShard().updateCommands();
	}

	@Override
	default CommandEditAction editCommandById(String id) {
		return anyShard().editCommandById(id);
	}

	@Override
	default RestAction<Void> deleteCommandById(String commandId) {
		return anyShard().deleteCommandById(getShardsQueued());
	}

	@Override
	default void setRequiredScopes(Collection<String> scopes) {
		this.getShardCache().forEach(jda -> jda.setRequiredScopes(scopes));
	}

	@Override
	default String getInviteUrl(Collection<Permission> permissions) {
		return anyShard().getInviteUrl(permissions);
	}

	/**
	 * Get a random shard.
	 * 
	 * @return A random shard
	 * @throws IllegalStateException if there are no shards active
	 */
	private JDA anyShard() {
		return this.getShardCache().stream().findAny().orElseThrow(() -> new IllegalStateException("no active shards"));
	}
}
