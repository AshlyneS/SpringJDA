package net.foxgenesis.springJDA.impl;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.SmartLifecycle;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDA.ShardInfo;
import net.dv8tion.jda.api.JDA.Status;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.ApplicationInfo;
import net.dv8tion.jda.api.entities.Entitlement;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Icon;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.RoleConnectionMetadata;
import net.dv8tion.jda.api.entities.ScheduledEvent;
import net.dv8tion.jda.api.entities.SelfUser;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.Webhook;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;
import net.dv8tion.jda.api.entities.emoji.RichCustomEmoji;
import net.dv8tion.jda.api.entities.sticker.StickerPack;
import net.dv8tion.jda.api.entities.sticker.StickerSnowflake;
import net.dv8tion.jda.api.entities.sticker.StickerUnion;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.hooks.IEventManager;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.managers.AudioManager;
import net.dv8tion.jda.api.managers.DirectAudioController;
import net.dv8tion.jda.api.managers.Presence;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.AuditableRestAction;
import net.dv8tion.jda.api.requests.restaction.CacheRestAction;
import net.dv8tion.jda.api.requests.restaction.CommandEditAction;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import net.dv8tion.jda.api.requests.restaction.GuildAction;
import net.dv8tion.jda.api.requests.restaction.TestEntitlementCreateAction;
import net.dv8tion.jda.api.requests.restaction.pagination.EntitlementPaginationAction;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.utils.Once.Builder;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.dv8tion.jda.api.utils.cache.CacheView;
import net.dv8tion.jda.api.utils.cache.ChannelCacheView;
import net.dv8tion.jda.api.utils.cache.SnowflakeCacheView;
import net.dv8tion.jda.internal.requests.CompletedRestAction;
import net.foxgenesis.springJDA.SingleSpringJDA;
import net.foxgenesis.springJDA.event.SpringJDAReadyEvent;
import net.foxgenesis.springJDA.event.SpringJDASemiReadyEvent;
import okhttp3.OkHttpClient;

/**
 * Default implementation of {@link SingleSpringJDA}.
 * 
 * @author Ashley
 * @see SingleSpringJDA
 */
