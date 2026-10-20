package dev.ricodevvv.riven.game.event;

import dev.ricodevvv.riven.game.Game;
import dev.ricodevvv.riven.game.GameSpectator;
import org.bukkit.event.HandlerList;

/**
 * Someone became a spectator or stopped being one.
 */
public class GameSpectatorEvent extends GameEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final GameSpectator spectator;
    private final Action action;

    public GameSpectatorEvent(Game<?> game, GameSpectator spectator, Action action) {
        super(game);
        this.spectator = spectator;
        this.action = action;
    }

    public GameSpectator getSpectator() {
        return this.spectator;
    }

    public Action getAction() {
        return this.action;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    public enum Action {
        ADD,
        REMOVE
    }

}
