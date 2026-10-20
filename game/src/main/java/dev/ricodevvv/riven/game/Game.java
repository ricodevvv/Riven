package dev.ricodevvv.riven.game;

import dev.ricodevvv.riven.game.GameFramework;
import dev.ricodevvv.riven.game.event.GameJoinEvent;
import dev.ricodevvv.riven.game.event.GameLeaveEvent;
import dev.ricodevvv.riven.game.event.GameReconnectEvent;
import dev.ricodevvv.riven.game.event.GameReconnectedEvent;
import dev.ricodevvv.riven.game.event.GameSpectatorEvent;
import dev.ricodevvv.riven.game.event.GameStartEvent;
import dev.ricodevvv.riven.game.event.GameStateChangedEvent;
import dev.ricodevvv.riven.game.event.GameTeamRegisteredEvent;
import dev.ricodevvv.riven.game.event.GameWinEvent;
import dev.ricodevvv.riven.game.protocol.ProtocolManager;
import dev.ricodevvv.riven.game.protocol.SpectatorProtocol;
import dev.ricodevvv.riven.game.protocol.WaitingProtocol;
import dev.ricodevvv.riven.game.protocol.WinProtocol;
import dev.ricodevvv.riven.game.team.GameTeam;
import dev.ricodevvv.riven.game.timer.GameStartingTimer;
import dev.ricodevvv.riven.game.timer.GameTimer;
import dev.ricodevvv.riven.game.util.GameMessages;
import dev.ricodevvv.riven.game.waitingroom.WaitingRoom;
import dev.ricodevvv.riven.language.LanguageManager;
import dev.ricodevvv.riven.scoreboard.NameTagTeam;
import dev.ricodevvv.riven.scoreboard.NameTagTeamHandler;
import dev.ricodevvv.riven.scoreboard.NameTagVisibility;
import dev.ricodevvv.riven.util.PlayerUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

/**
 * A game. Port of Hyrame's HyriGame with one big change: Hyriode ran one game per server, here every
 * arena has its own, so nothing is global. Each game has its world, its players, its tab teams and its
 * protocols, and the listeners filter by game.
 *
 * @param <P> player type of the game
 */
public abstract class Game<P extends GamePlayer> {

    protected static final String SPECTATORS_TEAM = "zspectators";
    protected static final String DEFAULT_TEAM = "zdefault";

    protected final JavaPlugin plugin;
    protected final GameFramework framework;
    protected final String name;
    protected final String displayName;
    protected final GameType type;
    protected final World world;

    protected final List<GameTeam> teams = new CopyOnWriteArrayList<>();
    protected final List<P> players = new CopyOnWriteArrayList<>();
    protected final List<GameSpectator> spectators = new CopyOnWriteArrayList<>();
    protected final ProtocolManager protocolManager;
    protected final NameTagTeamHandler nameTagHandler = new NameTagTeamHandler();

    protected GameStartingTimer startingTimer;
    protected GameTimer timer;
    protected WaitingRoom waitingRoom;

    protected String mapName;
    protected String descriptionKey;
    protected boolean defaultStarting = true;
    protected boolean usingGameTabList = true;
    protected boolean usingTeams = true;
    protected boolean spectatorsAllowed = true;
    protected boolean teamSelector = true;
    protected int reconnectionTime = -1;
    protected int startingTime = 30;
    protected int endDuration = 10;
    protected int minPlayers;
    protected int maxPlayers;

    private GameState state = GameState.WAITING;
    private BukkitTask endTask;
    private boolean destroyed;

    /**
     * @param plugin owner plugin
     * @param name internal game name (for example "bedwars")
     * @param displayName name shown to players
     * @param type game type
     * @param world world the game is played in
     */
    public Game(JavaPlugin plugin, String name, String displayName, GameType type, World world) {
        this.plugin = plugin;
        this.framework = GameFramework.get();
        this.name = name;
        this.displayName = displayName;
        this.type = type;
        this.world = world;
        this.mapName = world.getName();
        this.minPlayers = type.getMinPlayers();
        this.maxPlayers = type.getMaxPlayers();
        this.protocolManager = new ProtocolManager(plugin, this);
    }