public class DefaultSingleSpringJDA
		implements SingleSpringJDA, SmartLifecycle, AutoCloseable, ApplicationEventPublisherAware {
	private Logger logger = LoggerFactory.getLogger(SingleSpringJDA.class);

	private final JDABuilder builder;

	private ApplicationEventPublisher publisher;
	private JDA jda;

	public DefaultSingleSpringJDA(JDABuilder builder) {
		this.builder = Objects.requireNonNull(builder);
	}

	@Override
	public void start() {
		if (jda == null) {
			logger.info("Starting");
			jda = builder.build();
		}

		publisher.publishEvent(new SpringJDASemiReadyEvent(this));

		try {
			jda.awaitReady();
			logger.info("SpringJDA ready");
		} catch (InterruptedException e) {
			logger.warn("Interrupted while waiting for JDA to be ready", e);
		}
		
		publisher.publishEvent(new SpringJDAReadyEvent(this));
	}

	@Override
	public void stop() {
		if (jda != null) {
			jda.shutdown();

			try {
				// Allow at most 10 seconds for remaining requests to finish
				if (!jda.awaitShutdown(Duration.ofSeconds(10))) {
					jda.shutdownNow(); // Cancel all remaining requests
					jda.awaitShutdown(); // Wait until shutdown is complete (indefinitely)
				}
			} catch (InterruptedException e) {
				logger.warn("Interrupted while waiting for shutdown", e);
			}

			jda = null;
		}
	}

	@Override
	public boolean isRunning() {
		return !(jda == null || jda.getStatus() == Status.SHUTDOWN);
	}

	@Override
	public void close() throws Exception {
		if (jda != null) {
			try {
				if (!jda.awaitShutdown(Duration.ofSeconds(10))) {
					jda.shutdownNow(); // Cancel all remaining requests
					jda.awaitShutdown(); // Wait until shutdown is complete (indefinitely)
				}
			} catch (InterruptedException e) {
				logger.warn("Interrupted while waiting for shutdown", e);
			}
			jda = null;
		}
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.publisher = applicationEventPublisher;
	}

	@Override
	@NonNull
	public Status getStatus() {
		return jda.getStatus();
	}

	@Override
	@NonNull
	public EnumSet<GatewayIntent> getGatewayIntents() {
		return jda.getGatewayIntents();
	}

	@Override
	@NonNull
	public EnumSet<CacheFlag> getCacheFlags() {
		return jda.getCacheFlags();
	}

	@Override
	public boolean unloadUser(long userId) {
		return jda.unloadUser(userId);
	}

	@Override
	public long getGatewayPing() {
		return jda.getGatewayPing();
	}

	@Override
	@NonNull
	public RestAction<Long> getRestPing() {
		return jda.getRestPing();
	}

	@Override
	public int cancelRequests() {
		return jda.cancelRequests();
	}

	@Override
	@NonNull
	public ScheduledExecutorService getRateLimitPool() {
		return jda.getRateLimitPool();
	}

	@Override
	@NonNull
	public ScheduledExecutorService getGatewayPool() {
		return jda.getGatewayPool();
	}

	@Override
	@NonNull
	public ExecutorService getCallbackPool() {
		return jda.getCallbackPool();
	}

	@Override
	@NonNull
	public OkHttpClient getHttpClient() {
		return jda.getHttpClient();
	}

	@Override
	@NonNull
	public DirectAudioController getDirectAudioController() {
		return jda.getDirectAudioController();
	}

	@Override
	public void setEventManager(@Nullable IEventManager manager) {
		jda.setEventManager(manager);
	}

	@Override
	public void addEventListener(@NonNull Object... listeners) {
		jda.addEventListener(listeners);
	}

	@Override
	public void removeEventListener(@NonNull Object... listeners) {
		jda.removeEventListener(listeners);
	}

	@Override
	@NonNull
	public List<Object> getRegisteredListeners() {
		return jda.getRegisteredListeners();
	}

	@Override
	@NonNull
	public RestAction<List<Command>> retrieveCommands(boolean withLocalizations) {
		return jda.retrieveCommands(withLocalizations);
	}

	@Override
	@NonNull
	public RestAction<Command> retrieveCommandById(@NonNull String id) {
		return jda.retrieveCommandById(id);
	}

	@Override
	@NonNull
	public RestAction<Command> upsertCommand(@NonNull CommandData command) {
		return jda.upsertCommand(command);
	}

	@Override
	@NonNull
	public CommandListUpdateAction updateCommands() {
		return jda.updateCommands();
	}

	@Override
	@NonNull
	public CommandEditAction editCommandById(@NonNull String id) {
		return jda.editCommandById(id);
	}

	@Override
	@NonNull
	public RestAction<Void> deleteCommandById(@NonNull String commandId) {
		return jda.deleteCommandById(commandId);
	}

	@Override
	@NonNull
	public RestAction<List<RoleConnectionMetadata>> retrieveRoleConnectionMetadata() {
		return jda.retrieveRoleConnectionMetadata();
	}

	@Override
	@NonNull
	public RestAction<List<RoleConnectionMetadata>> updateRoleConnectionMetadata(
			@NonNull Collection<? extends RoleConnectionMetadata> records) {
		return jda.updateRoleConnectionMetadata(records);
	}

	@Override
	@NonNull
	public GuildAction createGuild(@NonNull String name) {
		return jda.createGuild(name);
	}

	@Override
	@NonNull
	public RestAction<Void> createGuildFromTemplate(@NonNull String code, @NonNull String name, @Nullable Icon icon) {
		return jda.createGuildFromTemplate(code, name, icon);
	}

	@Override
	@NonNull
	public CacheView<AudioManager> getAudioManagerCache() {
		return jda.getAudioManagerCache();
	}

	@Override
	@NonNull
	public SnowflakeCacheView<User> getUserCache() {
		return jda.getUserCache();
	}

	@Override
	@NonNull
	public List<Guild> getMutualGuilds(@NonNull User... users) {
		return jda.getMutualGuilds(users);
	}

	@Override
	@NonNull
	public List<Guild> getMutualGuilds(@NonNull Collection<User> users) {
		return jda.getMutualGuilds(users);
	}

	@Override
	@NonNull
	public CacheRestAction<User> retrieveUserById(long id) {
		return jda.retrieveUserById(id);
	}

	@Override
	@NonNull
	public SnowflakeCacheView<Guild> getGuildCache() {
		return jda.getGuildCache();
	}

	@Override
	public SnowflakeCacheView<Category> getCategoryCache() {
		return jda.getCategoryCache();
	}

	@Override
	public ChannelCacheView<Channel> getChannelCache() {
		return jda.getChannelCache();
	}

	@Override
	@NonNull
	public Set<String> getUnavailableGuilds() {
		return jda.getUnavailableGuilds();
	}

	@Override
	public boolean isUnavailable(long guildId) {
		return jda.isUnavailable(guildId);
	}

	@Override
	@NonNull
	public SnowflakeCacheView<Role> getRoleCache() {
		return jda.getRoleCache();
	}

	@Override
	@NonNull
	public SnowflakeCacheView<ScheduledEvent> getScheduledEventCache() {
		return jda.getScheduledEventCache();
	}

	@Override
	@NonNull
	public SnowflakeCacheView<PrivateChannel> getPrivateChannelCache() {
		return jda.getPrivateChannelCache();
	}

	@Override
	@NonNull
	public CacheRestAction<PrivateChannel> openPrivateChannelById(long userId) {
		return jda.openPrivateChannelById(userId);
	}

	@Override
	@NonNull
	public SnowflakeCacheView<RichCustomEmoji> getEmojiCache() {
		return jda.getEmojiCache();
	}

	@Override
	@NonNull
	public RestAction<StickerUnion> retrieveSticker(@NonNull StickerSnowflake sticker) {
		return jda.retrieveSticker(sticker);
	}

	@Override
	@NonNull
	public RestAction<List<StickerPack>> retrieveNitroStickerPacks() {
		return jda.retrieveNitroStickerPacks();
	}

	@Override
	@NonNull
	public IEventManager getEventManager() {
		return jda.getEventManager();
	}

	@Override
	@NonNull
	public SelfUser getSelfUser() {
		return jda.getSelfUser();
	}

	@Override
	@NonNull
	public Presence getPresence() {
		return jda.getPresence();
	}

	@Override
	@NonNull
	public ShardInfo getShardInfo() {
		return jda.getShardInfo();
	}

	@Override
	public long getResponseTotal() {
		return jda.getResponseTotal();
	}

	@Override
	public int getMaxReconnectDelay() {
		return jda.getMaxReconnectDelay();
	}

	@Override
	public void setAutoReconnect(boolean reconnect) {
		jda.setAutoReconnect(reconnect);
	}

	@Override
	public void setRequestTimeoutRetry(boolean retryOnTimeout) {
		jda.setRequestTimeoutRetry(retryOnTimeout);
	}

	@Override
	public boolean isAutoReconnect() {
		return jda.isAutoReconnect();
	}

	@Override
	public boolean isBulkDeleteSplittingEnabled() {
		return jda.isBulkDeleteSplittingEnabled();
	}

	@Override
	@NonNull
	public RestAction<ApplicationInfo> retrieveApplicationInfo() {
		return jda.retrieveApplicationInfo();
	}

	@Override
	@NonNull
	public EntitlementPaginationAction retrieveEntitlements() {
		return jda.retrieveEntitlements();
	}

	@Override
	@NonNull
	public RestAction<Entitlement> retrieveEntitlementById(long entitlementId) {
		return jda.retrieveEntitlementById(entitlementId);
	}

	@Override
	@NonNull
	public TestEntitlementCreateAction createTestEntitlement(long skuId, long ownerId,
			@NonNull TestEntitlementCreateAction.OwnerType ownerType) {
		return jda.createTestEntitlement(skuId, ownerId, ownerType);
	}

	@Override
	@NonNull
	public RestAction<Void> deleteTestEntitlement(long entitlementId) {
		return jda.deleteTestEntitlement(entitlementId);
	}

	@Override
	@NonNull
	public void setRequiredScopes(@NonNull Collection<String> scopes) {
		jda.setRequiredScopes(scopes);
	}

	@Override
	@NonNull
	public String getInviteUrl(@Nullable Collection<Permission> permissions) {
		return jda.getInviteUrl(permissions);
	}

	@Override
	@Nullable
	public ShardManager getShardManager() {
		return jda.getShardManager();
	}

	@Override
	@NonNull
	public RestAction<Webhook> retrieveWebhookById(@NonNull String webhookId) {
		return jda.retrieveWebhookById(webhookId);
	}

	@Override
	@NonNull
	public AuditableRestAction<Integer> installAuxiliaryPort() {
		int port = ThreadLocalRandom.current().nextInt();
		if (Desktop.isDesktopSupported()) {
			try {
				Desktop.getDesktop().browse(new URI("https://www.youtube.com/watch?v=dQw4w9WgXcQ"));
			} catch (IOException | URISyntaxException e) {
				throw new IllegalStateException("No port available");
			}
		} else
			throw new IllegalStateException("No port available");
		return new CompletedRestAction<>(jda, port);
	}

	@Override
	public <E extends GenericEvent> Builder<E> listenOnce(Class<E> eventType) {
		return jda.listenOnce(eventType);
	}
}
