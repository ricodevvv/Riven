package dev.ricodevvv.riven.game.event;

import dev.ricodevvv.riven.game.Game;
import dev.ricodevvv.riven.game.team.GameTeam;
import org.bukkit.event.HandlerList;

/**
 * A team won the game.
 */
public class GameWinEvent extends GameEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final GameTeam winner;

    public GameWinEvent(Game<?> game, GameTeam winner) {
        super(game);
        this.winner = winner;
    }

    public GameTeam getWinner() {
        return this.winner;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

}
