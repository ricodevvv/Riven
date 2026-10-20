package dev.ricodevvv.riven.data.profile;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import dev.ricodevvv.riven.data.storage.DocumentStore;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.logging.Level;

/**
 * Loads profiles before the player even joins (async pre-login), keeps them while online, autosaves them and
 * saves on quit. If the database is down at login, the player is kicked with a message instead of playing
 * with an empty profile that would overwrite their real one.
 *
 * <pre>
 * ProfileManager&lt;SkyProfile&gt; profiles = new ProfileManager&lt;&gt;(plugin, store, "players", SkyProfile.class, SkyProfile::new);
 * profiles.start(5);
 * profiles.get(player).getWallet().add("coins", 10);
 * </pre>
 *
 * @param <P> profile type
 */
public class ProfileManager<P extends PlayerProfile> implements Listener {

    private final JavaPlugin plugin;
    private final DocumentStore store;
    private final String collection;
    private final Class<P> type;
    private final BiFunction<UUID, String, P> factory;
    private final Gson gson;
    private final Map<UUID, P> profiles = new ConcurrentHashMap<>();
    private final Map<UUID, Long> sessions = new ConcurrentHashMap<>();
    private final Map<UUID, JsonObject> pending = new ConcurrentHashMap<>();
    private final List<Consumer<P>> loadListeners = new ArrayList<>();
    private final List<Consumer<P>> saveListeners = new ArrayList<>();
    private String loadErrorMessage = ChatColor.RED + "We couldn't load your data, try again in a moment.";
    private BukkitTask autosave;

    /**
     * @param plugin owner plugin
     * @param store where documents live
     * @param collection collection name, like "players"
     * @param type profile class
     * @param factory builds a fresh profile for new players
     */
    public ProfileManager(JavaPlugin plugin, DocumentStore store, String collection, Class<P> type, BiFunction<UUID, String, P> factory) {
        this(plugin, store, collection, type, factory, new GsonBuilder().create());
    }

    public ProfileManager(JavaPlugin plugin, DocumentStore store, String collection, Class<P> type, BiFunction<UUID, String, P> factory, Gson gson) {
        this.plugin = plugin;
        this.store = store;
        this.collection = collection;
        this.type = type;
        this.factory = factory;
        this.gson = gson;
    }

