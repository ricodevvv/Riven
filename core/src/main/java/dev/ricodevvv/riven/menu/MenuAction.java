package dev.ricodevvv.riven.menu;

import org.bukkit.entity.Player;

/**
 * A custom {@code [name] argument} action for menu files.
 */
@FunctionalInterface
public interface MenuAction {

    /**
     * @param player player who clicked
     * @param menu open menu, can be null for open-actions run before it's built
     * @param argument everything after the tag, placeholders already applied
     */
    void run(Player player, ConfigMenu menu, String argument);

}
