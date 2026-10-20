package dev.ricodevvv.riven.game.event;

import dev.ricodevvv.riven.game.Game;
import dev.ricodevvv.riven.game.team.GameTeam;
import org.bukkit.event.HandlerList;

/**
 * A team got registered in the game.
 */
public class GameTeamRegisteredEvent extends GameEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final GameTeam team;

    public GameTeamRegisteredEvent(Game<?> game, GameTeam team) {
        super(game);
        this.team = team;
    }

    public GameTeam getTeam() {
        return this.team;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

}
