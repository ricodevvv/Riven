package dev.ricodevvv.riven.game.team;

import dev.ricodevvv.riven.game.GameFramework;
import dev.ricodevvv.riven.game.Game;
import dev.ricodevvv.riven.game.GamePlayer;
import dev.ricodevvv.riven.game.event.GamePlayerJoinTeamEvent;
import dev.ricodevvv.riven.game.event.GamePlayerLeaveTeamEvent;
import dev.ricodevvv.riven.scoreboard.NameTagVisibility;
import dev.ricodevvv.riven.util.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * Team of a game. Port of HyriGameTeam: keeps its players, color and size.
 */
public class GameTeam {

    protected final Game<?> game;
    protected final String name;
    protected final String displayKey;
    protected final Map<UUID, GamePlayer> players = new LinkedHashMap<>();

    protected TeamColor color;
    protected boolean friendlyFire;
    protected NameTagVisibility nameTagVisibility;
    protected int teamSize;

    /**
     * @param game owner game
     * @param name internal name (unique in the game)
     * @param displayKey language key of the name, or a fixed text if the key doesn't exist
     * @param color color, can be null for one-player teams
     * @param teamSize max players
     */
    public GameTeam(Game<?> game, String name, String displayKey, TeamColor color, int teamSize) {
        this.game = game;
        this.name = name;
        this.displayKey = displayKey;
        this.color = color;
        this.teamSize = teamSize;
        this.nameTagVisibility = NameTagVisibility.ALWAYS;
    }

    /**
     * Adds a player to the team.
     *
     * @param player player
     * @return true if they got in
     */
    public boolean addPlayer(GamePlayer player) {
        if (player.hasTeam() || this.isFull()) {
            return false;
        }

        this.players.put(player.getUniqueId(), player);

        this.game.updateTabPlayer(player);

        Bukkit.getPluginManager().callEvent(new GamePlayerJoinTeamEvent(this.game, this, player));
        return true;
    }

    /**
     * Removes a player from the team.
     *
     * @param player player
     * @return true if they were in and got removed
     */
    public boolean removePlayer(GamePlayer player) {
        if (this.players.remove(player.getUniqueId()) == null) {
            return false;
        }

        this.game.updateTabPlayer(player);

        Bukkit.getPluginManager().callEvent(new GamePlayerLeaveTeamEvent(this.game, this, player));
        return true;
    }

    /**
     * Teleports every connected player of the team.
     *
     * @param location destination
     */
    public void teleport(Location location) {
        for (GamePlayer player : this.getOnlinePlayers()) {
            player.getPlayer().teleport(location);
        }
    }

    /**
     * Sends a per-player message to the whole team.
     *
     * @param message message per player
     */
    public void sendMessage(Function<Player, String> message) {
        for (GamePlayer player : this.getOnlinePlayers()) {
            player.getPlayer().sendMessage(message.apply(player.getPlayer()));
        }
    }

    /**
     * Sends a title to the whole team.
     *
     * @param title title per player
     * @param subtitle subtitle per player
     * @param fadeIn fade in ticks
     * @param stay ticks it stays
     * @param fadeOut fade out ticks
     */
    public void sendTitle(Function<Player, String> title, Function<Player, String> subtitle, int fadeIn, int stay, int fadeOut) {
        for (GamePlayer player : this.getOnlinePlayers()) {
            Title.sendTitle(player.getPlayer(), title.apply(player.getPlayer()), subtitle.apply(player.getPlayer()), fadeIn, stay, fadeOut);
        }
    }

    public boolean contains(UUID playerId) {
        return this.players.containsKey(playerId);
    }

    public boolean contains(GamePlayer player) {
        return this.contains(player.getUniqueId());
    }

    public boolean isFull() {
        return this.players.size() >= this.teamSize;
    }

    public GamePlayer getPlayer(UUID playerId) {
        return this.players.get(playerId);
    }

    public Collection<GamePlayer> getPlayers() {
        return Collections.unmodifiableCollection(this.players.values());
    }

    /**
     * @return connected players that aren't spectating
     */
    public List<GamePlayer> getPlayersPlaying() {
        final List<GamePlayer> result = new ArrayList<>();

        for (GamePlayer player : this.players.values()) {
            if (player.isOnline() && !player.isSpectator()) {
                result.add(player);
            }
        }
        return result;
    }

    public boolean hasPlayersPlaying() {
        return !this.getPlayersPlaying().isEmpty();
    }

    /**
     * @return connected players of the team
     */
    public List<GamePlayer> getOnlinePlayers() {
        final List<GamePlayer> result = new ArrayList<>();

        for (GamePlayer player : this.players.values()) {
            if (player.isOnline()) {
                result.add(player);
            }
        }
        return result;
    }

    /**
     * @param target player reading it
     * @return the team name in their language
     */
    public String getDisplayName(Player target) {
        if (this.displayKey == null) {
            return this.name;
        }
        return GameFramework.get().getLanguage().has(this.displayKey) ? GameFramework.get().getLanguage().get(target, this.displayKey) : this.displayKey;
    }

    /**
     * @param target player reading it
     * @return the team name with its color
     */
    public String getFormattedDisplayName(Player target) {
        return (this.color != null ? this.color.getChatColor().toString() : "") + this.getDisplayName(target);
    }

    public Game<?> getGame() {
        return this.game;
    }

    public String getName() {
        return this.name;
    }

    public TeamColor getColor() {
        return this.color;
    }

    public void setColor(TeamColor color) {
        this.color = color;
        this.game.updateTabTeam(this);
    }

    public boolean isFriendlyFire() {
        return this.friendlyFire;
    }

    public void setFriendlyFire(boolean friendlyFire) {
        this.friendlyFire = friendlyFire;
    }

    public NameTagVisibility getNameTagVisibility() {
        return this.nameTagVisibility;
    }

    public void setNameTagVisibility(NameTagVisibility nameTagVisibility) {
        this.nameTagVisibility = nameTagVisibility;
        this.game.updateTabTeam(this);
    }

    public int getTeamSize() {
        return this.teamSize;
    }

    public void setTeamSize(int teamSize) {
        this.teamSize = teamSize;
    }

}
