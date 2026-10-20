package dev.ricodevvv.riven.game.event;

import dev.ricodevvv.riven.game.Game;
import org.bukkit.event.Event;

/**
 * Base of every game event. Since many games run at once, it always carries which one.
 */
public abstract class GameEvent extends Event {

    protected final Game<?> game;

    protected GameEvent(Game<?> game) {
        this.game = game;
    }

    public Game<?> getGame() {
        return this.game;
    }

}
