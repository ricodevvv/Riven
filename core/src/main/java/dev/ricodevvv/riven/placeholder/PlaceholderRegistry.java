package dev.ricodevvv.riven.placeholder;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Placeholders that work with or without PlaceholderAPI. Register a provider per prefix and Riven resolves
 * {@code %prefix_params%} in menus, scoreboards and messages. If PlaceholderAPI is installed, every prefix is
 * also exported as a PlaceholderAPI expansion, and {@link #apply(Player, String)} runs PlaceholderAPI too.
 */
public class PlaceholderRegistry {

    private static final Pattern PLACEHOLDER = Pattern.compile("%([a-zA-Z0-9]+)_([^%]+)%");

    private final JavaPlugin plugin;
    private final Map<String, PlaceholderProvider> providers = new ConcurrentHashMap<>();
    private final Map<String, Object> expansions = new ConcurrentHashMap<>();
    private boolean papi;

    public PlaceholderRegistry(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        this.papi = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
    }

    public void stop() {
        if (this.papi) {
            for (Object expansion : this.expansions.values()) {
                PapiBridge.unregister(expansion);
            }
        }

        this.expansions.clear();
        this.providers.clear();
    }

    /**
     * Registers a provider. With PlaceholderAPI around, {@code %prefix_...%} works in any plugin too.
     *
     * @param prefix prefix without symbols, like "sw"
     * @param provider provider
     */
    public void register(String prefix, PlaceholderProvider provider) {
        final String key = prefix.toLowerCase();

        this.providers.put(key, provider);

        if (this.papi && !this.expansions.containsKey(key)) {
            try {
                this.expansions.put(key, PapiBridge.register(this.plugin, key, provider));
            } catch (Throwable throwable) {
                this.plugin.getLogger().log(Level.WARNING, "Couldn't hook %" + key + "_...% into PlaceholderAPI", throwable);
            }
        }
    }

    public void unregister(String prefix) {
        final String key = prefix.toLowerCase();

        this.providers.remove(key);

        final Object expansion = this.expansions.remove(key);

        if (expansion != null) {
            PapiBridge.unregister(expansion);
        }
    }

    /**
     * Resolves Riven placeholders and, if installed, PlaceholderAPI ones.
     *
     * @param player player, can be null
     * @param text text
     * @return the text with values in
     */
    public String apply(OfflinePlayer player, String text) {
        if (text == null || text.indexOf('%') == -1) {
            return text;
        }

        final String local = this.applyLocal(player, text);

        if (this.papi && player != null && local.indexOf('%') != -1) {
            return PapiBridge.apply(player, local);
        }
        return local;
    }

    /**
     * Resolves only the placeholders registered here.
     *
     * @param player player, can be null
     * @param text text
     * @return the text with values in
     */
    public String applyLocal(OfflinePlayer player, String text) {
        if (text == null || text.indexOf('%') == -1 || this.providers.isEmpty()) {
            return text;
        }

        final Matcher matcher = PLACEHOLDER.matcher(text);
        final StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {
            final PlaceholderProvider provider = this.providers.get(matcher.group(1).toLowerCase());
            String value = null;

            if (provider != null) {
                try {
                    value = provider.resolve(player, matcher.group(2));
                } catch (Exception e) {
                    this.plugin.getLogger().log(Level.WARNING, "Placeholder " + matcher.group() + " failed", e);
                }
            }

            matcher.appendReplacement(buffer, Matcher.quoteReplacement(value != null ? value : matcher.group()));
        }

        matcher.appendTail(buffer);
        return buffer.toString();
    }

    public boolean isPlaceholderApiEnabled() {
        return this.papi;
    }

    public Map<String, PlaceholderProvider> getProviders() {
        return Collections.unmodifiableMap(this.providers);
    }

}
