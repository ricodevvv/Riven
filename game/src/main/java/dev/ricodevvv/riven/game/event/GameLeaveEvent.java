package dev.ricodevvv.riven.game.event;

import dev.ricodevvv.riven.game.Game;
import dev.ricodevvv.riven.game.GamePlayer;
import org.bukkit.event.HandlerList;

/**
 * A player left the game (disconnected or quit).
 */
public class GameLeaveEvent extends GameEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final GamePlayer gamePlayer;

    public GameLeaveEvent(Game<?> game, GamePlayer gamePlayer) {
        super(game);
        this.gamePlayer = gamePlayer;
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
