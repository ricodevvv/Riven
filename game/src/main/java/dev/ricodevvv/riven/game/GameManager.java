package dev.ricodevvv.riven.game;

import dev.ricodevvv.riven.game.listener.GameChatListener;
import dev.ricodevvv.riven.game.listener.GameListener;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Keeps every game running on the server and which one each player is in.
 * That's what makes multi-arena work: every listener asks it "which game is this player in?".
 */
public class GameManager implements Listener {

    private final JavaPlugin plugin;
    private final List<Game<?>> games = new CopyOnWriteArrayList<>();
    private final Map<UUID, Game<?>> playerGames = new ConcurrentHashMap<>();
    private final List<Listener> listeners = new ArrayList<>();
    private boolean autoReconnect = true;

    public GameManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        this.listeners.add(this);
        this.listeners.add(new GameListener(this));
        this.listeners.add(new GameChatListener(this));

        for (Listener listener : this.listeners) {
            Bukkit.getPluginManager().registerEvents(listener, this.plugin);
        }
    }

    public void stop() {
        for (Listener listener : this.listeners) {
            HandlerList.unregisterAll(listener);
        }

        this.listeners.clear();

        for (Game<?> game : new ArrayList<>(this.games)) {
            game.destroy();
        }

        this.games.clear();
        this.playerGames.clear();
    }

    /**
     * Registers a game and leaves it waiting for players.
     *
     * @param game game
     */
    public void register(Game<?> game) {
        if (this.games.contains(game)) {
            return;
        }

        this.games.add(game);

        game.postRegistration();
    }

    void unregister(Game<?> game) {
        this.games.remove(game);
        this.playerGames.values().removeIf(value -> value == game);
    }

    void track(UUID playerId, Game<?> game) {
        final Game<?> old = this.playerGames.put(playerId, game);

        if (old != null && old != game) {
            final Player player = Bukkit.getPlayer(playerId);

            if (player != null) {
                this.playerGames.remove(playerId);
                old.leave(player);
                this.playerGames.put(playerId, game);
            }
        }
    }

    void untrack(UUID playerId, Game<?> game) {
        this.playerGames.remove(playerId, game);
    }

    /**
     * @param player player
     * @return the game they play or watch, or null
     */
    public Game<?> getGame(Player player) {
        return this.playerGames.get(player.getUniqueId());
    }

    public Game<?> getGame(UUID playerId) {
        return this.playerGames.get(playerId);
    }

    /**
     * @param world world
     * @return the game using that world, or null
     */
    public Game<?> getGame(World world) {
        for (Game<?> game : this.games) {
            if (game.getWorld() == world) {
                return game;
            }
        }
        return null;
    }

    /**
     * Finds the game of an entity: by their game if it's a player, otherwise by the world.
     *
     * @param entity entity
     * @return the game or null
     */
    public Game<?> getGame(Entity entity) {
        if (entity instanceof Player) {
            final Game<?> game = this.getGame((Player) entity);

            if (game != null) {
                return game;
            }
        }
        return this.getGame(entity.getWorld());
    }

    /**
     * Finds a running game waiting for this player to come back.
     *
     * @param playerId player uuid
     * @return the game or null
     */
    public Game<?> getReconnectableGame(UUID playerId) {
        for (Game<?> game : this.games) {
            if (game.canReconnect(playerId)) {
                return game;
            }
        }
        return null;
    }

    public List<Game<?>> getGames() {
        return Collections.unmodifiableList(this.games);
    }

    public boolean isAutoReconnect() {
        return this.autoReconnect;
    }

    /**
     * When on, joining the server puts you straight back into the game you left.
     *
     * @param autoReconnect true to turn it on
     */
    public void setAutoReconnect(boolean autoReconnect) {
        this.autoReconnect = autoReconnect;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onJoin(PlayerJoinEvent event) {
        if (!this.autoReconnect) {
            return;
        }

        final Player player = event.getPlayer();
        final Game<?> game = this.getReconnectableGame(player.getUniqueId());

        if (game != null) {
            game.join(player);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onQuit(PlayerQuitEvent event) {
        final Game<?> game = this.getGame(event.getPlayer());

        if (game != null) {
            game.leave(event.getPlayer());
        }
    }

}
