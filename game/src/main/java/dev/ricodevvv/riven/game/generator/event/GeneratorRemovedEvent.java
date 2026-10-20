package dev.ricodevvv.riven.game.generator.event;

import dev.ricodevvv.riven.game.generator.Generator;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired when a generator gets removed (for example when the game ends).
 */
public class GeneratorRemovedEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Generator generator;

    public GeneratorRemovedEvent(Generator generator) {
        this.generator = generator;
    }

    public Generator getGenerator() {
        return this.generator;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

}
