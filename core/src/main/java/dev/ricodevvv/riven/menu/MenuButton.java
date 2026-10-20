package dev.ricodevvv.riven.menu;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Button made in code but placed from the menu file with {@code button: id}. The file decides the slot,
 * the code decides the item and what happens on click.
 */
public interface MenuButton {

    /**
     * @param player viewer
     * @param menu open menu
     * @param configured the item written in the file, or null if it only says "button"
     * @return the item to show, null to leave the slot empty
     */
    ItemStack render(Player player, ConfigMenu menu, ItemStack configured);

    /**
     * @param player viewer
     * @param menu open menu
     * @param event click event
     */
    void click(Player player, ConfigMenu menu, InventoryClickEvent event);

}
