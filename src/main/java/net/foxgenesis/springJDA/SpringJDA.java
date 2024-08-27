package net.foxgenesis.springJDA;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.regex.Matcher;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import net.dv8tion.jda.annotations.Incubating;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
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
import net.dv8tion.jda.api.entities.channel.attribute.IGuildChannelContainer;
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
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.emoji.RichCustomEmoji;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.hooks.IEventManager;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.CommandCreateAction;
import net.dv8tion.jda.api.requests.restaction.CommandEditAction;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.utils.MiscUtil;
import net.dv8tion.jda.api.utils.Once;
import net.dv8tion.jda.api.utils.cache.ChannelCacheView;
import net.dv8tion.jda.api.utils.cache.SnowflakeCacheView;
import net.dv8tion.jda.internal.interactions.CommandDataImpl;
import net.dv8tion.jda.internal.utils.Checks;
import net.dv8tion.jda.internal.utils.Helpers;

/**
 * Interface containing common proxy methods for interacting with a wrapped
 * {@link JDA} or {@link ShardManager}.
 * <p>
 * Further methods of {@link JDA} or {@link ShardManager} can be obtained
 * through {@link SingleSpringJDA} or {@link ShardedSpringJDA} instance
 * respectively.
 * 
 * @see SingleSpringJDA
 * @see ShardedSpringJDA
 * @author Ashley
 */
public interface SpringJDA extends IGuildChannelContainer<Channel> {

	static String SPRING_JDA = "spring-jda";

	/**
	 * Adds all provided listeners to the event-listeners that will be used to
	 * handle events.
	 *
	 * <p>
	 * Note: when using the {@link net.dv8tion.jda.api.hooks.InterfacedEventManager
	 * InterfacedEventListener} (), the given listener <b>must</b> be an instance of
	 * {@link net.dv8tion.jda.api.hooks.EventListener EventListener}!
	 *
	 * @param listeners The listener(s) which will react to events.
	 *
	 * @throws java.lang.IllegalArgumentException If either listeners or one of it's
	 *                                            objects is {@code null}.
	 */
	void addEventListener(@NonNull final Object... listeners);

	/**
	 * Removes all provided listeners from the event-listeners and no longer uses
	 * them to handle events.
	 *
	 * @param listeners The listener(s) to be removed.
	 *
	 * @throws java.lang.IllegalArgumentException If either listeners or one of it's
	 *                                            objects is {@code null}.
	 */
	void removeEventListener(@NonNull final Object... listeners);

	/**
	 * The {@link GatewayIntent GatewayIntents} for the JDA sessions of this shard
	 * manager.
	 *
	 * @return {@link EnumSet} of active gateway intents
	 */
	EnumSet<GatewayIntent> getGatewayIntents();

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
	RestAction<ApplicationInfo> retrieveApplicationInfo();

	/**
	 * Retrieves a custom emoji matching the specified {@code id} if one is
	 * available in our cache.
	 *
	 * <p>
	 * <b>Unicode emojis are not included as {@link RichCustomEmoji}!</b>
	 *
	 * @param id The id of the requested {@link RichCustomEmoji}.
	 *
	 * @return An {@link RichCustomEmoji} represented by this id or null if none is
	 *         found in our cache.
	 */
	@Nullable
	default RichCustomEmoji getEmojiById(final long id) {
		return this.getEmojiCache().getElementById(id);
	}