    /**
     * Creates the game's own player object.
     *
     * @param player bukkit player
     * @return the game player
     */
    protected abstract P createPlayer(Player player);

    /**
     * Called when the game gets registered in the {@link GameManager}: starts the waiting phase, the room and the tab teams.
     */
    public void postRegistration() {
        if (this.usingGameTabList) {
            this.nameTagHandler.addTeam(new NameTagTeam(DEFAULT_TEAM, ChatColor.GRAY.toString(), "", NameTagVisibility.ALWAYS));
            this.nameTagHandler.addTeam(new NameTagTeam(SPECTATORS_TEAM, ChatColor.GRAY.toString(), "", NameTagVisibility.NEVER));
        }

        if (this.defaultStarting) {
            this.startingTimer = new GameStartingTimer(this, this.startingTime);
            this.startingTimer.runTaskTimer(this.plugin, 20L, 20L);

            this.protocolManager.enableProtocol(new WaitingProtocol(this, this.teamSelector));
        }

        this.protocolManager.enableProtocol(new SpectatorProtocol(this));
        this.protocolManager.enableProtocol(new WinProtocol(this));

        if (this.waitingRoom != null) {
            this.waitingRoom.setup();
        }
    }

    /**
     * Adds a player. If they were already in (they had disconnected) it tries to reconnect them.
     *
     * @param player player
     * @return true if they got in
     */
    public boolean join(Player player) {
        return this.join(player, false);
    }

    /**
     * Adds a player, optionally without announcing it or sending them the game description.
     *
     * @param player player
     * @param silent true to keep it quiet
     * @return true if they got in
     */
    public boolean join(Player player, boolean silent) {
        if (this.destroyed) {
            return false;
        }

        final P existing = this.getPlayer(player.getUniqueId());

        if (existing != null) {
            return this.reconnect(existing, player);
        }

        if (!this.state.isAccessible() || this.isFull()) {
            return false;
        }

        final P gamePlayer = this.createPlayer(player);

        this.players.add(gamePlayer);
        this.framework.getGameManager().track(player.getUniqueId(), this);

        if (!this.usingTeams) {
            final GameTeam team = new GameTeam(this, player.getName(), player.getName(), null, 1);

            this.registerTeam(team);
            team.addPlayer(gamePlayer);
        }

        if (this.usingGameTabList) {
            this.nameTagHandler.addReceiver(player);
            this.updateTabPlayer(gamePlayer);
        }

        if (this.descriptionKey != null && !silent) {
            player.spigot().sendMessage(GameMessages.createDescription(this, player));
        }

        Bukkit.getPluginManager().callEvent(new GameJoinEvent(this, gamePlayer, silent));
        return true;
    }

    /**
     * Tries to reconnect a player who left the running game.
     *
     * @param gamePlayer game player
     * @param player bukkit player that came back
     * @return true if they got back in
     */
    protected boolean reconnect(P gamePlayer, Player player) {
        if (!this.canReconnect(gamePlayer.getUniqueId())) {
            return false;
        }

        final GameReconnectEvent event = new GameReconnectEvent(this, gamePlayer);

        Bukkit.getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            return false;
        }

        gamePlayer.setOnline(player);

        this.framework.getGameManager().track(player.getUniqueId(), this);

        if (this.usingGameTabList) {
            this.nameTagHandler.addReceiver(player);
            this.updateTabPlayer(gamePlayer);
        }

        if (!gamePlayer.isSpectator()) {
            this.broadcast(target -> this.lang().get(target, "game.reconnected").replace("%player%", gamePlayer.formatNameWithTeam()));
        }

