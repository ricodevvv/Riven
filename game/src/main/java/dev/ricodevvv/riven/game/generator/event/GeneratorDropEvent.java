package dev.ricodevvv.riven.game.generator.event;

import dev.ricodevvv.riven.game.generator.Generator;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

/**
 * Fired right before a generator drops an item.
 */
public class GeneratorDropEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Generator generator;
    private ItemStack item;
    private boolean cancelled;

    public GeneratorDropEvent(Generator generator, ItemStack item) {
        this.generator = generator;
        this.item = item;
    }

    public Generator getGenerator() {
        return this.generator;
    }

    public ItemStack getItem() {
        return this.item;
    }

    public void setItem(ItemStack item) {
        this.item = item;
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
