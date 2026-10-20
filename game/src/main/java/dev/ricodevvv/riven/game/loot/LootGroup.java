package dev.ricodevvv.riven.game.loot;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * A group of items that compete with each other (swords, helmets, blocks...). When a chest is filled the group
 * shows up with {@link #getChance()}%, and then picks between {@link #getMinRolls()} and {@link #getMaxRolls()}
 * items by weight. With {@link #isUnique()} the same item never comes twice in one chest.
 */
public class LootGroup {

    private final String id;
    private final double chance;
    private final int minRolls;
    private final int maxRolls;
    private final boolean unique;
    private final List<LootItem> items;

    public LootGroup(String id, double chance, int minRolls, int maxRolls, boolean unique, List<LootItem> items) {
        this.id = id;
        this.chance = Math.max(0, Math.min(100, chance));
        this.minRolls = Math.max(0, minRolls);
        this.maxRolls = Math.max(this.minRolls, maxRolls);
        this.unique = unique;
        this.items = new ArrayList<>(items);
    }

    /**
     * @param random random source
     * @return the items this group adds to one chest
     */
    public List<ItemStack> roll(Random random) {
        final List<ItemStack> result = new ArrayList<>();

        if (this.items.isEmpty() || random.nextDouble() * 100 >= this.chance) {
            return result;
        }

        final int rolls = this.minRolls + (this.maxRolls > this.minRolls ? random.nextInt(this.maxRolls - this.minRolls + 1) : 0);
        final List<LootItem> pool = new ArrayList<>(this.items);

        for (int i = 0; i < rolls && !pool.isEmpty(); i++) {
            final LootItem picked = pick(pool, random);

            if (picked == null) {
                break;
            }

            result.add(picked.create(random));

            if (this.unique) {
                pool.remove(picked);
            }
        }
        return result;
    }

    private static LootItem pick(List<LootItem> pool, Random random) {
        double total = 0;

        for (LootItem item : pool) {
            total += item.getWeight();
        }

        if (total <= 0) {
            return null;
        }

        double value = random.nextDouble() * total;

        for (LootItem item : pool) {
            value -= item.getWeight();

            if (value < 0) {
                return item;
            }
        }
        return pool.get(pool.size() - 1);
    }

    /**
     * Real chance (0 to 100) that an item ends up in a chest, counting the group chance, the rolls and the
     * other weights. Exact for single roll groups, a close estimate for the rest. This is what editors show.
     *
     * @param item item of this group
     * @return percentage
     */
    public double getRealChance(LootItem item) {
        double total = 0;

        for (LootItem current : this.items) {
            total += current.getWeight();
        }

        if (total <= 0 || item.getWeight() <= 0) {
            return 0;
        }

        final double perRoll = item.getWeight() / total;
        final double rolls = (this.minRolls + this.maxRolls) / 2.0D;
        final double atLeastOnce = 1 - Math.pow(1 - perRoll, rolls);

        return this.chance * atLeastOnce;
    }

    public String getId() {
        return this.id;
    }

    public double getChance() {
        return this.chance;
    }

    public int getMinRolls() {
        return this.minRolls;
    }

    public int getMaxRolls() {
        return this.maxRolls;
    }

    public boolean isUnique() {
        return this.unique;
    }

    public List<LootItem> getItems() {
        return Collections.unmodifiableList(this.items);
    }

}