        Bukkit.getPluginManager().callEvent(new GameReconnectedEvent(this, gamePlayer));
        return true;
    }

    /**
     * @param playerId player uuid
     * @return true if it's a disconnected player of this game that can still come back
     */
    public boolean canReconnect(UUID playerId) {
        final P gamePlayer = this.getPlayer(playerId);

        return gamePlayer != null
                && !gamePlayer.isOnline()
                && this.isReconnectionAllowed()
                && this.state == GameState.PLAYING
                && !gamePlayer.isReconnectionExpired(this.reconnectionTime);
    }

    /**
     * Removes a player or spectator from the game (they disconnected or left).
     *
     * @param player player
     */
    public void leave(Player player) {
        final GameSpectator spectator = this.getSpectator(player.getUniqueId());

        if (spectator != null) {
            this.removeSpectator(player);
            return;
        }

        final P gamePlayer = this.getPlayer(player.getUniqueId());

        if (gamePlayer == null || !gamePlayer.isOnline()) {
            return;
        }

        this.framework.getGameManager().untrack(player.getUniqueId(), this);
        this.framework.getScoreboardManager().removeSidebar(player);

        if (this.usingGameTabList) {
            this.nameTagHandler.removeReceiver(player);
        }

        gamePlayer.setOffline();

        if (this.state.isAccessible()) {
            this.players.remove(gamePlayer);

            final GameTeam team = gamePlayer.getTeam();

            if (team != null) {
                team.removePlayer(gamePlayer);

                if (!this.usingTeams) {
                    this.teams.remove(team);
                }
            }

            this.nameTagHandler.removePlayer(gamePlayer.getName());
        } else if (this.state == GameState.PLAYING) {
            gamePlayer.initDisconnectionTime();

            if (!gamePlayer.isSpectator()) {
                this.broadcast(target -> this.lang().get(target, "game.disconnected").replace("%player%", gamePlayer.formatNameWithTeam()));
            }
        }

        Bukkit.getPluginManager().callEvent(new GameLeaveEvent(this, gamePlayer));
    }

    /**
     * Adds an outsider as a spectator.
     *
     * @param player player
     * @return true if it worked
     */
    public boolean addSpectator(Player player) {
        if (this.destroyed || !this.spectatorsAllowed || this.getSpectator(player.getUniqueId()) != null) {
            return false;
        }

        final GameSpectator spectator = new GameSpectator(this, player);

        this.spectators.add(spectator);
        this.framework.getGameManager().track(player.getUniqueId(), this);

        if (this.usingGameTabList) {
            this.nameTagHandler.addReceiver(player);
            this.nameTagHandler.setPlayerTeam(player.getName(), SPECTATORS_TEAM);
        }

        spectator.setSpectator(true);
        return true;
    }

    /**
     * Removes an outside spectator.
     *
     * @param player player
     */
    public void removeSpectator(Player player) {
        final GameSpectator spectator = this.getSpectator(player.getUniqueId());

        if (spectator == null) {
            return;
        }

        this.spectators.remove(spectator);
        this.framework.getGameManager().untrack(player.getUniqueId(), this);
        this.framework.getScoreboardManager().removeSidebar(player);

        if (this.usingGameTabList) {
            this.nameTagHandler.removePlayer(player.getName());
            this.nameTagHandler.removeReceiver(player);
        }

        Bukkit.getPluginManager().callEvent(new GameSpectatorEvent(this, spectator, GameSpectatorEvent.Action.REMOVE));
    }

    /**
     * Starts the game.
     */
    public void start() {
        if (this.state == GameState.PLAYING || this.state == GameState.ENDED) {
            return;
        }

        if (this.defaultStarting) {
            this.protocolManager.disableProtocol(WaitingProtocol.class);

            if (this.startingTimer != null) {
                this.startingTimer.cancel();
            }
        }

        if (this.waitingRoom != null) {
            this.waitingRoom.remove();
        }

        this.setState(GameState.PLAYING);

        this.timer = new GameTimer();
        this.timer.runTaskTimer(this.plugin, 20L, 20L);

        this.giveRandomTeams();

        for (P gamePlayer : this.players) {
            gamePlayer.initConnectionTime();

            if (gamePlayer.isOnline()) {
                PlayerUtil.resetPlayer(gamePlayer.getPlayer(), true);
            }
        }

        Bukkit.getPluginManager().callEvent(new GameStartEvent(this));
    }

    /**
     * Declares a winner and ends the game.
     *
     * @param winner winning team
     */
    public void win(GameTeam winner) {
        if (winner == null || this.state == GameState.ENDED) {
            return;
        }

        final NameTagTeam spectatorsTeam = this.nameTagHandler.getTeam(SPECTATORS_TEAM);

        if (spectatorsTeam != null) {
            spectatorsTeam.setNameTagVisibility(NameTagVisibility.ALWAYS);
            this.nameTagHandler.updateTeam(SPECTATORS_TEAM);
        }

        this.end();

        Bukkit.getPluginManager().callEvent(new GameWinEvent(this, winner));
    }

    /**
     * Ends the game. After {@link #endDuration} seconds it calls {@link #onEnded()}.
     */
    public void end() {
        if (this.state == GameState.ENDED) {
            return;
        }

        this.setState(GameState.ENDED);

        if (this.timer != null) {
            this.timer.cancel();
        }

        this.endTask = Bukkit.getScheduler().runTaskLater(this.plugin, this::onEnded, this.endDuration * 20L);
    }

    /**
     * What to do when the celebration time is over. Nothing by default; BedWars restarts the arena here.
     */
    protected void onEnded() {}

    /**
     * Tears the game down completely. The object is useless afterwards.
     */
    public void destroy() {
        if (this.destroyed) {
            return;
        }

        this.destroyed = true;

        if (this.startingTimer != null) {
            try {
                this.startingTimer.cancel();
            } catch (IllegalStateException ignored) {
            }
        }

        if (this.timer != null) {
            try {
                this.timer.cancel();
            } catch (IllegalStateException ignored) {
            }
        }

        if (this.endTask != null) {
            this.endTask.cancel();
        }

        this.protocolManager.disable();

        if (this.waitingRoom != null && this.waitingRoom.isSetup()) {
            this.waitingRoom.remove();
        }

        for (Player viewer : this.getViewers()) {
            this.framework.getScoreboardManager().removeSidebar(viewer);
            this.framework.getGameManager().untrack(viewer.getUniqueId(), this);
        }

        this.nameTagHandler.destroy();
        this.players.clear();
        this.spectators.clear();
        this.teams.clear();

        this.framework.getGameManager().unregister(this);
    }

    /**
     * Registers a team.
     *
     * @param team team
     * @return the same team
     */
    public GameTeam registerTeam(GameTeam team) {
        if (this.getTeam(team.getName()) != null) {
            throw new IllegalStateException("There's already a team called " + team.getName());
        }

        this.teams.add(team);

        if (this.usingGameTabList) {
            this.nameTagHandler.addTeam(new NameTagTeam(team.getName(), team.getColor() != null ? team.getColor().getChatColor().toString() : "", "", team.getNameTagVisibility()));
        }

        Bukkit.getPluginManager().callEvent(new GameTeamRegisteredEvent(this, team));
        return team;
    }

    /**
     * Re-sends a team's prefix and visibility to the tab.
     *
     * @param team team
     */
    public void updateTabTeam(GameTeam team) {
        final NameTagTeam tabTeam = this.nameTagHandler.getTeam(team.getName());

        if (tabTeam == null) {
            return;
        }

        tabTeam.setPrefix(team.getColor() != null ? team.getColor().getChatColor().toString() : "");
        tabTeam.setNameTagVisibility(team.getNameTagVisibility());

        this.nameTagHandler.updateTeam(team.getName());
    }

    /**
     * Puts the player in the right tab team.
     *
     * @param gamePlayer player
     */
    public void updateTabPlayer(GamePlayer gamePlayer) {
        if (!this.usingGameTabList) {
            return;
        }

        final GameTeam team = gamePlayer.getTeam();

        this.nameTagHandler.setPlayerTeam(gamePlayer.getName(), team == null ? DEFAULT_TEAM : team.getName());
    }

    /**
     * Starts the countdown again (in case the old one got cancelled, for example while another map loads).
     *
     * @param time seconds
     */
    public void restartStartingTimer(int time) {
        if (this.startingTimer != null) {
            try {
                this.startingTimer.cancel();
            } catch (IllegalStateException ignored) {
            }
        }

        final GameStartingTimer old = this.startingTimer;

        this.startingTimer = new GameStartingTimer(this, this.startingTime);
        this.startingTimer.shortenTo(time);
        this.startingTimer.runTaskTimer(this.plugin, 20L, 20L);

        if (old != null) {
            old.transferListeners(this.startingTimer);
        }
    }

    /**
     * Stops the countdown without starting the game.
     */
    public void pauseStartingTimer() {
        if (this.startingTimer != null) {
            try {
                this.startingTimer.cancel();
            } catch (IllegalStateException ignored) {
            }
        }
    }

    /**
     * Puts everyone who didn't pick a team into the emptiest one.
     */
    public void giveRandomTeams() {
        final List<P> withoutTeam = new ArrayList<>();

        for (P player : this.players) {
            if (!player.hasTeam()) {
                withoutTeam.add(player);
            }
        }

        Collections.shuffle(withoutTeam);

        for (P player : withoutTeam) {
            final GameTeam team = this.getSmallestTeam();

            if (team != null) {
                team.addPlayer(player);
            }
        }
    }

    /**
     * Sends a per-player message to every connected player and spectator.
     *
     * @param message message per player
     */
    public void broadcast(Function<Player, String> message) {
        for (Player player : this.getViewers()) {
            player.sendMessage(message.apply(player));
        }
    }

    /**
     * Sends a language file message to the whole game.
     *
     * @param key message key
     */
    public void broadcastMessage(String key) {
        this.broadcast(player -> this.lang().get(player, key));
    }

    /**
     * Sends a message only to the people watching (outside spectators and dead players).
     *
     * @param message message per player
     */
    public void sendMessageToSpectators(Function<Player, String> message) {
        for (Player player : this.getViewers()) {
            final P gamePlayer = this.getPlayer(player.getUniqueId());

            if (gamePlayer == null || gamePlayer.isSpectator()) {
                player.sendMessage(this.lang().get(player, "game.spectators.chat").replace("%message%", message.apply(player)));
            }
        }
    }

    /**
     * @return everyone with the game on screen: connected players and spectators
     */
    public List<Player> getViewers() {
        final List<Player> viewers = new ArrayList<>();

        for (P gamePlayer : this.players) {
            if (gamePlayer.isOnline()) {
                viewers.add(gamePlayer.getPlayer());
            }
        }

        for (GameSpectator spectator : this.spectators) {
            if (spectator.isOnline()) {
                viewers.add(spectator.getPlayer());
            }
        }
        return viewers;
    }

    /**
     * @param player player
     * @return true if they're a (connected) player or a spectator of this game
     */
    public boolean contains(Player player) {
        final P gamePlayer = this.getPlayer(player.getUniqueId());

        return (gamePlayer != null && gamePlayer.isOnline()) || this.getSpectator(player.getUniqueId()) != null;
    }

    public P getPlayer(UUID uuid) {
        for (P player : this.players) {
            if (player.getUniqueId().equals(uuid)) {
                return player;
            }
        }
        return null;
    }

    public P getPlayer(Player player) {
        return this.getPlayer(player.getUniqueId());
    }

    public GameSpectator getSpectator(UUID uuid) {
        for (GameSpectator spectator : this.spectators) {
            if (spectator.getUniqueId().equals(uuid)) {
                return spectator;
            }
        }
        return null;
    }

    public List<GameSpectator> getSpectators() {
        return this.spectators;
    }

    public List<P> getPlayers() {
        return this.players;
    }

    /**
     * @return connected players
     */
    public List<P> getOnlinePlayers() {
        final List<P> result = new ArrayList<>();

        for (P player : this.players) {
            if (player.isOnline()) {
                result.add(player);
            }
        }
        return result;
    }

    public GameTeam getTeam(String name) {
        for (GameTeam team : this.teams) {
            if (team.getName().equalsIgnoreCase(name)) {
                return team;
            }
        }
        return null;
    }

    public List<GameTeam> getTeams() {
        return this.teams;
    }

    /**
     * @return the emptiest team that isn't full, or null if they're all full
     */
    public GameTeam getSmallestTeam() {
        GameTeam result = null;

        for (GameTeam team : this.teams) {
            if (!team.isFull() && (result == null || team.getPlayers().size() < result.getPlayers().size())) {
                result = team;
            }
        }
        return result;
    }

    /**
     * @param first a player
     * @param second another player
     * @return true if they're on the same team
     */
    public boolean areInSameTeam(Player first, Player second) {
        final P firstPlayer = this.getPlayer(first.getUniqueId());
        final P secondPlayer = this.getPlayer(second.getUniqueId());

        return firstPlayer != null && secondPlayer != null && firstPlayer.getTeam() != null && firstPlayer.getTeam() == secondPlayer.getTeam();
    }

    /**
     * @return true if there are enough people to start
     */
    public boolean canStart() {
        return this.players.size() >= Math.min(this.minPlayers, this.maxPlayers);
    }

    public boolean isFull() {
        return this.players.size() >= this.maxPlayers;
    }

    public GameState getState() {
        return this.state;
    }

    /**
     * Changes the state and fires {@link GameStateChangedEvent}.
     *
     * @param state new state
     */
    public void setState(GameState state) {
        if (this.state == state) {
            return;
        }

        final GameState oldState = this.state;

        this.state = state;

        Bukkit.getPluginManager().callEvent(new GameStateChangedEvent(this, oldState, state));
    }

    protected LanguageManager lang() {
        return this.framework.getLanguage();
    }

    public JavaPlugin getPlugin() {
        return this.plugin;
    }

    public String getName() {
        return this.name;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public GameType getType() {
        return this.type;
    }

    public World getWorld() {
        return this.world;
    }

    public String getMapName() {
        return this.mapName;
    }

    public String getDescriptionKey() {
        return this.descriptionKey;
    }

    public ProtocolManager getProtocolManager() {
        return this.protocolManager;
    }

    public NameTagTeamHandler getNameTagHandler() {
        return this.nameTagHandler;
    }

    public GameStartingTimer getStartingTimer() {
        return this.startingTimer;
    }

    public GameTimer getTimer() {
        return this.timer;
    }

    public WaitingRoom getWaitingRoom() {
        return this.waitingRoom;
    }

    public boolean isUsingTeams() {
        return this.usingTeams;
    }

    public boolean isUsingGameTabList() {
        return this.usingGameTabList;
    }

    public boolean isSpectatorsAllowed() {
        return this.spectatorsAllowed;
    }

    public void setSpectatorsAllowed(boolean spectatorsAllowed) {
        this.spectatorsAllowed = spectatorsAllowed;
    }

    public boolean isTeamSelector() {
        return this.teamSelector;
    }

    public int getReconnectionTime() {
        return this.reconnectionTime;
    }

    public boolean isReconnectionAllowed() {
        return this.reconnectionTime > 0;
    }

    public int getMinPlayers() {
        return this.minPlayers;
    }

    public int getMaxPlayers() {
        return this.maxPlayers;
    }

    public boolean isDestroyed() {
        return this.destroyed;
    }

}
