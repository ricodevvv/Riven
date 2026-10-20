package dev.ricodevvv.riven.data.cosmetic;

import dev.ricodevvv.riven.config.ItemParser;
import dev.ricodevvv.riven.util.TextUtil;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * Loads cosmetics from one file per type, {@code <folder>/<type>.yml}:
 *
 * <pre>
 * cosmetics:
 *   red:
 *     name: "&amp;cRed cage"
 *     icon: { material: STAINED_GLASS, data: 14 }
 *     price: 2500
 *     currency: coins
 *     rarity: RARE
 *     auto-unlock: false
 *     purchasable: true
 *     # anything else is type data, read it with getData()
 * </pre>
 *
 * Ownership comes from {@link CosmeticData}, the auto-unlock flag or the {@code <prefix>.cosmetic.<type>.<id>}
 * permission.
 */
public class CosmeticRegistry {

    private final JavaPlugin plugin;
    private final File folder;
    private final String permissionPrefix;
    private final Map<String, Map<String, Cosmetic>> cosmetics = new LinkedHashMap<>();

    /**
     * @param plugin owner plugin
     * @param folder folder inside the plugin folder
     * @param permissionPrefix prefix for unlock permissions, like "skywars"
     */
    public CosmeticRegistry(JavaPlugin plugin, String folder, String permissionPrefix) {
        this.plugin = plugin;
        this.folder = new File(plugin.getDataFolder(), folder);
        this.permissionPrefix = permissionPrefix;
    }

    /**
     * Loads every type file. Types listed in {@code bundled} are copied from the jar if missing.
     *
     * @param bundled type ids shipped in the jar
     */
    public void load(String... bundled) {
        this.cosmetics.clear();

        if (!this.folder.exists() && !this.folder.mkdirs()) {
            this.plugin.getLogger().warning("Couldn't create " + this.folder);
        }

        for (String type : bundled) {
            final String resource = this.folder.getName() + "/" + type + ".yml";

            if (!new File(this.folder, type + ".yml").exists() && this.plugin.getResource(resource) != null) {
                this.plugin.saveResource(resource, false);
            }
        }

        final File[] files = this.folder.listFiles((dir, name) -> name.endsWith(".yml"));

        if (files == null) {
            return;
        }

        for (File file : files) {
            final String type = file.getName().substring(0, file.getName().length() - 4).toLowerCase();

            try {
                this.loadType(type, YamlConfiguration.loadConfiguration(file).getConfigurationSection("cosmetics"));
            } catch (Exception e) {
                this.plugin.getLogger().log(Level.SEVERE, "Couldn't load cosmetics " + file.getName(), e);
            }
        }
    }

    private void loadType(String type, ConfigurationSection section) {
        final Map<String, Cosmetic> byId = new LinkedHashMap<>();

        if (section != null) {
            for (String id : section.getKeys(false)) {
                final ConfigurationSection cosmetic = section.getConfigurationSection(id);

                if (cosmetic == null) {
                    continue;
                }

                final ItemStack icon = ItemParser.parse(cosmetic.getConfigurationSection("icon"));

                byId.put(id.toLowerCase(), new Cosmetic(type, id,
                        TextUtil.colorize(cosmetic.getString("name", id)),
                        icon != null ? icon : new ItemStack(Material.PAPER),
                        cosmetic.getLong("price", 0),
                        cosmetic.getString("currency", "coins"),
                        Rarity.parse(cosmetic.getString("rarity", "COMMON")),
                        cosmetic.getBoolean("auto-unlock", false),
                        cosmetic.getBoolean("purchasable", true),
                        cosmetic));
            }
        }

        this.cosmetics.put(type, byId);
    }

    /**
     * Adds a cosmetic made in code (win effects and kill effects usually are).
     *
     * @param cosmetic cosmetic
     */
    public void register(Cosmetic cosmetic) {
        this.cosmetics.computeIfAbsent(cosmetic.getType(), key -> new LinkedHashMap<>()).put(cosmetic.getId(), cosmetic);
    }

    public Cosmetic get(String type, String id) {
        final Map<String, Cosmetic> byId = this.cosmetics.get(type.toLowerCase());

        return byId == null || id == null ? null : byId.get(id.toLowerCase());
    }

    public List<Cosmetic> getAll(String type) {
        final Map<String, Cosmetic> byId = this.cosmetics.get(type.toLowerCase());

        return byId == null ? Collections.emptyList() : new ArrayList<>(byId.values());
    }

    public Collection<String> getTypes() {
        return Collections.unmodifiableSet(this.cosmetics.keySet());
    }

    /**
     * @param player player
     * @param data their cosmetics
     * @param cosmetic cosmetic
     * @return true if they can use it
     */
    public boolean canUse(Player player, CosmeticData data, Cosmetic cosmetic) {
        return cosmetic.isAutoUnlock() || data.owns(cosmetic.getType(), cosmetic.getId()) || player.hasPermission(cosmetic.getPermission(this.permissionPrefix));
    }

    /**
     * @param player player
     * @param data their cosmetics
     * @param type type
     * @return the selected cosmetic if they can still use it, otherwise null
     */
    public Cosmetic getSelected(Player player, CosmeticData data, String type) {
        final Cosmetic cosmetic = this.get(type, data.getSelected(type));

        return cosmetic != null && this.canUse(player, data, cosmetic) ? cosmetic : null;
    }

    public String getPermissionPrefix() {
        return this.permissionPrefix;
    }

}
