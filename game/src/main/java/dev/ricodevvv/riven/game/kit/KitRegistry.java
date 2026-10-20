package dev.ricodevvv.riven.game.kit;

import dev.ricodevvv.riven.config.ItemParser;
import dev.ricodevvv.riven.util.TextUtil;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * Loads kits from {@code kits/<id>.yml} and can create them straight from a player's inventory, so admins
 * build kits in game. File format:
 *
 * <pre>
 * name: "&amp;aArcher"
 * icon: { material: BOW }
 * description: ["&amp;7Bow and arrows"]
 * permission: skywars.kit.archer
 * auto-unlock: false
 * category: solo
 * levels:
 *   1:
 *     price: 5000
 *     items:
 *       0: { material: BOW }
 *       1: { material: ARROW, amount: 16 }
 *     armor:
 *       helmet: { material: LEATHER_HELMET }
 *     effects: ["SPEED:200:0"]
 * </pre>
 */
public class KitRegistry {

    private static final String[] ARMOR_KEYS = {"boots", "leggings", "chestplate", "helmet"};

    private final JavaPlugin plugin;
    private final File folder;
    private final Map<String, Kit> kits = new LinkedHashMap<>();

    public KitRegistry(JavaPlugin plugin, String folder) {
        this.plugin = plugin;
        this.folder = new File(plugin.getDataFolder(), folder);
    }

    /**
     * Reads every kit file. Kits listed in {@code bundled} get copied from the jar first if missing.
     *
     * @param bundled kit ids shipped in the jar ({@code <folder>/<id>.yml})
     */
    public void load(String... bundled) {
        this.kits.clear();

        if (!this.folder.exists() && !this.folder.mkdirs()) {
            this.plugin.getLogger().warning("Couldn't create " + this.folder);
        }

        for (String id : bundled) {
            final String resource = this.folder.getName() + "/" + id + ".yml";

            if (!new File(this.folder, id + ".yml").exists() && this.plugin.getResource(resource) != null) {
                this.plugin.saveResource(resource, false);
            }
        }

        final File[] files = this.folder.listFiles((dir, name) -> name.endsWith(".yml"));

        if (files == null) {
            return;
        }

        for (File file : files) {
            final String id = file.getName().substring(0, file.getName().length() - 4).toLowerCase();

            try {
                this.kits.put(id, read(id, YamlConfiguration.loadConfiguration(file)));
            } catch (Exception e) {
                this.plugin.getLogger().log(Level.SEVERE, "Couldn't load kit " + file.getName(), e);
            }
        }
    }

    private static Kit read(String id, ConfigurationSection section) {
        final List<KitLevel> levels = new ArrayList<>();
        final ConfigurationSection levelsSection = section.getConfigurationSection("levels");

        if (levelsSection != null) {
            for (String key : levelsSection.getKeys(false)) {
                final ConfigurationSection level = levelsSection.getConfigurationSection(key);

                if (level != null) {
                    levels.add(readLevel(Integer.parseInt(key), level));
                }
            }
        }

        levels.sort((first, second) -> Integer.compare(first.getLevel(), second.getLevel()));

        final ItemStack icon = ItemParser.parse(section.getConfigurationSection("icon"));

        return new Kit(id,
                TextUtil.colorize(section.getString("name", id)),
                icon != null ? icon : new ItemStack(Material.CHEST),
                TextUtil.colorize(section.getStringList("description")),
                section.getString("permission"),
                section.getBoolean("auto-unlock"),
                section.getString("category"),
                levels);
    }

