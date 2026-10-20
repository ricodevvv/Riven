package dev.ricodevvv.riven.network.transfer;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.ricodevvv.riven.network.NetworkMessenger;
import dev.ricodevvv.riven.network.redis.RedisConnector;
import dev.ricodevvv.riven.party.Party;
import dev.ricodevvv.riven.player.PlayerLookup;
import dev.ricodevvv.riven.util.BungeeUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.logging.Level;

/**
 * Moves players between servers keeping the reason with them. Before sending a player through the proxy,
 * the context (which game, which mode, spectate who...) is saved in Redis for a minute; when they land, the
 * target server reads it and the arrive handlers decide what to do (join the game, spectate...).
 *
 * <pre>
 * JsonObject context = new JsonObject();
 * context.addProperty("game", "solo-3");
 * transfers.transfer(player, "game-2", context);
 * transfers.onArrive((player, context) -&gt; arenas.join(player, context.get("game").getAsString()));
 * </pre>
 */
public class TransferService implements Listener {

    private static final String REQUEST = "transfer:request";
    private static final int TTL_SECONDS = 60;

    private final JavaPlugin plugin;
    private final RedisConnector redis;
    private final NetworkMessenger messenger;
    private final PlayerLookup lookup;
    private final String serverId;
    private final List<BiConsumer<Player, JsonObject>> arriveHandlers = new CopyOnWriteArrayList<>();

    public TransferService(JavaPlugin plugin, RedisConnector redis, NetworkMessenger messenger, PlayerLookup lookup, String serverId) {
        this.plugin = plugin;
        this.redis = redis;
        this.messenger = messenger;
        this.lookup = lookup;
        this.serverId = serverId;
    }

    public void start() {
        BungeeUtil.register(this.plugin);
        Bukkit.getPluginManager().registerEvents(this, this.plugin);

        this.messenger.on(REQUEST, message -> {
            final JsonObject payload = message.getPayload();
            final Player player = Bukkit.getPlayer(UUID.fromString(payload.get("player").getAsString()));

            if (player != null) {
                this.transfer(player, payload.get("server").getAsString(), payload.getAsJsonObject("context"));
            }
        });
    }

    public void stop() {
        HandlerList.unregisterAll(this);
    }

    /**
     * @param handler runs on the main thread when a player arrives with a context (also for local transfers)
     */
    public void onArrive(BiConsumer<Player, JsonObject> handler) {
        this.arriveHandlers.add(handler);
    }

    /**
     * Sends a player to a server with a context. If the server is this one, the handlers run right away.
     *
     * @param player player on this server
     * @param server target server
     * @param context why they're going, can be null
     */
    public void transfer(Player player, String server, JsonObject context) {
        final JsonObject data = context == null ? new JsonObject() : context;

        if (server.equals(this.serverId)) {
            this.arrive(player, data);
            return;
        }

        final String key = this.redis.key("transfer:" + player.getUniqueId());
        final String json = data.toString();

        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
            try {
                this.redis.with(jedis -> jedis.setex(key, TTL_SECONDS, json));
            } catch (Exception e) {
                this.plugin.getLogger().log(Level.WARNING, "Couldn't store the transfer of " + player.getName(), e);
            }

            Bukkit.getScheduler().runTask(this.plugin, () -> {
                if (player.isOnline()) {
                    BungeeUtil.connect(this.plugin, player, server);
                }
            });
        });
    }

    /**
     * Sends any player of the network to a server, asking their current server to do it if needed.
     *
     * @param uniqueId player
     * @param server target server
     * @param context why they're going, can be null
     * @return false if the player isn't online anywhere
     */
    public boolean transfer(UUID uniqueId, String server, JsonObject context) {
        final Player local = Bukkit.getPlayer(uniqueId);

        if (local != null) {
            this.transfer(local, server, context);
            return true;
        }

        final String current = this.lookup.getServer(uniqueId);

        if (current == null) {
            return false;
        }

        if (current.equals(server)) {
            return true;
        }

        final JsonObject payload = new JsonObject();

        payload.addProperty("player", uniqueId.toString());
        payload.addProperty("server", server);
        payload.add("context", context == null ? new JsonObject() : context);

        this.messenger.send(REQUEST, payload, current);
        return true;
    }

    /**
     * Sends the leader and every member online anywhere to a server, so the party stays together. The context
     * gets a "party" field with the party id and a "leader" field.
     *
     * @param leader leader on this server
     * @param party party
     * @param server target server
     * @param context why they're going
     */
    public void transferParty(Player leader, Party party, String server, JsonObject context) {
        final JsonObject data = context == null ? new JsonObject() : context;

        data.addProperty("party", party.getId().toString());
        data.addProperty("leader", party.getLeader().toString());

        for (UUID member : party.getMembers()) {
            if (!member.equals(leader.getUniqueId())) {
                this.transfer(member, server, copy(data));
            }
        }

        this.transfer(leader, server, data);
    }

    private static JsonObject copy(JsonObject json) {
        return new JsonParser().parse(json.toString()).getAsJsonObject();
    }

    private void arrive(Player player, JsonObject context) {
        for (BiConsumer<Player, JsonObject> handler : this.arriveHandlers) {
            try {
                handler.accept(player, context);
            } catch (Exception e) {
                this.plugin.getLogger().log(Level.SEVERE, "Transfer handler failed for " + player.getName(), e);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        final String key = this.redis.key("transfer:" + player.getUniqueId());

        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
            final String json;

            try {
                json = this.redis.with(jedis -> {
                    final String value = jedis.get(key);

                    if (value != null) {
                        jedis.del(key);
                    }
                    return value;
                });
            } catch (Exception e) {
                this.plugin.getLogger().log(Level.WARNING, "Couldn't read the transfer of " + player.getName(), e);
                return;
            }

            if (json == null) {
                return;
            }

            final JsonObject context = new JsonParser().parse(json).getAsJsonObject();

            Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
                if (player.isOnline()) {
                    this.arrive(player, context);
                }
            }, 5L);
        });
    }

}
