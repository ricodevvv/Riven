package dev.ricodevvv.riven.game.generator.event;

import dev.ricodevvv.riven.game.generator.Generator;
import dev.ricodevvv.riven.game.generator.GeneratorTier;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired when a generator gets upgraded.
 */
public class GeneratorUpgradedEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Generator generator;
    private final GeneratorTier tier;
    private boolean cancelled;

    public GeneratorUpgradedEvent(Generator generator, GeneratorTier tier) {
        this.generator = generator;
        this.tier = tier;
    }

    public Generator getGenerator() {
        return this.generator;
    }

    public GeneratorTier getTier() {
        return this.tier;
    }

    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

}
