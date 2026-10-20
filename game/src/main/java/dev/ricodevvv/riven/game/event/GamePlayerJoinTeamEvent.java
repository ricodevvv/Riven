package dev.ricodevvv.riven.game.event;

import dev.ricodevvv.riven.game.Game;
import dev.ricodevvv.riven.game.GamePlayer;
import dev.ricodevvv.riven.game.team.GameTeam;
import org.bukkit.event.HandlerList;

/**
 * A player joined a team.
 */
public class GamePlayerJoinTeamEvent extends GameEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final GameTeam team;
    private final GamePlayer gamePlayer;

    public GamePlayerJoinTeamEvent(Game<?> game, GameTeam team, GamePlayer gamePlayer) {
        super(game);
        this.team = team;
        this.gamePlayer = gamePlayer;
    }

    public GameTeam getTeam() {
        return this.team;
    }

    public GamePlayer getGamePlayer() {
        return this.gamePlayer;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

}