	/**
	 * Retrieves a custom emoji matching the specified {@code id} if one is
	 * available in our cache.
	 *
	 * <p>
	 * <b>Unicode emojis are not included as {@link RichCustomEmoji}!</b>
	 *
	 * @param id The id of the requested {@link RichCustomEmoji}.
	 *
	 * @throws java.lang.NumberFormatException If the provided {@code id} cannot be
	 *                                         parsed by
	 *                                         {@link Long#parseLong(String)}
	 *
	 * @return An {@link RichCustomEmoji} represented by this id or null if none is
	 *         found in our cache.
	 */
	@Nullable
	default RichCustomEmoji getEmojiById(@NonNull final String id) {
		return this.getEmojiCache().getElementById(id);
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
	SnowflakeCacheView<RichCustomEmoji> getEmojiCache();

	/**
	 * A collection of all known custom emojis (managed/restricted included).
	 *
	 * <p>
	 * <b>Hint</b>: To check whether you can use a {@link RichCustomEmoji} in a
	 * specific context you can use
	 * {@link RichCustomEmoji#canInteract(net.dv8tion.jda.api.entities.Member)} or
	 * {@link RichCustomEmoji#canInteract(net.dv8tion.jda.api.entities.User, MessageChannel)}
	 *
	 * <p>
	 * <b>Unicode emojis are not included as {@link RichCustomEmoji}!</b>
	 *
	 * <p>
	 * This copies the backing store into a list. This means every call creates a
	 * new list with O(n) complexity. It is recommended to store this into a local
	 * variable or use {@link #getEmojiCache()} and use its more efficient versions
	 * of handling these values.
	 *
	 * @return An immutable list of custom emojis (which may or may not be available
	 *         to usage).
	 */
	@NonNull
	default List<RichCustomEmoji> getEmojis() {
		return this.getEmojiCache().asList();
	}

	/**
	 * An unmodifiable list of all {@link RichCustomEmoji RichCustomEmojis} that
	 * have the same name as the one provided. <br>
	 * If there are no {@link RichCustomEmoji RichCustomEmojis} with the provided
	 * name, this will return an empty list.
	 *
	 * <p>
	 * <b>Unicode emojis are not included as {@link RichCustomEmoji}!</b>
	 *
	 * @param name       The name of the requested {@link RichCustomEmoji
	 *                   RichCustomEmojis}. Without colons.
	 * @param ignoreCase Whether to ignore case or not when comparing the provided
	 *                   name to each {@link RichCustomEmoji#getName()}.
	 *
	 * @return Possibly-empty list of all the {@link RichCustomEmoji
	 *         RichCustomEmojis} that all have the same name as the provided name.
	 */
	@NonNull
	default List<RichCustomEmoji> getEmojisByName(@NonNull final String name, final boolean ignoreCase) {
		return this.getEmojiCache().getElementsByName(name, ignoreCase);
	}

	/**
	 * This returns the {@link net.dv8tion.jda.api.entities.Guild Guild} which has
	 * the same id as the one provided. <br>
	 * If there is no connected guild with an id that matches the provided one, this
	 * will return {@code null}.
	 *
	 * @param id The id of the {@link net.dv8tion.jda.api.entities.Guild Guild}.
	 *
	 * @return Possibly-null {@link net.dv8tion.jda.api.entities.Guild Guild} with
	 *         matching id.
	 */
	@Nullable
	default Guild getGuildById(final long id) {
		return getGuildCache().getElementById(id);
	}

	/**
	 * This returns the {@link net.dv8tion.jda.api.entities.Guild Guild} which has
	 * the same id as the one provided. <br>
	 * If there is no connected guild with an id that matches the provided one, this
	 * will return {@code null}.
	 *
	 * @param id The id of the {@link net.dv8tion.jda.api.entities.Guild Guild}.
	 *
	 * @return Possibly-null {@link net.dv8tion.jda.api.entities.Guild Guild} with
	 *         matching id.
	 */
	@Nullable
	default Guild getGuildById(@NonNull final String id) {
		return getGuildById(MiscUtil.parseSnowflake(id));
	}

	/**
	 * An unmodifiable list of all {@link net.dv8tion.jda.api.entities.Guild Guilds}
	 * that have the same name as the one provided. <br>
	 * If there are no {@link net.dv8tion.jda.api.entities.Guild Guilds} with the
	 * provided name, this will return an empty list.
	 *
	 * @param name       The name of the requested
	 *                   {@link net.dv8tion.jda.api.entities.Guild Guilds}.
	 * @param ignoreCase Whether to ignore case or not when comparing the provided
	 *                   name to each
	 *                   {@link net.dv8tion.jda.api.entities.Guild#getName()}.
	 *
	 * @return Possibly-empty list of all the
	 *         {@link net.dv8tion.jda.api.entities.Guild Guilds} that all have the
	 *         same name as the provided name.
	 */
	@NonNull
	default List<Guild> getGuildsByName(@NonNull final String name, final boolean ignoreCase) {
		return this.getGuildCache().getElementsByName(name, ignoreCase);
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
	SnowflakeCacheView<Guild> getGuildCache();

	/**
	 * An unmodifiable List of all {@link net.dv8tion.jda.api.entities.Guild Guilds}
	 * that the logged account is connected to. <br>
	 * If this account is not connected to any
	 * {@link net.dv8tion.jda.api.entities.Guild Guilds}, this will return an empty
	 * list.
	 *
	 * <p>
	 * This copies the backing store into a list. This means every call creates a
	 * new list with O(n) complexity. It is recommended to store this into a local
	 * variable or use {@link #getGuildCache()} and use its more efficient versions
	 * of handling these values.
	 *
	 * @return Possibly-empty list of all the
	 *         {@link net.dv8tion.jda.api.entities.Guild Guilds} that this account
	 *         is connected to.
	 */
	@NonNull
	default List<Guild> getGuilds() {
		return this.getGuildCache().asList();
	}

	/**
	 * Gets all {@link net.dv8tion.jda.api.entities.Guild Guilds} that contain all
	 * given users as their members.
	 *
	 * @param users The users which all the returned
	 *              {@link net.dv8tion.jda.api.entities.Guild Guilds} must contain.
	 *
	 * @return Unmodifiable list of all {@link net.dv8tion.jda.api.entities.Guild
	 *         Guild} instances which have all
	 *         {@link net.dv8tion.jda.api.entities.User Users} in them.
	 */
	@NonNull
	default List<Guild> getMutualGuilds(@NonNull final Collection<User> users) {
		Checks.noneNull(users, "users");
		return this.getGuildCache().stream().filter(guild -> users.stream().allMatch(guild::isMember))
				.collect(Helpers.toUnmodifiableList());
	}

	/**
	 * Gets all {@link net.dv8tion.jda.api.entities.Guild Guilds} that contain all
	 * given users as their members.
	 *
	 * @param users The users which all the returned
	 *              {@link net.dv8tion.jda.api.entities.Guild Guilds} must contain.
	 *
	 * @return Unmodifiable list of all {@link net.dv8tion.jda.api.entities.Guild
	 *         Guild} instances which have all
	 *         {@link net.dv8tion.jda.api.entities.User Users} in them.
	 */
	@NonNull
	default List<Guild> getMutualGuilds(@NonNull final User... users) {
		Checks.notNull(users, "users");
		return this.getMutualGuilds(Arrays.asList(users));
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
	 * @throws java.lang.IllegalArgumentException If the provided id String is not a
	 *                                            valid snowflake.
	 * @throws java.lang.IllegalStateException    If there isn't any active shards.
	 *
	 * @return {@link net.dv8tion.jda.api.requests.RestAction RestAction} - Type:
	 *         {@link net.dv8tion.jda.api.entities.User User} <br>
	 *         On request, gets the User with id matching provided id from Discord.
	 */
	@NonNull
	default RestAction<User> retrieveUserById(@NonNull String id) {
		return retrieveUserById(MiscUtil.parseSnowflake(id));
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
	RestAction<User> retrieveUserById(long id);

	/**
	 * Finds and collects all {@link net.dv8tion.jda.api.entities.Guild Guild}
	 * instances that contain the specified within the current
	 * {@link net.dv8tion.jda.api.JDA JDA} instance.<br>
	 * <p>
	 * This method is a shortcut for
	 * {@link net.dv8tion.jda.api.JDA#getMutualGuilds(User...)
	 * JDA.getMutualGuilds(User)}.
	 * </p>
	 *
	 * @return Immutable list of all {@link net.dv8tion.jda.api.entities.Guild
	 *         Guilds} that this user is a member of.
	 */
	default RestAction<List<Guild>> getMutualGuilds(long userId) {
		return retrieveUserById(userId).map(User::getMutualGuilds);
	}

	/**
	 * Finds and collects all {@link net.dv8tion.jda.api.entities.Guild Guild}
	 * instances that are owned by the specified user within the current
	 * {@link net.dv8tion.jda.api.JDA JDA} instance.
	 *
	 * @return Immutable list of all {@link net.dv8tion.jda.api.entities.Guild
	 *         Guilds} that this user owns and is a member of.
	 */
	default RestAction<List<Guild>> getOwnedMutualGuilds(long userId) {
		return getMutualGuilds(userId)
				.map(guilds -> guilds.stream().filter(guild -> guild.getOwnerIdLong() == userId).toList());
	}

	/**
	 * Searches for a user that has the matching Discord Tag. <br>
	 * Format has to be in the form {@code Username#Discriminator} where the
	 * username must be between 2 and 32 characters (inclusive) matching the exact
	 * casing and the discriminator must be exactly 4 digits.
	 *
	 * <p>
	 * This only checks users that are known to the currently logged in account
	 * (shard). If a user exists with the tag that is not available in the
	 * {@link #getUserCache() User-Cache} it will not be detected. <br>
	 * Currently Discord does not offer a way to retrieve a user by their discord
	 * tag.
	 *
	 * <p>
	 * <b>This will only check cached users!</b>
	 *
	 * <p>
	 * To check users without discriminators, use {@code username#0000} instead.
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
	default User getUserByTag(@NonNull String tag) {
		Checks.notNull(tag, "Tag");
		Matcher matcher = User.USER_TAG.matcher(tag);
		Checks.check(matcher.matches(), "Invalid tag format!");
		String username = matcher.group(1);
		String discriminator = matcher.group(2);
		return getUserByTag(username, discriminator);
	}

	/**
	 * Searches for a user that has the matching Discord Tag. <br>
	 * Format has to be in the form {@code Username#Discriminator} where the
	 * username must be between 2 and 32 characters (inclusive) matching the exact
	 * casing and the discriminator must be exactly 4 digits.
	 *
	 * <p>
	 * This only checks users that are known to the currently logged in account
	 * (shard). If a user exists with the tag that is not available in the
	 * {@link #getUserCache() User-Cache} it will not be detected. <br>
	 * Currently Discord does not offer a way to retrieve a user by their discord
	 * tag.
	 *
	 * <p>
	 * <b>This will only check cached users!</b>
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
	@Incubating
	default User getUserByTag(@NonNull String username, @NonNull String discriminator) {
		Checks.notNull(username, "Username");
		Checks.notNull(discriminator, "Discriminator");
		Checks.check(discriminator.length() == 4 && Helpers.isNumeric(discriminator),
				"Invalid format for discriminator!");
		int codePointLength = Helpers.codePointLength(username);
		Checks.check(codePointLength >= 2 && codePointLength <= 32,
				"Username must be between 2 and 32 codepoints in length!");
		return getUserCache().applyStream(stream -> stream.filter(it -> it.getDiscriminator().equals(discriminator))
				.filter(it -> it.getName().equals(username)).findFirst().orElse(null));
	}

	/**
	 * An unmodifiable list of all known
	 * {@link net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel
	 * PrivateChannels}.
	 *
	 * <p>
	 * This copies the backing store into a list. This means every call creates a
	 * new list with O(n) complexity. It is recommended to store this into a local
	 * variable or use {@link #getPrivateChannelCache()} and use its more efficient
	 * versions of handling these values.
	 *
	 * @return Possibly-empty list of all
	 *         {@link net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel
	 *         PrivateChannels}.
	 */
	@NonNull
	default List<PrivateChannel> getPrivateChannels() {
		return this.getPrivateChannelCache().asList();
	}

	/**
	 * Retrieves the {@link net.dv8tion.jda.api.entities.Role Role} associated to
	 * the provided id. <br>
	 * This iterates over all {@link net.dv8tion.jda.api.entities.Guild Guilds} and
	 * check whether a Role from that Guild is assigned to the specified ID and will
	 * return the first that can be found.
	 *
	 * @param id The id of the searched Role
	 *
	 * @return Possibly-null {@link net.dv8tion.jda.api.entities.Role Role} for the
	 *         specified ID
	 */
	@Nullable
	default Role getRoleById(final long id) {
		return this.getRoleCache().getElementById(id);
	}

	/**
	 * Retrieves the {@link net.dv8tion.jda.api.entities.Role Role} associated to
	 * the provided id. <br>
	 * This iterates over all {@link net.dv8tion.jda.api.entities.Guild Guilds} and
	 * check whether a Role from that Guild is assigned to the specified ID and will
	 * return the first that can be found.
	 *
	 * @param id The id of the searched Role
	 *
	 * @throws java.lang.NumberFormatException If the provided {@code id} cannot be
	 *                                         parsed by
	 *                                         {@link Long#parseLong(String)}
	 *
	 * @return Possibly-null {@link net.dv8tion.jda.api.entities.Role Role} for the
	 *         specified ID
	 */
	@Nullable
	default Role getRoleById(@NonNull final String id) {
		return this.getRoleCache().getElementById(id);
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
	SnowflakeCacheView<Role> getRoleCache();

	/**
	 * All {@link net.dv8tion.jda.api.entities.Role Roles} this ShardManager
	 * instance can see. <br>
	 * This will iterate over each {@link net.dv8tion.jda.api.entities.Guild Guild}
	 * retrieved from {@link #getGuilds()} and collect its
	 * {@link net.dv8tion.jda.api.entities.Guild#getRoles() Guild.getRoles()}.
	 *
	 * <p>
	 * This copies the backing store into a list. This means every call creates a
	 * new list with O(n) complexity. It is recommended to store this into a local
	 * variable or use {@link #getRoleCache()} and use its more efficient versions
	 * of handling these values.
	 *
	 * @return Immutable List of all visible Roles
	 */
	@NonNull
	default List<Role> getRoles() {
		return this.getRoleCache().asList();
	}

	/**
	 * Retrieves all {@link net.dv8tion.jda.api.entities.Role Roles} visible to this
	 * ShardManager instance. <br>
	 * This simply filters the Roles returned by {@link #getRoles()} with the
	 * provided name, either using {@link String#equals(Object)} or
	 * {@link String#equalsIgnoreCase(String)} on
	 * {@link net.dv8tion.jda.api.entities.Role#getName()}.
	 *
	 * @param name       The name for the Roles
	 * @param ignoreCase Whether to use {@link String#equalsIgnoreCase(String)}
	 *
	 * @return Immutable List of all Roles matching the parameters provided.
	 */
	@NonNull
	default List<Role> getRolesByName(@NonNull final String name, final boolean ignoreCase) {
		return this.getRoleCache().getElementsByName(name, ignoreCase);
	}

	/**
	 * This returns the
	 * {@link net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel
	 * PrivateChannel} which has the same id as the one provided. <br>
	 * If there is no known
	 * {@link net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel
	 * PrivateChannel} with an id that matches the provided one, then this will
	 * return {@code null}.
	 *
	 * @param id The id of the
	 *           {@link net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel
	 *           PrivateChannel}.
	 *
	 * @return Possibly-null
	 *         {@link net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel
	 *         PrivateChannel} with matching id.
	 */
	@Nullable
	default PrivateChannel getPrivateChannelById(final long id) {
		return this.getPrivateChannelCache().getElementById(id);
	}

	/**
	 * This returns the
	 * {@link net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel
	 * PrivateChannel} which has the same id as the one provided. <br>
	 * If there is no known
	 * {@link net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel
	 * PrivateChannel} with an id that matches the provided one, this will return
	 * {@code null}.
	 *
	 * @param id The id of the
	 *           {@link net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel
	 *           PrivateChannel}.
	 *
	 * @throws java.lang.NumberFormatException If the provided {@code id} cannot be
	 *                                         parsed by
	 *                                         {@link Long#parseLong(String)}
	 *
	 * @return Possibly-null
	 *         {@link net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel
	 *         PrivateChannel} with matching id.
	 */
	@Nullable
	default PrivateChannel getPrivateChannelById(@NonNull final String id) {
		return this.getPrivateChannelCache().getElementById(id);
	}

	@Nullable
	@Override
	default GuildChannel getGuildChannelById(long id) {
		return getChannelCache().ofType(GuildChannel.class).getElementById(id);
	}

	@Nullable
	@Override
	default GuildChannel getGuildChannelById(@NonNull ChannelType type, long id) {
		Channel channel = getChannelCache().getElementById(type, id);
		return channel instanceof GuildChannel ? (GuildChannel) channel : null;
	}

	@NonNull
	@Override
	default SnowflakeCacheView<Category> getCategoryCache() {
		return getChannelCache().ofType(Category.class);
	}

	@NonNull
	@Override
	default SnowflakeCacheView<TextChannel> getTextChannelCache() {
		return getChannelCache().ofType(TextChannel.class);
	}

	@NonNull
	@Override
	default SnowflakeCacheView<NewsChannel> getNewsChannelCache() {
		return getChannelCache().ofType(NewsChannel.class);
	}

	@NonNull
	@Override
	default SnowflakeCacheView<VoiceChannel> getVoiceChannelCache() {
		return getChannelCache().ofType(VoiceChannel.class);
	}

	@NonNull
	@Override
	default SnowflakeCacheView<StageChannel> getStageChannelCache() {
		return getChannelCache().ofType(StageChannel.class);
	}

	@NonNull
	@Override
	default SnowflakeCacheView<ThreadChannel> getThreadChannelCache() {
		return getChannelCache().ofType(ThreadChannel.class);
	}

	@NonNull
	@Override
	default SnowflakeCacheView<ForumChannel> getForumChannelCache() {
		return getChannelCache().ofType(ForumChannel.class);
	}

	@NonNull
	@Override
	default SnowflakeCacheView<MediaChannel> getMediaChannelCache() {
		return getChannelCache().ofType(MediaChannel.class);
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
	default SnowflakeCacheView<PrivateChannel> getPrivateChannelCache() {
		return getChannelCache().ofType(PrivateChannel.class);
	}

	@NonNull
	@Override
	ChannelCacheView<Channel> getChannelCache();

	/**
	 * Returns the currently logged in account represented by
	 * {@link net.dv8tion.jda.api.entities.SelfUser SelfUser}. <br>
	 * Account settings <b>cannot</b> be modified using this object. If you wish to
	 * modify account settings please use the AccountManager which is accessible by
	 * {@link net.dv8tion.jda.api.entities.SelfUser#getManager()}.
	 *
	 * @return The currently logged in account.
	 */
	User getSelfUser();

	/**
	 * This returns the {@link net.dv8tion.jda.api.entities.User User} which has the
	 * same id as the one provided. <br>
	 * If there is no visible user with an id that matches the provided one, this
	 * will return {@code null}.
	 *
	 * @param id The id of the requested {@link net.dv8tion.jda.api.entities.User
	 *           User}.
	 *
	 * @return Possibly-null {@link net.dv8tion.jda.api.entities.User User} with
	 *         matching id.
	 */
	@Nullable
	default User getUserById(final long id) {
		return this.getUserCache().getElementById(id);
	}

	/**
	 * This returns the {@link net.dv8tion.jda.api.entities.User User} which has the
	 * same id as the one provided. <br>
	 * If there is no visible user with an id that matches the provided one, this
	 * will return {@code null}.
	 *
	 * @param id The id of the requested {@link net.dv8tion.jda.api.entities.User
	 *           User}.
	 *
	 * @return Possibly-null {@link net.dv8tion.jda.api.entities.User User} with
	 *         matching id.
	 */
	@Nullable
	default User getUserById(@NonNull final String id) {
		return this.getUserCache().getElementById(id);
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
	SnowflakeCacheView<User> getUserCache();

	/**
	 * An unmodifiable list of all {@link net.dv8tion.jda.api.entities.User Users}
	 * that share a {@link net.dv8tion.jda.api.entities.Guild Guild} with the
	 * currently logged in account. <br>
	 * This list will never contain duplicates and represents all
	 * {@link net.dv8tion.jda.api.entities.User Users} that JDA can currently see.
	 *
	 * <p>
	 * If the developer is sharding, then only users from guilds connected to the
	 * specifically logged in shard will be returned in the List.
	 *
	 * <p>
	 * This copies the backing store into a list. This means every call creates a
	 * new list with O(n) complexity. It is recommended to store this into a local
	 * variable or use {@link #getUserCache()} and use its more efficient versions
	 * of handling these values.
	 *
	 * @return List of all {@link net.dv8tion.jda.api.entities.User Users} that are
	 *         visible to JDA.
	 */
	@NonNull
	default List<User> getUsers() {
		return this.getUserCache().asList();
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
	void setActivity(@Nullable final Activity activity);

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
	void setIdle(final boolean idle);

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
	void setPresence(@Nullable final OnlineStatus status, @Nullable final Activity activity);

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
	void setStatus(@Nullable final OnlineStatus status);

	/**
	 * Returns a reusable builder for a one-time event listener.
	 *
	 * <p>
	 * Note that this method only works if the
	 * {@link JDABuilder#setEventManager(IEventManager) event manager} is either the
	 * {@link net.dv8tion.jda.api.hooks.InterfacedEventManager
	 * InterfacedEventManager} or
	 * {@link net.dv8tion.jda.api.hooks.AnnotatedEventManager
	 * AnnotatedEventManager}. <br>
	 * Other implementations can support it as long as they call
	 * {@link net.dv8tion.jda.api.hooks.EventListener#onEvent(GenericEvent)
	 * EventListener.onEvent(GenericEvent)}.
	 *
	 * <p>
	 * <b>Example:</b>
	 *
	 * <p>
	 * Listening to a message from a channel and a user, after using a slash
	 * command:
	 * 
	 * <pre>{@code
	 * final Duration timeout = Duration.ofSeconds(5);
	 * event.reply("Reply in " + TimeFormat.RELATIVE.after(timeout) + " if you can!").setEphemeral(true).queue();
	 *
	 * event.getJDA().listenOnce(MessageReceivedEvent.class)
	 * 		.filter(messageEvent -> messageEvent.getChannel().getIdLong() == event.getChannel().getIdLong())
	 * 		.filter(messageEvent -> messageEvent.getAuthor().getIdLong() == event.getUser().getIdLong())
	 * 		.timeout(timeout, () -> {
	 * 			event.getHook().editOriginal("Timeout!").queue();
	 * 		}).subscribe(messageEvent -> {
	 * 			event.getHook().editOriginal("You sent: " + messageEvent.getMessage().getContentRaw()).queue();
	 * 		});
	 * }</pre>
	 *
	 * @param eventType Type of the event to listen to
	 *
	 * @throws IllegalArgumentException If the provided event type is {@code null}
	 *
	 * @return The one-time event listener builder
	 */
	@NonNull
	<E extends GenericEvent> Once.Builder<E> listenOnce(@NonNull Class<E> eventType);

	/**
	 * Retrieves the list of global commands. <br>
	 * This list does not include guild commands! Use
	 * {@link Guild#retrieveCommands()} for guild commands. <br>
	 * This list does not include localization data. Use
	 * {@link #retrieveCommands(boolean)} to get localization data
	 *
	 * @return {@link RestAction} - Type: {@link List} of {@link Command}
	 */
	@NonNull
	default RestAction<List<Command>> retrieveCommands() {
		return retrieveCommands(false);
	}

	/**
	 * Retrieves the list of global commands. <br>
	 * This list does not include guild commands! Use
	 * {@link Guild#retrieveCommands()} for guild commands.
	 *
	 * @param withLocalizations {@code true} if the localization data (such as name
	 *                          and description) should be included
	 *
	 * @return {@link RestAction} - Type: {@link List} of {@link Command}
	 */
	@NonNull
	RestAction<List<Command>> retrieveCommands(boolean withLocalizations);

	/**
	 * Retrieves the existing {@link Command} instance by id.
	 *
	 * <p>
	 * If there is no command with the provided ID, this RestAction fails with
	 * {@link net.dv8tion.jda.api.requests.ErrorResponse#UNKNOWN_COMMAND
	 * ErrorResponse.UNKNOWN_COMMAND}
	 *
	 * @param id The command id
	 *
	 * @throws IllegalArgumentException If the provided id is not a valid snowflake
	 *
	 * @return {@link RestAction} - Type: {@link Command}
	 */
	@NonNull
	RestAction<Command> retrieveCommandById(@NonNull String id);

	/**
	 * Retrieves the existing {@link Command} instance by id.
	 *
	 * <p>
	 * If there is no command with the provided ID, this RestAction fails with
	 * {@link net.dv8tion.jda.api.requests.ErrorResponse#UNKNOWN_COMMAND
	 * ErrorResponse.UNKNOWN_COMMAND}
	 *
	 * @param id The command id
	 *
	 * @return {@link RestAction} - Type: {@link Command}
	 */
	@NonNull
	default RestAction<Command> retrieveCommandById(long id) {
		return retrieveCommandById(Long.toUnsignedString(id));
	}

	/**
	 * Creates or updates a global command. <br>
	 * If a command with the same name exists, it will be replaced. This operation
	 * is idempotent. Commands will persist between restarts of your bot, you only
	 * have to create a command once.
	 *
	 * <p>
	 * To specify a complete list of all commands you can use
	 * {@link #updateCommands()} instead.
	 *
	 * <p>
	 * You need the OAuth2 scope {@code "applications.commands"} in order to add
	 * commands to a guild.
	 *
	 * @param command The {@link CommandData} for the command
	 *
	 * @throws IllegalArgumentException If null is provided
	 *
	 * @return {@link RestAction} - Type: {@link Command} <br>
	 *         The RestAction used to create or update the command
	 *
	 * @see Commands#slash(String, String) Commands.slash(...)
	 * @see Commands#message(String) Commands.message(...)
	 * @see Commands#user(String) Commands.user(...)
	 * @see Guild#upsertCommand(CommandData) Guild.upsertCommand(...)
	 */
	@NonNull
	RestAction<Command> upsertCommand(@NonNull CommandData command);

	/**
	 * Creates or updates a global slash command. <br>
	 * If a command with the same name exists, it will be replaced. This operation
	 * is idempotent. Commands will persist between restarts of your bot, you only
	 * have to create a command once.
	 *
	 * <p>
	 * To specify a complete list of all commands you can use
	 * {@link #updateCommands()} instead.
	 *
	 * <p>
	 * You need the OAuth2 scope {@code "applications.commands"} in order to add
	 * commands to a guild.
	 *
	 * @param name        The lowercase alphanumeric (with dash) name, 1-32
	 *                    characters
	 * @param description The description for the command, 1-100 characters
	 *
	 * @throws IllegalArgumentException If null is provided or the name/description
	 *                                  do not meet the requirements
	 *
	 * @return {@link CommandCreateAction}
	 *
	 * @see Guild#upsertCommand(String, String)
	 */
	@NonNull
	default CommandCreateAction upsertCommand(@NonNull String name, @NonNull String description) {
		return (CommandCreateAction) upsertCommand(new CommandDataImpl(name, description));
	}

	/**
	 * Configures the complete list of global commands. <br>
	 * This will replace the existing command list for this bot. You should only use
	 * this once on startup!
	 *
	 * <p>
	 * This operation is idempotent. Commands will persist between restarts of your
	 * bot, you only have to create a command once.
	 *
	 * <p>
	 * You need the OAuth2 scope {@code "applications.commands"} in order to add
	 * commands to a guild.
	 *
	 * <p>
	 * <b>Examples</b>
	 *
	 * <p>
	 * Set list to 2 commands:
	 * 
	 * <pre>{@code
	 * jda.updateCommands().addCommands(Commands.slash("ping", "Gives the current ping"))
	 * 		.addCommands(Commands.slash("ban", "Ban the target user").setGuildOnly(true)
	 * 				.setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.BAN_MEMBERS))
	 * 				.addOption(OptionType.USER, "user", "The user to ban", true))
	 * 		.queue();
	 * }</pre>
	 *
	 * <p>
	 * Delete all commands:
	 * 
	 * <pre>{@code
	 * jda.updateCommands().queue();
	 * }</pre>
	 *
	 * @return {@link CommandListUpdateAction}
	 *
	 * @see Guild#updateCommands()
	 */
	@NonNull
	CommandListUpdateAction updateCommands();

	/**
	 * Edit an existing global command by id.
	 *
	 * <p>
	 * If there is no command with the provided ID, this RestAction fails with
	 * {@link net.dv8tion.jda.api.requests.ErrorResponse#UNKNOWN_COMMAND
	 * ErrorResponse.UNKNOWN_COMMAND}
	 *
	 * @param id The id of the command to edit
	 *
	 * @throws IllegalArgumentException If the provided id is not a valid snowflake
	 *
	 * @return {@link CommandEditAction} used to edit the command
	 */
	@NonNull
	CommandEditAction editCommandById(@NonNull String id);

	/**
	 * Edit an existing global command by id.
	 *
	 * <p>
	 * If there is no command with the provided ID, this RestAction fails with
	 * {@link net.dv8tion.jda.api.requests.ErrorResponse#UNKNOWN_COMMAND
	 * ErrorResponse.UNKNOWN_COMMAND}
	 *
	 * @param id The id of the command to edit
	 *
	 * @return {@link CommandEditAction} used to edit the command
	 */
	@NonNull
	default CommandEditAction editCommandById(long id) {
		return editCommandById(Long.toUnsignedString(id));
	}

	/**
	 * Delete the global command for this id.
	 *
	 * <p>
	 * If there is no command with the provided ID, this RestAction fails with
	 * {@link net.dv8tion.jda.api.requests.ErrorResponse#UNKNOWN_COMMAND
	 * ErrorResponse.UNKNOWN_COMMAND}
	 *
	 * @param commandId The id of the command that should be deleted
	 *
	 * @throws IllegalArgumentException If the provided id is not a valid snowflake
	 *
	 * @return {@link RestAction}
	 */
	@NonNull
	RestAction<Void> deleteCommandById(@NonNull String commandId);

	/**
	 * Delete the global command for this id.
	 *
	 * <p>
	 * If there is no command with the provided ID, this RestAction fails with
	 * {@link net.dv8tion.jda.api.requests.ErrorResponse#UNKNOWN_COMMAND
	 * ErrorResponse.UNKNOWN_COMMAND}
	 *
	 * @param commandId The id of the command that should be deleted
	 *
	 * @return {@link RestAction}
	 */
	@NonNull
	default RestAction<Void> deleteCommandById(long commandId) {
		return deleteCommandById(Long.toUnsignedString(commandId));
	}

	/**
	 * Retrieves the currently configured {@link RoleConnectionMetadata} records for
	 * this application.
	 *
	 * @return {@link RestAction} - Type: {@link List} of
	 *         {@link RoleConnectionMetadata}
	 *
	 * @see <a href=
	 *      "https://discord.com/developers/docs/tutorials/configuring-app-metadata-for-linked-roles"
	 *      target="_blank">Configuring App Metadata for Linked Roles</a>
	 */
	@NonNull
	RestAction<List<RoleConnectionMetadata>> retrieveRoleConnectionMetadata();

	/**
	 * Updates the currently configured {@link RoleConnectionMetadata} records for
	 * this application.
	 *
	 * <p>
	 * Returns the updated connection metadata records on success.
	 *
	 * @param records The new records to set
	 *
	 * @throws IllegalArgumentException If null is provided or more than
	 *                                  {@value RoleConnectionMetadata#MAX_RECORDS}
	 *                                  records are configured.
	 *
	 * @return {@link RestAction} - Type: {@link List} of
	 *         {@link RoleConnectionMetadata}
	 *
	 * @see <a href=
	 *      "https://discord.com/developers/docs/tutorials/configuring-app-metadata-for-linked-roles"
	 *      target="_blank">Configuring App Metadata for Linked Roles</a>
	 */
	@NonNull
	RestAction<List<RoleConnectionMetadata>> updateRoleConnectionMetadata(
			@NonNull Collection<? extends RoleConnectionMetadata> records);

	/**
	 * Retrieves a {@link net.dv8tion.jda.api.entities.Webhook Webhook} by its id.
	 * <br>
	 * If the webhook does not belong to any known guild of this JDA session, it
	 * will be {@link Webhook#isPartial() partial}.
	 *
	 * <p>
	 * Possible {@link net.dv8tion.jda.api.requests.ErrorResponse ErrorResponses}
	 * caused by the returned {@link net.dv8tion.jda.api.requests.RestAction
	 * RestAction} include the following:
	 * <ul>
	 * <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#MISSING_PERMISSIONS
	 * MISSING_PERMISSIONS} <br>
	 * We do not have the required permissions</li>
	 *
	 * <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#UNKNOWN_WEBHOOK
	 * UNKNOWN_WEBHOOK} <br>
	 * A webhook with this id does not exist</li>
	 * </ul>
	 *
	 * @param webhookId The webhook id
	 *
	 * @throws IllegalArgumentException If the {@code webhookId} is null or empty
	 *
	 * @return {@link net.dv8tion.jda.api.requests.RestAction RestAction} - Type:
	 *         {@link net.dv8tion.jda.api.entities.Webhook Webhook} <br>
	 *         The webhook object.
	 *
	 * @see Guild#retrieveWebhooks()
	 * @see TextChannel#retrieveWebhooks()
	 */
	@NonNull
	RestAction<Webhook> retrieveWebhookById(@NonNull String webhookId);

	/**
	 * Retrieves a {@link net.dv8tion.jda.api.entities.Webhook Webhook} by its id.
	 * <br>
	 * If the webhook does not belong to any known guild of this JDA session, it
	 * will be {@link Webhook#isPartial() partial}.
	 *
	 * <p>
	 * Possible {@link net.dv8tion.jda.api.requests.ErrorResponse ErrorResponses}
	 * caused by the returned {@link net.dv8tion.jda.api.requests.RestAction
	 * RestAction} include the following:
	 * <ul>
	 * <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#MISSING_PERMISSIONS
	 * MISSING_PERMISSIONS} <br>
	 * We do not have the required permissions</li>
	 *
	 * <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#UNKNOWN_WEBHOOK
	 * UNKNOWN_WEBHOOK} <br>
	 * A webhook with this id does not exist</li>
	 * </ul>
	 *
	 * @param webhookId The webhook id
	 *
	 * @return {@link net.dv8tion.jda.api.requests.RestAction RestAction} - Type:
	 *         {@link net.dv8tion.jda.api.entities.Webhook Webhook} <br>
	 *         The webhook object.
	 *
	 * @see Guild#retrieveWebhooks()
	 * @see TextChannel#retrieveWebhooks()
	 */
	@NonNull
	default RestAction<Webhook> retrieveWebhookById(long webhookId) {
		return retrieveWebhookById(Long.toUnsignedString(webhookId));
	}

	/**
	 * Configures the required scopes applied to the
	 * {@link #getInviteUrl(Permission...)} and similar methods. <br>
	 * To use slash commands you must add {@code "applications.commands"} to these
	 * scopes. The scope {@code "bot"} is always applied.
	 *
	 * @param scopes The scopes to use with {@link #getInviteUrl(Permission...)} and
	 *               the likes
	 *
	 * @throws IllegalArgumentException If null is provided
	 *
	 * @return The current JDA instance
	 */
	@NonNull
	default void setRequiredScopes(@NonNull String... scopes) {
		Checks.noneNull(scopes, "Scopes");
		setRequiredScopes(Arrays.asList(scopes));
	}

	/**
	 * Configures the required scopes applied to the
	 * {@link #getInviteUrl(Permission...)} and similar methods. <br>
	 * To use slash commands you must add {@code "applications.commands"} to these
	 * scopes. The scope {@code "bot"} is always applied.
	 *
	 * @param scopes The scopes to use with {@link #getInviteUrl(Permission...)} and
	 *               the likes
	 *
	 * @throws IllegalArgumentException If null is provided
	 *
	 * @return The current JDA instance
	 */
	@NonNull
	void setRequiredScopes(@NonNull Collection<String> scopes);

	/**
	 * Creates an authorization invite url for the currently logged in Bot-Account.
	 * <br>
	 * Example Format:
	 * {@code https://discord.com/oauth2/authorize?scope=bot&client_id=288202953599221761&permissions=8}
	 *
	 * <p>
	 * <b>Hint:</b> To enable a pre-selected Guild of choice append the parameter
	 * {@code &guild_id=YOUR_GUILD_ID}
	 *
	 * @param permissions The permissions to use in your invite, these can be
	 *                    changed by the link user. <br>
	 *                    If no permissions are provided the {@code permissions}
	 *                    parameter is omitted
	 *
	 * @return A valid OAuth2 invite url for the currently logged in Bot-Account
	 */
	@NonNull
	default String getInviteUrl(@Nullable Permission... permissions) {
		Checks.noneNull(permissions, "Permissions");
		return getInviteUrl(Arrays.asList(permissions));
	}

	/**
	 * Creates an authorization invite url for the currently logged in Bot-Account.
	 * <br>
	 * Example Format:
	 * {@code https://discord.com/oauth2/authorize?scope=bot&client_id=288202953599221761&permissions=8}
	 *
	 * <p>
	 * <b>Hint:</b> To enable a pre-selected Guild of choice append the parameter
	 * {@code &guild_id=YOUR_GUILD_ID}
	 *
	 * @param permissions The permissions to use in your invite, these can be
	 *                    changed by the link user. <br>
	 *                    If no permissions are provided the {@code permissions}
	 *                    parameter is omitted
	 *
	 * @return A valid OAuth2 invite url for the currently logged in Bot-Account
	 */
	@NonNull
	String getInviteUrl(@Nullable Collection<Permission> permissions);

	/**
	 * Check if this {@link SpringJDA} instance is in the process of shutting down.
	 * 
	 * @return {@code true} if this instance is shutting down and should not be
	 *         operated on
	 */
	boolean isValid();
}
