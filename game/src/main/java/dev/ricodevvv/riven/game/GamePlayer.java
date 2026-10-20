package dev.ricodevvv.riven.game;

import dev.ricodevvv.riven.game.event.GameDeathEvent;
import dev.ricodevvv.riven.game.protocol.LastHitterProtocol;
import dev.ricodevvv.riven.game.team.GameTeam;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Player of a game. Port of HyriGamePlayer, but each one knows which game it belongs to.
 */
public class GamePlayer extends GameSpectator {

    private boolean dead;
    private boolean online = true;
    protected long connectionTime = -1;
    protected long disconnectionTime = -1;

    public GamePlayer(Game<?> game, Player player) {
        super(game, player);
    }

    /**
     * @return the name with the team color
     */
    public String formatNameWithTeam() {
        final GameTeam team = this.getTeam();

        return team != null && team.getColor() != null ? team.getColor().getChatColor() + this.name : this.name;
    }

    /**
     * Marks the player as dead and fires {@link GameDeathEvent}.
     *
     * @param reason death cause
     * @param killers last people who hit them, can be null
     * @return the fired event (to read the extra messages)
     */
    public GameDeathEvent setDead(GameDeathEvent.Reason reason, List<LastHitterProtocol.LastHitter> killers) {
        this.dead = true;

        final GameDeathEvent event = new GameDeathEvent(this.game, this, reason, killers);

        Bukkit.getPluginManager().callEvent(event);

        return event;
    }

    /**
     * Marks them dead without events or messages (for example when reconnecting and waiting to respawn).
     */
    public void markDead() {
        this.dead = true;
    }

    public void setNotDead() {
        this.dead = false;
    }

    public boolean isDead() {
        return this.dead;
    }

    @Override
    public boolean isOnline() {
        return this.online && super.isOnline();
    }

    void setOnline(Player player) {
        this.player = player;
        this.online = true;
        this.disconnectionTime = -1;
    }

    void setOffline() {
        this.online = false;
    }

    void initConnectionTime() {
        this.connectionTime = System.currentTimeMillis();
    }

    void initDisconnectionTime() {
        this.disconnectionTime = System.currentTimeMillis();
    }

    /**
     * @param reconnectionTime seconds they have to come back
     * @return true if the reconnection window is over
     */
    public boolean isReconnectionExpired(int reconnectionTime) {
        return this.disconnectionTime != -1 && System.currentTimeMillis() - this.disconnectionTime > reconnectionTime * 1000L;
    }

    /**
     * @return milliseconds played in the game, or -1 if it didn't start
     */
    public long getPlayTime() {
        if (this.connectionTime == -1) {
            return -1;
        }
        return (this.disconnectionTime == -1 ? System.currentTimeMillis() : this.disconnectionTime) - this.connectionTime;
    }

    /**
     * @return the player's team or null
     */
    public GameTeam getTeam() {
        for (GameTeam team : this.game.getTeams()) {
            if (team.contains(this.uniqueId)) {
                return team;
            }
        }
        return null;
    }

    public boolean hasTeam() {
        return this.getTeam() != null;
    }

    public boolean isInTeam(GameTeam team) {
        return team != null && team.contains(this.uniqueId);
    }

    /**
     * Takes the player out of their team, if any.
     */
    public void removeFromTeam() {
        final GameTeam team = this.getTeam();

        if (team != null) {
            team.removePlayer(this);
        }
    }

}
