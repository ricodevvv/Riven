package dev.ricodevvv.riven.game.loot;

import org.bukkit.inventory.ItemStack;

import java.util.Random;

/**
 * One possible item of a {@link LootGroup}: the item, its weight and how many come.
 */
public class LootItem {

    private final ItemStack item;
    private final double weight;
    private final int minAmount;
    private final int maxAmount;

    /**
     * @param item item, its amount is ignored
     * @param weight how likely it is compared to the rest of the group
     * @param minAmount minimum amount
     * @param maxAmount maximum amount
     */
    public LootItem(ItemStack item, double weight, int minAmount, int maxAmount) {
        this.item = item;
        this.weight = Math.max(0, weight);
        this.minAmount = Math.max(1, minAmount);
        this.maxAmount = Math.max(this.minAmount, maxAmount);
    }

    /**
     * @param random random source
     * @return a copy of the item with a random amount in range
     */
    public ItemStack create(Random random) {
        final ItemStack result = this.item.clone();

        result.setAmount(this.minAmount + (this.maxAmount > this.minAmount ? random.nextInt(this.maxAmount - this.minAmount + 1) : 0));
        return result;
    }

    public ItemStack getItem() {
        return this.item.clone();
    }

    public double getWeight() {
        return this.weight;
    }

    public int getMinAmount() {
        return this.minAmount;
    }

    public int getMaxAmount() {
        return this.maxAmount;
    }

}
