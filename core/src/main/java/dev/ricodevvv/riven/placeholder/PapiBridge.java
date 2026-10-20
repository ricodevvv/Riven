package dev.ricodevvv.riven.placeholder;

import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Everything that touches PlaceholderAPI classes lives here, so nothing loads them when it isn't installed.
 */
final class PapiBridge {

    private PapiBridge() {}

    static Object register(JavaPlugin plugin, String prefix, PlaceholderProvider provider) {
        final PlaceholderExpansion expansion = new PlaceholderExpansion() {
            @Override
            public String getIdentifier() {
                return prefix;
            }

            @Override
            public String getAuthor() {
                return String.join(", ", plugin.getDescription().getAuthors());
            }

            @Override
            public String getVersion() {
                return plugin.getDescription().getVersion();
            }

            @Override
            public boolean persist() {
                return true;
            }

            @Override
            public String onRequest(OfflinePlayer player, String params) {
                return provider.resolve(player, params);
            }
        };

        expansion.register();
        return expansion;
    }

    static void unregister(Object expansion) {
        try {
            ((PlaceholderExpansion) expansion).unregister();
        } catch (Throwable ignored) {
        }
    }

    static String apply(OfflinePlayer player, String text) {
        return PlaceholderAPI.setPlaceholders(player, text);
    }

}
