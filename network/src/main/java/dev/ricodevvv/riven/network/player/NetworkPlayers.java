package dev.ricodevvv.riven.network.player;

import dev.ricodevvv.riven.network.redis.RedisConnector;
import dev.ricodevvv.riven.player.PlayerLookup;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Network-wide player registry in Redis: which server every player is on and their name. It's a
 * {@link PlayerLookup}, so parties and commands see players on any server. Local players answer from memory;
 * remote ones take one quick Redis read.
 */
public class NetworkPlayers implements PlayerLookup, Listener {

    private static final String SERVER_KEY = "players:server";
    private static final String NAME_KEY = "players:name";
    private static final String UUID_KEY = "players:uuid";

    private final JavaPlugin plugin;
    private final RedisConnector redis;
    private final String serverId;

    public NetworkPlayers(JavaPlugin plugin, RedisConnector redis, String serverId) {
        this.plugin = plugin;
        this.redis = redis;
        this.serverId = serverId;
    }

    public void start() {
        Bukkit.getPluginManager().registerEvents(this, this.plugin);

        for (Player player : Bukkit.getOnlinePlayers()) {
            this.register(player);
        }
    }

    /**
     * Unregisters everyone of this server (on shutdown).
     */
    public void stop() {
        HandlerList.unregisterAll(this);

        try {
            this.redis.with(jedis -> {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    final String id = player.getUniqueId().toString();

                    if (this.serverId.equals(jedis.hget(this.redis.key(SERVER_KEY), id))) {
                        jedis.hdel(this.redis.key(SERVER_KEY), id);
                    }
                }
                return null;
            });
        } catch (Exception e) {
            this.plugin.getLogger().log(Level.WARNING, "Couldn't unregister players from Redis", e);
        }
    }

    private void register(Player player) {
        final String id = player.getUniqueId().toString();
        final String name = player.getName();

        this.redis.async(jedis -> {
            jedis.hset(this.redis.key(SERVER_KEY), id, this.serverId);
            jedis.hset(this.redis.key(NAME_KEY), id, name);
            jedis.hset(this.redis.key(UUID_KEY), name.toLowerCase(), id);
        });
    }

    /**
     * Drops every player registered on a server (when it dies without saying goodbye).
     *
     * @param server server id
     */
    public void clearServer(String server) {
        this.redis.async(jedis -> {
            final List<String> stale = new ArrayList<>();

            for (Map.Entry<String, String> entry : jedis.hgetAll(this.redis.key(SERVER_KEY)).entrySet()) {
                if (entry.getValue().equals(server)) {
                    stale.add(entry.getKey());
                }
            }

            if (!stale.isEmpty()) {
                jedis.hdel(this.redis.key(SERVER_KEY), stale.toArray(new String[0]));
            }
        });
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        this.register(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        final String id = event.getPlayer().getUniqueId().toString();

        this.redis.async(jedis -> {
            if (this.serverId.equals(jedis.hget(this.redis.key(SERVER_KEY), id))) {
                jedis.hdel(this.redis.key(SERVER_KEY), id);
            }
        });
    }

    @Override
    public UUID findOnline(String name) {
        final Player local = Bukkit.getPlayerExact(name);

        if (local != null) {
            return local.getUniqueId();
        }

        try {
            return this.redis.with(jedis -> {
                final String id = jedis.hget(this.redis.key(UUID_KEY), name.toLowerCase());

                return id != null && jedis.hexists(this.redis.key(SERVER_KEY), id) ? UUID.fromString(id) : null;
            });
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String getName(UUID uniqueId) {
        final Player local = Bukkit.getPlayer(uniqueId);

        if (local != null) {
            return local.getName();
        }

        try {
            final String name = this.redis.with(jedis -> jedis.hget(this.redis.key(NAME_KEY), uniqueId.toString()));

            return name != null ? name : PlayerLookup.LOCAL.getName(uniqueId);
        } catch (Exception e) {
            return PlayerLookup.LOCAL.getName(uniqueId);
        }
    }

    @Override
    public boolean isOnline(UUID uniqueId) {
        return this.getServer(uniqueId) != null;
    }

    @Override
    public String getServer(UUID uniqueId) {
        if (Bukkit.getPlayer(uniqueId) != null) {
            return this.serverId;
        }

        try {
            return this.redis.with(jedis -> jedis.hget(this.redis.key(SERVER_KEY), uniqueId.toString()));
        } catch (Exception e) {
            return null;
        }
    }

}