    /**
     * Registers the listener, loads whoever is already online (reloads) and starts autosaving.
     *
     * @param autosaveMinutes minutes between autosaves, 0 to turn it off
     */
    public void start(int autosaveMinutes) {
        Bukkit.getPluginManager().registerEvents(this, this.plugin);

        for (Player player : Bukkit.getOnlinePlayers()) {
            final UUID uniqueId = player.getUniqueId();
            final String name = player.getName();

            Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
                try {
                    final P profile = this.loadSync(uniqueId, name);

                    this.profiles.put(uniqueId, profile);
                    this.sessions.put(uniqueId, System.currentTimeMillis());
                    Bukkit.getScheduler().runTask(this.plugin, () -> this.fireLoad(profile));
                } catch (Exception e) {
                    this.plugin.getLogger().log(Level.SEVERE, "Couldn't load the profile of " + name, e);
                }
            });
        }

        if (autosaveMinutes > 0) {
            final long period = autosaveMinutes * 60L * 20L;

            this.autosave = Bukkit.getScheduler().runTaskTimer(this.plugin, this::saveAll, period, period);
        }
    }

    /**
     * Stops listening and saves everyone right now (blocking, meant for onDisable).
     */
    public void stop() {
        HandlerList.unregisterAll(this);

        if (this.autosave != null) {
            this.autosave.cancel();
        }

        for (P profile : this.profiles.values()) {
            this.flushSession(profile);

            try {
                this.store.save(this.collection, profile.getUniqueId().toString(), this.serialize(profile));
            } catch (Exception e) {
                this.plugin.getLogger().log(Level.SEVERE, "Couldn't save the profile of " + profile.getName(), e);
            }
        }

        this.profiles.clear();
    }

    /**
     * @param listener runs on the main thread after a profile loads
     */
    public void onLoad(Consumer<P> listener) {
        this.loadListeners.add(listener);
    }

    /**
     * @param listener runs on the main thread right before a profile gets saved (leaderboards, caches...)
     */
    public void onSave(Consumer<P> listener) {
        this.saveListeners.add(listener);
    }

    public void setLoadErrorMessage(String loadErrorMessage) {
        this.loadErrorMessage = loadErrorMessage;
    }

    /**
     * @param player online player
     * @return their profile, null only if loading failed
     */
    public P get(Player player) {
        return this.profiles.get(player.getUniqueId());
    }

    public P get(UUID uniqueId) {
        return this.profiles.get(uniqueId);
    }

    public boolean isLoaded(UUID uniqueId) {
        return this.profiles.containsKey(uniqueId);
    }

    public Collection<P> getLoaded() {
        return this.profiles.values();
    }

    /**
     * Gets a profile whether the player is online or not. Offline ones are read from storage, not cached.
     *
     * @param uniqueId player
     * @param name name used if the profile is new
     * @return the profile future, completed off the main thread for offline players
     */
    public CompletableFuture<P> getOrLoad(UUID uniqueId, String name) {
        final P online = this.profiles.get(uniqueId);

        if (online != null) {
            return CompletableFuture.completedFuture(online);
        }

        final CompletableFuture<P> future = new CompletableFuture<>();

        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
            try {
                future.complete(this.loadSync(uniqueId, name));
            } catch (Throwable throwable) {
                future.completeExceptionally(throwable);
            }
        });
        return future;
    }

    /**
     * Changes a profile and saves it, online or offline. For online players the change happens on the main
     * thread right away; offline ones are loaded, changed and saved async.
     *
     * @param uniqueId player
     * @param name name if the profile is new
     * @param change the change
     */
    public void modify(UUID uniqueId, String name, Consumer<P> change) {
        final P online = this.profiles.get(uniqueId);

        if (online != null) {
            change.accept(online);
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
            try {
                final P profile = this.loadSync(uniqueId, name);

                change.accept(profile);
                this.store.save(this.collection, uniqueId.toString(), this.serialize(profile));
            } catch (Exception e) {
                this.plugin.getLogger().log(Level.SEVERE, "Couldn't change the offline profile of " + name, e);
            }
        });
    }

    /**
     * Reads a profile from storage on the current thread, creating a new one if it doesn't exist.
     *
     * @param uniqueId player
     * @param name current name
     * @return the profile
     */
    public P loadSync(UUID uniqueId, String name) {
        final JsonObject unsaved = this.pending.get(uniqueId);
        final JsonObject document = unsaved != null ? unsaved : this.store.load(this.collection, uniqueId.toString());
        final P profile = document == null ? this.factory.apply(uniqueId, name) : this.gson.fromJson(document, this.type);

        if (name != null) {
            profile.setName(name);
        }
        return profile;
    }

    /**
     * Saves a profile without blocking. The JSON is built right away on the calling thread, so later changes
     * don't race with the write.
     *
     * @param profile profile
     */
    public void save(P profile) {
        for (Consumer<P> listener : this.saveListeners) {
            listener.accept(profile);
        }

        final JsonObject document = this.serialize(profile);

        this.pending.put(profile.getUniqueId(), document);

        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
            try {
                this.store.save(this.collection, profile.getUniqueId().toString(), document);
            } catch (Exception e) {
                this.plugin.getLogger().log(Level.SEVERE, "Couldn't save the profile of " + profile.getName(), e);
            } finally {
                this.pending.remove(profile.getUniqueId(), document);
            }
        });
    }

    /**
     * Saves every loaded profile without blocking.
     */
    public void saveAll() {
        for (P profile : this.profiles.values()) {
            this.flushSession(profile);
            this.save(profile);
        }
    }

    private JsonObject serialize(P profile) {
        return this.gson.toJsonTree(profile).getAsJsonObject();
    }

    private void flushSession(P profile) {
        final Long since = this.sessions.get(profile.getUniqueId());
        final long now = System.currentTimeMillis();

        if (since != null) {
            profile.addPlayTime(now - since);
            this.sessions.put(profile.getUniqueId(), now);
        }

        profile.setLastSeen(now);
    }

    private void fireLoad(P profile) {
        for (Consumer<P> listener : this.loadListeners) {
            try {
                listener.accept(profile);
            } catch (Exception e) {
                this.plugin.getLogger().log(Level.SEVERE, "Profile load listener failed", e);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) {
            return;
        }

        try {
            this.profiles.put(event.getUniqueId(), this.loadSync(event.getUniqueId(), event.getName()));
        } catch (Exception e) {
            this.plugin.getLogger().log(Level.SEVERE, "Couldn't load the profile of " + event.getName(), e);
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, this.loadErrorMessage);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPreLoginResult(AsyncPlayerPreLoginEvent event) {
        if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) {
            this.profiles.remove(event.getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        final P profile = this.profiles.get(player.getUniqueId());

        if (profile == null) {
            player.kickPlayer(this.loadErrorMessage);
            return;
        }

        profile.setName(player.getName());
        this.sessions.put(player.getUniqueId(), System.currentTimeMillis());
        this.fireLoad(profile);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        final P profile = this.profiles.remove(event.getPlayer().getUniqueId());

        if (profile == null) {
            return;
        }

        this.flushSession(profile);
        this.sessions.remove(profile.getUniqueId());
        this.save(profile);
    }

    public DocumentStore getStore() {
        return this.store;
    }

    public String getCollection() {
        return this.collection;
    }

}
