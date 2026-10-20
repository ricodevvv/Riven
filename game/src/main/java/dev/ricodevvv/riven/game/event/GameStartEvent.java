package dev.ricodevvv.riven.game.event;

import dev.ricodevvv.riven.game.Game;
import org.bukkit.event.HandlerList;

/**
 * The game started.
 */
public class GameStartEvent extends GameEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    public GameStartEvent(Game<?> game) {
        super(game);
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

}
