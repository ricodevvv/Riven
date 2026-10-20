package dev.ricodevvv.riven.game.generator;

import org.bukkit.inventory.ItemStack;

/**
 * What shows on top of the generator (hologram, spinning block...). Gets notified by the generator.
 */
public interface GeneratorHeader {

    /**
     * @return item used for the animation, can be null
     */
    ItemStack getItem();

    default void onCreate(Generator generator) {}

    default void onDrop(Generator generator) {}

    default void onUpgrade(Generator generator) {}

    default void onRemove(Generator generator) {}

}
