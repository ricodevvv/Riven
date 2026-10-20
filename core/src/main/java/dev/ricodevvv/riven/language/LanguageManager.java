package dev.ricodevvv.riven.language;

import dev.ricodevvv.riven.util.TextUtil;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Loads the {@code lang/<iso>.yml} files and hands out every message in each player's language.
 * Replaces Hyriode's HyriLanguageMessage, which depended on their network.
 */
public class LanguageManager {

    private static final String FOLDER = "lang";

    private final JavaPlugin plugin;
    private final File folder;
    private final List<String> bundled;
    private final Map<String, YamlConfiguration> languages = new HashMap<>();
    private final Map<UUID, String> playerLanguages = new ConcurrentHashMap<>();
    private final Set<String> warnedKeys = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private String defaultLanguage;

    /**
     * @param plugin plugin that owns the files
     * @param defaultLanguage default language (for example "en")
     * @param bundled languages shipped inside the jar, copied over if they don't exist yet
     */
    public LanguageManager(JavaPlugin plugin, String defaultLanguage, String... bundled) {
        this.plugin = plugin;
        this.folder = new File(plugin.getDataFolder(), FOLDER);
        this.defaultLanguage = defaultLanguage.toLowerCase();
        this.bundled = Arrays.asList(bundled);
    }

    /**
     * (Re)loads every language. Keys missing in a file are filled in from the jar.
     */
    public void load() {
        this.languages.clear();
        this.warnedKeys.clear();

        if (!this.folder.exists() && !this.folder.mkdirs()) {
            this.plugin.getLogger().warning("Couldn't create the language folder");
        }

        for (String iso : this.bundled) {
            final File file = new File(this.folder, iso + ".yml");

            if (!file.exists()) {
                this.plugin.saveResource(FOLDER + "/" + iso + ".yml", false);
            }
        }

        final File[] files = this.folder.listFiles((dir, name) -> name.endsWith(".yml"));

        if (files == null) {
            return;
        }

        for (File file : files) {
            final String iso = file.getName().substring(0, file.getName().length() - 4).toLowerCase();
            final YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            final InputStream defaults = this.plugin.getResource(FOLDER + "/" + iso + ".yml");

            if (defaults != null) {
                config.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(defaults, StandardCharsets.UTF_8)));
            }

