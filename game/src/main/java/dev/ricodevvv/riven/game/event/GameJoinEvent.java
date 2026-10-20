package dev.ricodevvv.riven.game.event;

import dev.ricodevvv.riven.game.Game;
import dev.ricodevvv.riven.game.GamePlayer;
import org.bukkit.event.HandlerList;

/**
 * A player joined the game as a player.
 */
public class GameJoinEvent extends GameEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final GamePlayer gamePlayer;
    private final boolean silent;

    public GameJoinEvent(Game<?> game, GamePlayer gamePlayer, boolean silent) {
        super(game);
        this.gamePlayer = gamePlayer;
        this.silent = silent;
    }

    public GamePlayer getGamePlayer() {
        return this.gamePlayer;
    }

    /**
     * @return true if the join shouldn't be announced (for example when the map changes after a vote)
     */
    public boolean isSilent() {
        return this.silent;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

}
