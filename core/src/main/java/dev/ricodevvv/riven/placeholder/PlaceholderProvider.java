package dev.ricodevvv.riven.placeholder;

import org.bukkit.OfflinePlayer;

/**
 * Answers placeholders under one prefix. For prefix "sw" it gets {@code %sw_coins%} as "coins".
 */
@FunctionalInterface
public interface PlaceholderProvider {

    /**
     * @param player player the text is for, can be null
     * @param params everything after the prefix and the underscore
     * @return the value, or null if this provider doesn't know it
     */
    String resolve(OfflinePlayer player, String params);

}
