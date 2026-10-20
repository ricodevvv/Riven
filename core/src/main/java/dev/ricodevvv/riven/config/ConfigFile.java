package dev.ricodevvv.riven.config;

import dev.ricodevvv.riven.util.LocationWrapper;
import dev.ricodevvv.riven.util.TextUtil;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

/**
 * A YAML file of the plugin. The first time it's copied from the jar; after that every key that's in the jar
 * but missing on disk gets added and saved, so updates never leave old configs without new options.
 */
public class ConfigFile {

    private final JavaPlugin plugin;
    private final String resource;
    private final File file;
    private YamlConfiguration config;

    /**
     * @param plugin owner plugin
     * @param resource path inside the jar and inside the plugin folder (like "menus/main.yml")
     */
    public ConfigFile(JavaPlugin plugin, String resource) {
        this(plugin, resource, new File(plugin.getDataFolder(), resource));
    }

    /**
     * @param plugin owner plugin
     * @param resource path inside the jar used for defaults, can point to nothing
     * @param file file on disk
     */
    public ConfigFile(JavaPlugin plugin, String resource, File file) {
        this.plugin = plugin;
        this.resource = resource;
        this.file = file;
    }

    /**
     * Loads (or reloads) the file, copying it or filling in missing keys from the jar.
     *
     * @return this file
     */
    public ConfigFile load() {
        final InputStream bundled = this.resource == null ? null : this.plugin.getResource(this.resource);

        if (!this.file.exists()) {
            final File parent = this.file.getParentFile();

            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                this.plugin.getLogger().warning("Couldn't create " + parent);
            }

            if (bundled != null && this.file.equals(new File(this.plugin.getDataFolder(), this.resource))) {
                this.plugin.saveResource(this.resource, false);
            }
        }

        this.config = YamlConfiguration.loadConfiguration(this.file);

        if (bundled != null) {
            final YamlConfiguration defaults = YamlConfiguration.loadConfiguration(new InputStreamReader(bundled, StandardCharsets.UTF_8));
            boolean changed = false;

            for (String key : defaults.getKeys(true)) {
                if (!defaults.isConfigurationSection(key) && !this.config.contains(key)) {
                    this.config.set(key, defaults.get(key));
                    changed = true;
                }
            }

            this.config.setDefaults(defaults);

            if (changed && this.file.exists()) {
                this.save();
            }
        }
        return this;
    }

    public void save() {
        try {
            this.config.save(this.file);
        } catch (IOException e) {
            this.plugin.getLogger().log(Level.SEVERE, "Couldn't save " + this.file.getName(), e);
        }
    }

    public YamlConfiguration get() {
        return this.config;
    }

    public File getFile() {
        return this.file;
    }

    /**
     * @param path key
     * @return the colored string, or null
     */
    public String getText(String path) {
        return TextUtil.colorize(this.config.getString(path));
    }

    /**
     * @param path key
     * @return the colored list (a plain string becomes a one-line list)
     */
    public List<String> getTextList(String path) {
        if (this.config.isString(path)) {
            return Collections.singletonList(TextUtil.colorize(this.config.getString(path)));
        }
        return TextUtil.colorize(new ArrayList<>(this.config.getStringList(path)));
    }

    /**
     * @param path key
     * @return the section, created if it doesn't exist
     */
    public ConfigurationSection section(String path) {
        final ConfigurationSection section = this.config.getConfigurationSection(path);

        return section != null ? section : this.config.createSection(path);
    }

    /**
     * Reads a location stored with {@link #setLocation(String, Location)}.
     *
     * @param path key
     * @param world world to apply it to
     * @return the location, or null if it's missing or broken
     */
    public Location getLocation(String path, World world) {
        final LocationWrapper wrapper = LocationWrapper.deserialize(this.config.getString(path));

        return wrapper == null || world == null ? null : wrapper.asBukkit(world);
    }

    /**
     * Stores a location as {@code world;x;y;z;yaw;pitch}.
     *
     * @param path key
     * @param location location, null removes it
     */
    public void setLocation(String path, Location location) {
        this.config.set(path, location == null ? null : location.getWorld().getName() + ";" + LocationWrapper.from(location).serialize());
    }

    /**
     * Reads a location written by {@link #setLocation(String, Location)}, world included.
     *
     * @param path key
     * @return the location, or null if the world isn't loaded or the value is broken
     */
    public Location getLocation(String path) {
        final String value = this.config.getString(path);

        if (value == null || !value.contains(";")) {
            return null;
        }

        final int split = value.indexOf(';');
        final World world = org.bukkit.Bukkit.getWorld(value.substring(0, split));

        return this.getLocationValue(value.substring(split + 1), world);
    }

    private Location getLocationValue(String value, World world) {
        final LocationWrapper wrapper = LocationWrapper.deserialize(value);

        return wrapper == null || world == null ? null : wrapper.asBukkit(world);
    }

}