            this.languages.put(iso, config);
        }

        if (!this.languages.containsKey(this.defaultLanguage)) {
            this.plugin.getLogger().log(Level.WARNING, "Default language ''{0}'' doesn't exist, using the first one available", this.defaultLanguage);

            if (!this.languages.isEmpty()) {
                this.defaultLanguage = this.languages.keySet().iterator().next();
            }
        }
    }

    /**
     * Looks up a message in a language. Falls back to the default language, and to the key itself if it's missing there too.
     *
     * @param iso language
     * @param key message key
     * @return the colored message
     */
    public String get(String iso, String key) {
        final Object value = this.resolve(iso, key);

        if (value == null) {
            if (this.warnedKeys.add(key)) {
                this.plugin.getLogger().warning("Missing message '" + key + "' in the language files");
            }
            return key;
        }

        if (value instanceof List) {
            final List<String> lines = new ArrayList<>();

            for (Object line : (List<?>) value) {
                lines.add(String.valueOf(line));
            }
            return TextUtil.colorize(String.join("\n", lines));
        }
        return TextUtil.colorize(String.valueOf(value));
    }

    /**
     * @param player player
     * @param key message key
     * @return the message in the player's language
     */
    public String get(Player player, String key) {
        return this.get(player == null ? this.defaultLanguage : this.getLanguage(player.getUniqueId()), key);
    }

    /**
     * @param playerId player uuid
     * @param key message key
     * @return the message in the player's language
     */
    public String get(UUID playerId, String key) {
        return this.get(this.getLanguage(playerId), key);
    }

    /**
     * @param key message key
     * @return the message in the default language
     */
    public String get(String key) {
        return this.get(this.defaultLanguage, key);
    }

    /**
     * Gets a message and swaps placeholders in pairs: {@code get(player, "kill", "%killer%", name, "%coins%", 10)}.
     *
     * @param player player, null for the default language
     * @param key message key
     * @param replacements placeholder and value pairs
     * @return the message with the values in
     */
    public String get(Player player, String key, Object... replacements) {
        return replace(this.get(player, key), replacements);
    }

    /**
     * Like {@link #get(Player, String)} but returns the fallback (colored) when no language has the key,
     * without logging a warning. Libraries use it for messages the plugin may or may not override.
     *
     * @param player player, null for the default language
     * @param key message key
     * @param fallback text used when the key is missing
     * @return the message
     */
    public String getOrDefault(Player player, String key, String fallback) {
        final Object value = this.resolve(player == null ? this.defaultLanguage : this.getLanguage(player.getUniqueId()), key);

        return value == null ? TextUtil.colorize(fallback) : this.get(player, key);
    }

    /**
     * Swaps placeholders in pairs on any text.
     *
     * @param text text
     * @param replacements placeholder and value pairs
     * @return the text with the values in
     */
    public static String replace(String text, Object... replacements) {
        if (text == null || replacements == null) {
            return text;
        }

        String result = text;

        for (int i = 0; i + 1 < replacements.length; i += 2) {
            result = result.replace(String.valueOf(replacements[i]), String.valueOf(replacements[i + 1]));
        }
        return result;
    }

    /**
     * Returns a message as a list of lines (handy for lores).
     *
     * @param player player
     * @param key message key
     * @return message lines
     */
    public List<String> getList(Player player, String key) {
        return new ArrayList<>(Arrays.asList(this.get(player, key).split("\n")));
    }

    /**
     * @param key key to check
     * @return true if the key exists in the default language
     */
    public boolean has(String key) {
        return this.resolve(this.defaultLanguage, key) != null;
    }

    /**
     * Like {@link #get(String, String)} but without falling back to the default language. Handy for things
     * that describe the language itself, like its name or its flag.
     *
     * @param iso language
     * @param key message key
     * @return the colored message, or null if that language doesn't have it
     */
    public String getExact(String iso, String key) {
        final YamlConfiguration config = this.languages.get(iso);

        return config != null && config.contains(key) ? TextUtil.colorize(config.getString(key)) : null;
    }

    private Object resolve(String iso, String key) {
        YamlConfiguration config = this.languages.get(iso);

        if (config != null && config.contains(key)) {
            return config.get(key);
        }

        config = this.languages.get(this.defaultLanguage);

        if (config != null && config.contains(key)) {
            return config.get(key);
        }
        return null;
    }

    /**
     * @param playerId player uuid
     * @return the language they picked, or the default one
     */
    public String getLanguage(UUID playerId) {
        final String iso = this.playerLanguages.get(playerId);

        return iso != null && this.languages.containsKey(iso) ? iso : this.defaultLanguage;
    }

    /**
     * Changes a player's language. Passing null puts them back on the default one.
     *
     * @param playerId player uuid
     * @param iso new language
     */
    public void setLanguage(UUID playerId, String iso) {
        if (iso == null) {
            this.playerLanguages.remove(playerId);
            return;
        }
        this.playerLanguages.put(playerId, iso.toLowerCase());
    }

    public void removePlayer(UUID playerId) {
        this.playerLanguages.remove(playerId);
    }

    public boolean exists(String iso) {
        return iso != null && this.languages.containsKey(iso.toLowerCase());
    }

    public Set<String> getAvailableLanguages() {
        return new TreeSet<>(this.languages.keySet());
    }

    public String getDefaultLanguage() {
        return this.defaultLanguage;
    }

    public void setDefaultLanguage(String defaultLanguage) {
        this.defaultLanguage = defaultLanguage.toLowerCase();
    }

    /**
     * @return every loaded key (handy for debugging)
     */
    public Set<String> getKeys() {
        final Set<String> keys = new HashSet<>();

        for (YamlConfiguration config : this.languages.values()) {
            keys.addAll(config.getKeys(true));
        }
        return keys;
    }

}
