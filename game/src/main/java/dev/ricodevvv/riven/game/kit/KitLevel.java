package dev.ricodevvv.riven.game.kit;

import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * What a kit gives at one level: inventory, armor and effects, plus what it costs to reach this level.
 */
public class KitLevel {

    private final int level;
    private final int price;
    private final ItemStack[] contents;
    private final ItemStack[] armor;
    private final List<PotionEffect> effects;

    /**
     * @param level level number, starting at 1
     * @param price cost to unlock (level 1) or upgrade to this level
     * @param contents 36 inventory slots, nulls allowed
     * @param armor boots, leggings, chestplate, helmet
     * @param effects effects given on apply
     */
    public KitLevel(int level, int price, ItemStack[] contents, ItemStack[] armor, List<PotionEffect> effects) {
        this.level = level;
        this.price = price;
        this.contents = contents == null ? new ItemStack[36] : contents;
        this.armor = armor == null ? new ItemStack[4] : armor;
        this.effects = effects == null ? new ArrayList<>() : new ArrayList<>(effects);
    }

    public int getLevel() {
        return this.level;
    }

    public int getPrice() {
        return this.price;
    }

    public ItemStack[] getContents() {
        return clone(this.contents);
    }

    public ItemStack[] getArmor() {
        return clone(this.armor);
    }

    public List<PotionEffect> getEffects() {
        return Collections.unmodifiableList(this.effects);
    }

    private static ItemStack[] clone(ItemStack[] items) {
        final ItemStack[] result = new ItemStack[items.length];

        for (int i = 0; i < items.length; i++) {
            result[i] = items[i] == null ? null : items[i].clone();
        }
        return result;
    }

}
