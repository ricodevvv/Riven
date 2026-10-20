package dev.ricodevvv.riven.network.server;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.ricodevvv.riven.network.NetworkMessenger;
import dev.ricodevvv.riven.network.redis.RedisConnector;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;

/**
 * Every server publishes a heartbeat with its state every two seconds (players, games, drain flag) and keeps
 * a live view of the others. A server that misses heartbeats for {@code timeout} is considered dead: it's
 * dropped and the down listeners run, so the rest can redirect its players.
 */
public class ServerRegistry {

    private static final String SERVERS_KEY = "servers";
    private static final String HEARTBEAT = "server:heartbeat";
    private static final String SHUTDOWN = "server:shutdown";

    private final JavaPlugin plugin;
    private final RedisConnector redis;
    private final NetworkMessenger messenger;
    private final String serverId;
    private final String group;
    private final ServerMode mode;
    private final long timeout;
    private final Map<String, ServerInfo> servers = new ConcurrentHashMap<>();
    private final List<Consumer<ServerInfo>> downListeners = new CopyOnWriteArrayList<>();
    private final List<Consumer<ServerInfo>> upListeners = new CopyOnWriteArrayList<>();

    private Supplier<List<GameInfo>> games = Collections::emptyList;
    private int maxGames;
    private volatile boolean draining;
    private BukkitTask heartbeatTask;
    private BukkitTask cleanupTask;

    /**
     * @param plugin owner plugin
     * @param redis connector
     * @param messenger messenger
     * @param serverId this server's name on the proxy
     * @param group server group
     * @param mode this server's mode
     * @param timeoutMillis heartbeat timeout, 30000 is a good default
     */
    public ServerRegistry(JavaPlugin plugin, RedisConnector redis, NetworkMessenger messenger, String serverId, String group, ServerMode mode, long timeoutMillis) {
        this.plugin = plugin;
        this.redis = redis;
        this.messenger = messenger;
        this.serverId = serverId;
        this.group = group;
        this.mode = mode;
        this.timeout = timeoutMillis;
    }

    public void start() {
        this.messenger.onAsync(HEARTBEAT, message -> this.accept(ServerInfo.fromJson(message.getPayload())));
        this.messenger.onAsync(SHUTDOWN, message -> {
            final ServerInfo removed = this.servers.remove(message.getSource());

            if (removed != null && !removed.getId().equals(this.serverId)) {
                this.fire(this.downListeners, removed);
            }
        });

        this.redis.async(jedis -> {
            for (String value : jedis.hgetAll(this.redis.key(SERVERS_KEY)).values()) {
                try {
                    final ServerInfo info = ServerInfo.fromJson(new JsonParser().parse(value).getAsJsonObject());

                    if (info.isAlive(this.timeout)) {
                        this.servers.put(info.getId(), info);
                    }
                } catch (Exception ignored) {
                }
            }
        });

        this.heartbeatTask = Bukkit.getScheduler().runTaskTimer(this.plugin, this::heartbeat, 20L, 40L);
        this.cleanupTask = Bukkit.getScheduler().runTaskTimerAsynchronously(this.plugin, this::cleanup, 100L, 100L);
    }

    /**
     * Stops the heartbeat and tells the network this server is gone.
     */
    public void stop() {
        if (this.heartbeatTask != null) {
            this.heartbeatTask.cancel();
        }

        if (this.cleanupTask != null) {
            this.cleanupTask.cancel();
        }

        try {
            this.redis.with(jedis -> jedis.hdel(this.redis.key(SERVERS_KEY), this.serverId));
        } catch (Exception e) {
            this.plugin.getLogger().log(Level.WARNING, "Couldn't remove this server from Redis", e);
        }

        this.messenger.send(SHUTDOWN, new JsonObject(), null);
    }

    private void heartbeat() {
        final ServerInfo info = this.snapshot();
        final String json = info.toJson().toString();

        this.servers.put(info.getId(), info);
        this.redis.async(jedis -> jedis.hset(this.redis.key(SERVERS_KEY), this.serverId, json));
        this.messenger.send(HEARTBEAT, info.toJson(), null);
    }

    /**
     * @return this server's current state
     */
    public ServerInfo snapshot() {
        List<GameInfo> current;

        try {
            current = this.games.get();
        } catch (Exception e) {
            current = Collections.emptyList();
        }

        return new ServerInfo(this.serverId, this.group, this.mode, Bukkit.getOnlinePlayers().size(), Bukkit.getMaxPlayers(), this.maxGames,
                this.draining, System.currentTimeMillis(), current);
    }

    private void accept(ServerInfo info) {
        final ServerInfo old = this.servers.put(info.getId(), info);

        if (old == null && !info.getId().equals(this.serverId)) {
            this.fire(this.upListeners, info);
        }
    }

    private void cleanup() {
        for (ServerInfo info : new ArrayList<>(this.servers.values())) {
            if (info.getId().equals(this.serverId) || info.isAlive(this.timeout)) {
                continue;
            }

            if (this.servers.remove(info.getId(), info)) {
                this.plugin.getLogger().warning("Server " + info.getId() + " stopped answering, dropping it");
                this.redis.async(jedis -> jedis.hdel(this.redis.key(SERVERS_KEY), info.getId()));
                this.fire(this.downListeners, info);
            }
        }
    }

    private void fire(List<Consumer<ServerInfo>> listeners, ServerInfo info) {
        if (!this.plugin.isEnabled()) {
            return;
        }

        Bukkit.getScheduler().runTask(this.plugin, () -> {
            for (Consumer<ServerInfo> listener : listeners) {
                listener.accept(info);
            }
        });
    }

    /**
     * @param games what this server reports as its games
     */
    public void setGames(Supplier<List<GameInfo>> games) {
        this.games = games;
    }

    /**
     * @param maxGames how many games this server can run at once
     */
    public void setMaxGames(int maxGames) {
        this.maxGames = maxGames;
    }

    public boolean isDraining() {
        return this.draining;
    }

    public void setDraining(boolean draining) {
        this.draining = draining;
    }

    /**
     * @param listener runs on the main thread when a server dies or shuts down
     */
    public void onServerDown(Consumer<ServerInfo> listener) {
        this.downListeners.add(listener);
    }

    /**
     * @param listener runs on the main thread when a new server shows up
     */
    public void onServerUp(Consumer<ServerInfo> listener) {
        this.upListeners.add(listener);
    }

    public ServerInfo getServer(String id) {
        return this.servers.get(id);
    }

    /**
     * @return every live server, this one included
     */
    public Collection<ServerInfo> getServers() {
        return Collections.unmodifiableCollection(this.servers.values());
    }

    /**
     * @param group group, null for any
     * @param mode mode, null for any
     * @return live servers matching both
     */
    public List<ServerInfo> getServers(String group, ServerMode mode) {
        final List<ServerInfo> result = new ArrayList<>();

        for (ServerInfo info : this.servers.values()) {
            if ((group == null || info.getGroup().equalsIgnoreCase(group)) && (mode == null || info.getMode() == mode) && info.isAlive(this.timeout)) {
                result.add(info);
            }
        }
        return result;
    }

    /**
     * @param gameMode game mode, null for any
     * @return players in games of that mode across the network
     */
    public int getPlayersIn(String gameMode) {
        int total = 0;

        for (ServerInfo info : this.getServers(this.group, ServerMode.GAME)) {
            total += info.getPlayersIn(gameMode);
        }
        return total;
    }

    public String getServerId() {
        return this.serverId;
    }

    public String getGroup() {
        return this.group;
    }

    public ServerMode getMode() {
        return this.mode;
    }

}
