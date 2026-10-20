package dev.ricodevvv.riven.game.event;

import dev.ricodevvv.riven.game.Game;
import dev.ricodevvv.riven.game.GameState;
import org.bukkit.event.HandlerList;

/**
 * The game changed state.
 */
public class GameStateChangedEvent extends GameEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final GameState oldState;
    private final GameState newState;

    public GameStateChangedEvent(Game<?> game, GameState oldState, GameState newState) {
        super(game);
        this.oldState = oldState;
        this.newState = newState;
    }

    public GameState getOldState() {
        return this.oldState;
    }

    public GameState getNewState() {
        return this.newState;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

}
