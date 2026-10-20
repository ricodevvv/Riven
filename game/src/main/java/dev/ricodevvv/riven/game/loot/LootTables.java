package dev.ricodevvv.riven.game.loot;

import dev.ricodevvv.riven.config.ItemParser;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

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
 * Loads loot tables from {@code <folder>/<id>.yml} and saves the ones edited in game:
 *
 * <pre>
 * groups:
 *   swords:
 *     chance: 80
 *     rolls: "1-1"
 *     unique: true
 *     items:
 *       - { item: "STONE_SWORD", weight: 60 }
 *       - { item: "IRON_SWORD enchant:DAMAGE_ALL:1", weight: 25, amount: "1" }
 *   blocks:
 *     chance: 100
 *     rolls: "1-2"
 *     items:
 *       - { item: "STONE", weight: 50, amount: "16-32" }
 * </pre>
 */
public class LootTables {

    private final JavaPlugin plugin;
    private final File folder;
    private final Map<String, LootTable> tables = new LinkedHashMap<>();

    public LootTables(JavaPlugin plugin, String folder) {
        this.plugin = plugin;
        this.folder = new File(plugin.getDataFolder(), folder);
    }

    /**
     * Reads every table. Tables listed in {@code bundled} are copied from the jar if missing.
     *
     * @param bundled table ids shipped in the jar
     */
    public void load(String... bundled) {
        this.tables.clear();

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
                this.tables.put(id, read(id, YamlConfiguration.loadConfiguration(file)));
            } catch (Exception e) {
                this.plugin.getLogger().log(Level.SEVERE, "Couldn't load loot table " + file.getName(), e);
            }
        }
    }

    private static LootTable read(String id, ConfigurationSection section) {
        final LootTable table = new LootTable(id);
        final ConfigurationSection groups = section.getConfigurationSection("groups");

        if (groups == null) {
            return table;
        }

        for (String groupId : groups.getKeys(false)) {
            final ConfigurationSection group = groups.getConfigurationSection(groupId);

            if (group == null) {
                continue;
            }

            final List<LootItem> items = new ArrayList<>();

            for (Map<?, ?> raw : group.getMapList("items")) {
                final ItemStack item = ItemParser.parse(String.valueOf(raw.get("item")));

                if (item == null) {
                    continue;
                }

                final int[] amount = range(raw.containsKey("amount") ? String.valueOf(raw.get("amount")) : String.valueOf(item.getAmount()));
                final Object weight = raw.containsKey("weight") ? raw.get("weight") : 1;

                items.add(new LootItem(item, Double.parseDouble(String.valueOf(weight)), amount[0], amount[1]));
            }

            final int[] rolls = range(group.getString("rolls", "1"));

            table.group(new LootGroup(groupId, group.getDouble("chance", 100), rolls[0], rolls[1], group.getBoolean("unique", true), items));
        }
        return table;
    }

    private static int[] range(String text) {
        final String[] parts = text.trim().split("-");

        try {
            final int min = Integer.parseInt(parts[0].trim());
            final int max = parts.length > 1 ? Integer.parseInt(parts[1].trim()) : min;

            return new int[] {min, max};
        } catch (NumberFormatException e) {
            return new int[] {1, 1};
        }
    }

    /**
     * Writes a table to disk with {@link ItemParser#toLine(ItemStack)}.
     *
     * @param table table
     */
    public void save(LootTable table) {
        this.save(table, ItemParser::toLine);
    }

    /**
     * Writes a table to disk (used by in-game editors) and keeps it loaded. Items are saved in the line format.
     *
     * @param table table
     * @param serializer turns an item into an {@link ItemParser} line
     */
    public void save(LootTable table, java.util.function.Function<ItemStack, String> serializer) {
        final File file = new File(this.folder, table.getId() + ".yml");
        final YamlConfiguration config = new YamlConfiguration();

        for (LootGroup group : table.getGroups().values()) {
            final String path = "groups." + group.getId();
            final List<Map<String, Object>> items = new ArrayList<>();

            config.set(path + ".chance", group.getChance());
            config.set(path + ".rolls", group.getMinRolls() + "-" + group.getMaxRolls());
            config.set(path + ".unique", group.isUnique());

            for (LootItem item : group.getItems()) {
                final Map<String, Object> entry = new LinkedHashMap<>();

                entry.put("item", serializer.apply(item.getItem()));
                entry.put("weight", item.getWeight());
                entry.put("amount", item.getMinAmount() + "-" + item.getMaxAmount());
                items.add(entry);
            }

            config.set(path + ".items", items);
        }

        try {
            config.save(file);
        } catch (IOException e) {
            this.plugin.getLogger().log(Level.SEVERE, "Couldn't save loot table " + table.getId(), e);
        }

        this.tables.put(table.getId(), table);
    }

    public LootTable getTable(String id) {
        return id == null ? null : this.tables.get(id.toLowerCase());
    }

    public Collection<LootTable> getTables() {
        return Collections.unmodifiableCollection(this.tables.values());
    }

}
