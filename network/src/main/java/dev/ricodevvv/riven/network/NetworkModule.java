package dev.ricodevvv.riven.network;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.ricodevvv.riven.Riven;
import dev.ricodevvv.riven.module.RivenModule;
import dev.ricodevvv.riven.network.party.NetworkPartyNotifier;
import dev.ricodevvv.riven.network.party.RedisPartyBackend;
import dev.ricodevvv.riven.network.player.NetworkPlayers;
import dev.ricodevvv.riven.network.redis.RedisConfig;
import dev.ricodevvv.riven.network.redis.RedisConnector;
import dev.ricodevvv.riven.network.server.DrainController;
import dev.ricodevvv.riven.network.server.ServerMode;
import dev.ricodevvv.riven.network.server.ServerRegistry;
import dev.ricodevvv.riven.network.transfer.LoadBalancer;
import dev.ricodevvv.riven.network.transfer.TransferService;
import dev.ricodevvv.riven.party.PartyManager;
import dev.ricodevvv.riven.player.PlayerLookup;
import dev.ricodevvv.riven.text.FancyMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * The network layer as a Riven module: Redis messaging, server registry with heartbeats, network wide player
 * lookup, transfers with context, load balancing, drain mode, cross-server parties, global spectating and
 * network broadcasts.
 *
 * <pre>
 * NetworkModule network = riven.install(new NetworkModule(RedisConfig.from(section), "game-1", "skywars", ServerMode.GAME));
 * network.getRegistry().setGames(() -&gt; arenas.toGameInfos());
 * network.getTransfers().onArrive((player, context) -&gt; ...);
 * </pre>
 */
public class NetworkModule implements RivenModule {

    private static final String BROADCAST = "network:broadcast";
    private static final String SPECTATE = "spectate";

    private final RedisConfig config;
    private final String serverId;
    private final String group;
    private final ServerMode mode;
    private boolean networkParties = true;
    private int partyTtl = 7200;
    private long serverTimeout = 30_000L;

    private RedisConnector redis;
    private NetworkMessenger messenger;
    private ServerRegistry registry;
    private NetworkPlayers players;
    private TransferService transfers;
    private LoadBalancer balancer;
    private DrainController drain;

    /**
     * @param config Redis settings
     * @param serverId this server's name on the proxy
     * @param group server group, like "skywars"
     * @param mode lobby or game
     */
    public NetworkModule(RedisConfig config, String serverId, String group, ServerMode mode) {
        this.config = config;
        this.serverId = serverId;
        this.group = group;
        this.mode = mode;
    }

    /**
     * @param networkParties false to keep parties local to each server
     * @param ttlSeconds how long a party lives in Redis without changes
     * @return this module
     */
    public NetworkModule parties(boolean networkParties, int ttlSeconds) {
        this.networkParties = networkParties;
        this.partyTtl = ttlSeconds;
        return this;
    }

    /**
     * @param millis how long without heartbeat before a server counts as dead
     * @return this module
     */
    public NetworkModule serverTimeout(long millis) {
        this.serverTimeout = millis;
        return this;
    }

    @Override
    public void enable(Riven riven) {
        this.redis = new RedisConnector(riven.getPlugin(), this.config);
        this.redis.connect();

        this.messenger = new NetworkMessenger(riven.getPlugin(), this.redis, this.serverId);
        this.messenger.start();

        this.registry = new ServerRegistry(riven.getPlugin(), this.redis, this.messenger, this.serverId, this.group, this.mode, this.serverTimeout);
        this.players = new NetworkPlayers(riven.getPlugin(), this.redis, this.serverId);
        this.transfers = new TransferService(riven.getPlugin(), this.redis, this.messenger, this.players, this.serverId);
        this.balancer = new LoadBalancer(this.registry);
        this.drain = new DrainController(riven.getPlugin(), this.registry, this.messenger);

        this.registry.onServerDown(info -> this.players.clearServer(info.getId()));
        this.registry.start();
        this.players.start();
        this.transfers.start();
        this.drain.start();

        if (this.networkParties) {
            final PartyManager parties = riven.getPartyManager();
            final RedisPartyBackend backend = new RedisPartyBackend(this.redis, this.messenger, this.partyTtl);
            final NetworkPartyNotifier notifier = new NetworkPartyNotifier(parties, this.messenger, this.players);

            backend.start();
            notifier.start();

            parties.setBackend(backend);
            parties.setLookup(this.players);
            parties.setNotifier(notifier);
        }

        this.messenger.on(BROADCAST, message -> {
            final JsonObject payload = message.getPayload();
            final String permission = payload.has("permission") ? payload.get("permission").getAsString() : null;
            final JsonArray raw = payload.getAsJsonArray("replacements");
            final Object[] replacements = new Object[raw.size()];

            for (int i = 0; i < raw.size(); i++) {
                replacements[i] = raw.get(i).getAsString();
            }

            for (Player player : Bukkit.getOnlinePlayers()) {
                if (permission == null || player.hasPermission(permission)) {
                    FancyMessage.sendAuto(player, Riven.get().getLanguage().get(player, payload.get("key").getAsString(), replacements));
                }
            }
        });

        riven.getPlugin().getLogger().info("Joined the network as " + this.serverId + " (" + this.mode + ", group " + this.group + ")");
    }

    @Override
    public void disable() {
        final PartyManager parties = Riven.get().getPartyManager();

        parties.setLookup(PlayerLookup.LOCAL);
        parties.setNotifier(null);

        this.transfers.stop();
        this.registry.stop();
        this.players.stop();
        this.messenger.stop();
        this.redis.close();
    }

    /**
     * Sends a language message to every player of every server (like "a game is starting, join with /sw join").
     *
     * @param key language key, may contain fancy blocks
     * @param permission only players with it get it, null for everyone
     * @param replacements placeholder and value pairs
     */
    public void broadcast(String key, String permission, Object... replacements) {
        final JsonObject payload = new JsonObject();
        final JsonArray list = new JsonArray();

        for (Object replacement : replacements) {
            list.add(new JsonPrimitive(String.valueOf(replacement)));
        }

        payload.addProperty("key", key);

        if (permission != null) {
            payload.addProperty("permission", permission);
        }

        payload.add("replacements", list);
        this.messenger.send(BROADCAST, payload, null);
    }

    /**
     * Sends a staff member to whatever server a player is on, with a "spectate" context holding the target's
     * uuid. The plugin's arrive handler puts them in spectator mode.
     *
     * @param staff staff member
     * @param targetName player to watch
     * @return false if the target isn't online anywhere
     */
    public boolean spectate(Player staff, String targetName) {
        final UUID target = this.players.findOnline(targetName);
        final String server = target == null ? null : this.players.getServer(target);

        if (server == null) {
            return false;
        }

        final JsonObject context = new JsonObject();

        context.addProperty(SPECTATE, target.toString());
        this.transfers.transfer(staff, server, context);
        return true;
    }

    public RedisConnector getRedis() {
        return this.redis;
    }

    public NetworkMessenger getMessenger() {
        return this.messenger;
    }

    public ServerRegistry getRegistry() {
        return this.registry;
    }

    public NetworkPlayers getPlayers() {
        return this.players;
    }

    public TransferService getTransfers() {
        return this.transfers;
    }

    public LoadBalancer getBalancer() {
        return this.balancer;
    }

    public DrainController getDrain() {
        return this.drain;
    }

    public String getServerId() {
        return this.serverId;
    }

    public ServerMode getMode() {
        return this.mode;
    }

}
