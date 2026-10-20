package dev.ricodevvv.riven.menu;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.function.Consumer;

/**
 * One element of a paginated list inside a {@link ConfigMenu} (a kit, a cosmetic, a map...).
 */
public class MenuEntry {

    private final ItemStack item;
    private final Consumer<InventoryClickEvent> click;

    public MenuEntry(ItemStack item, Consumer<InventoryClickEvent> click) {
        this.item = item;
        this.click = click;
    }

    public ItemStack getItem() {
        return this.item;
    }

    public Consumer<InventoryClickEvent> getClick() {
        return this.click;
    }

}