    private static KitLevel readLevel(int number, ConfigurationSection section) {
        final ItemStack[] contents = new ItemStack[36];
        final ItemStack[] armor = new ItemStack[4];
        final List<PotionEffect> effects = new ArrayList<>();
        final ConfigurationSection items = section.getConfigurationSection("items");
        final ConfigurationSection armorSection = section.getConfigurationSection("armor");

        if (items != null) {
            for (String slot : items.getKeys(false)) {
                final int index = Integer.parseInt(slot);

                if (index >= 0 && index < 36) {
                    contents[index] = ItemParser.parse(items.getConfigurationSection(slot));
                }
            }
        }

        if (armorSection != null) {
            for (int i = 0; i < ARMOR_KEYS.length; i++) {
                armor[i] = ItemParser.parse(armorSection.getConfigurationSection(ARMOR_KEYS[i]));
            }
        }

        for (String effect : section.getStringList("effects")) {
            final String[] parts = effect.split(":");
            final PotionEffectType type = PotionEffectType.getByName(parts[0].toUpperCase());

            if (type != null) {
                effects.add(new PotionEffect(type, parts.length > 1 ? Integer.parseInt(parts[1]) : Integer.MAX_VALUE, parts.length > 2 ? Integer.parseInt(parts[2]) : 0));
            }
        }

        return new KitLevel(number, section.getInt("price"), contents, armor, effects);
    }

    /**
     * Saves the player's current inventory as a level of a kit, creating the kit if it doesn't exist.
     *
     * @param id kit id
     * @param level level to write
     * @param price level price
     * @param player player whose inventory is copied
     * @return the reloaded kit
     */
    public Kit saveFromInventory(String id, int level, int price, Player player) {
        final File file = new File(this.folder, id.toLowerCase() + ".yml");
        final YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        if (!config.contains("name")) {
            config.set("name", "&a" + TextUtil.capitalize(id));
            ItemParser.write(config.createSection("icon"), new ItemStack(Material.CHEST));
            config.set("description", Collections.singletonList("&7A custom kit"));
            config.set("auto-unlock", false);
        }

        final String path = "levels." + level;

        config.set(path, null);
        config.set(path + ".price", price);

        final ItemStack[] contents = player.getInventory().getContents();

        for (int i = 0; i < contents.length && i < 36; i++) {
            if (contents[i] != null && contents[i].getType() != Material.AIR) {
                ItemParser.write(config.createSection(path + ".items." + i), contents[i]);
            }
        }

        final ItemStack[] armor = player.getInventory().getArmorContents();

        for (int i = 0; i < 4; i++) {
            if (armor[i] != null && armor[i].getType() != Material.AIR) {
                ItemParser.write(config.createSection(path + ".armor." + ARMOR_KEYS[i]), armor[i]);
            }
        }

        final List<String> effects = new ArrayList<>();

        for (PotionEffect effect : player.getActivePotionEffects()) {
            effects.add(effect.getType().getName() + ":" + effect.getDuration() + ":" + effect.getAmplifier());
        }

        config.set(path + ".effects", effects);

        try {
            config.save(file);
        } catch (IOException e) {
            this.plugin.getLogger().log(Level.SEVERE, "Couldn't save kit " + id, e);
        }

        final Kit kit = read(id.toLowerCase(), config);

        this.kits.put(kit.getId(), kit);
        return kit;
    }

    /**
     * Deletes a kit file.
     *
     * @param id kit id
     * @return true if it existed
     */
    public boolean delete(String id) {
        final Kit removed = this.kits.remove(id.toLowerCase());
        final File file = new File(this.folder, id.toLowerCase() + ".yml");

        if (file.exists() && !file.delete()) {
            this.plugin.getLogger().warning("Couldn't delete " + file.getName());
        }
        return removed != null;
    }

    public Kit getKit(String id) {
        return id == null ? null : this.kits.get(id.toLowerCase());
    }

    public Collection<Kit> getKits() {
        return Collections.unmodifiableCollection(this.kits.values());
    }

    /**
     * @param category category, null for every kit
     * @return kits of that category
     */
    public List<Kit> getKits(String category) {
        final List<Kit> result = new ArrayList<>();

        for (Kit kit : this.kits.values()) {
            if (category == null || kit.getCategory() == null || kit.getCategory().equalsIgnoreCase(category)) {
                result.add(kit);
            }
        }
        return result;
    }

    /**
     * @return the first auto unlocked kit, used when a player has nothing selected
     */
    public Kit getDefaultKit() {
        for (Kit kit : this.kits.values()) {
            if (kit.isAutoUnlock()) {
                return kit;
            }
        }
        return null;
    }

}
