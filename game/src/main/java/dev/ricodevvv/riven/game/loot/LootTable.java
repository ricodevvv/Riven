package dev.ricodevvv.riven.game.loot;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * A set of {@link LootGroup}s that fills one kind of chest (basic island chest, OP center chest...).
 */
public class LootTable {

    private final String id;
    private final Map<String, LootGroup> groups = new LinkedHashMap<>();

    public LootTable(String id) {
        this.id = id;
    }

    public LootTable group(LootGroup group) {
        this.groups.put(group.getId(), group);
        return this;
    }

    /**
     * @param random random source
     * @return every item of one chest
     */
    public List<ItemStack> roll(Random random) {
        final List<ItemStack> result = new ArrayList<>();

        for (LootGroup group : this.groups.values()) {
            result.addAll(group.roll(random));
        }
        return result;
    }

    /**
     * Rolls and spreads the items in random empty slots of an inventory. Items that don't fit are dropped.
     *
     * @param inventory chest inventory
     * @param random random source
     * @param clear true to empty the inventory first
     */
    public void fill(Inventory inventory, Random random, boolean clear) {
        if (clear) {
            inventory.clear();
        }

        final List<Integer> free = new ArrayList<>();

        for (int i = 0; i < inventory.getSize(); i++) {
            final ItemStack current = inventory.getItem(i);

            if (current == null || current.getType().name().equals("AIR")) {
                free.add(i);
            }
        }

        Collections.shuffle(free, random);

        int index = 0;

        for (ItemStack item : this.roll(random)) {
            if (index >= free.size()) {
                break;
            }
            inventory.setItem(free.get(index++), item);
        }
    }

    public String getId() {
        return this.id;
    }

    public LootGroup getGroup(String id) {
        return this.groups.get(id);
    }

    public Map<String, LootGroup> getGroups() {
        return Collections.unmodifiableMap(this.groups);
    }

}
