package dev.ricodevvv.riven.util;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

/**
 * Quick checks on items.
 */
public final class ItemUtil {

    private ItemUtil() {}

    public static boolean isSword(ItemStack itemStack) {
        return itemStack != null && itemStack.getType().name().endsWith("_SWORD");
    }

    public static boolean isPickaxe(ItemStack itemStack) {
        return itemStack != null && itemStack.getType().name().endsWith("_PICKAXE");
    }

    public static boolean isAxe(ItemStack itemStack) {
        return itemStack != null && itemStack.getType().name().endsWith("_AXE");
    }

    public static boolean isHelmet(ItemStack itemStack) {
        return itemStack != null && itemStack.getType().name().endsWith("_HELMET");
    }

    public static boolean isChestplate(ItemStack itemStack) {
        return itemStack != null && itemStack.getType().name().endsWith("_CHESTPLATE");
    }

    public static boolean isLeggings(ItemStack itemStack) {
        return itemStack != null && itemStack.getType().name().endsWith("_LEGGINGS");
    }

    public static boolean isBoots(ItemStack itemStack) {
        return itemStack != null && itemStack.getType().name().endsWith("_BOOTS");
    }

    public static boolean isArmor(ItemStack itemStack) {
        return isHelmet(itemStack) || isChestplate(itemStack) || isLeggings(itemStack) || isBoots(itemStack);
    }

    /**
     * @param itemStack item to check
     * @return true if it's null or air
     */
    public static boolean isEmpty(ItemStack itemStack) {
        return itemStack == null || itemStack.getType() == Material.AIR;
    }

    /**
     * Armor slot for a piece: 0 boots, 1 leggings, 2 chestplate, 3 helmet, like {@code getArmorContents()}.
     *
     * @param itemStack armor piece
     * @return the slot or -1 if it isn't armor
     */
    public static int getArmorSlot(ItemStack itemStack) {
        if (isBoots(itemStack)) {
            return 0;
        }
        if (isLeggings(itemStack)) {
            return 1;
        }
        if (isChestplate(itemStack)) {
            return 2;
        }
        return isHelmet(itemStack) ? 3 : -1;
    }

    /**
     * Drops an item right on the location, without the random jump Bukkit adds.
     *
     * @param location where to drop it
     * @param itemStack item
     * @return the item entity
     */
    public static Item dropItem(Location location, ItemStack itemStack) {
        final Item item = location.getWorld().dropItem(location, itemStack);

        item.setVelocity(new Vector(0.0D, 0.0D, 0.0D));

        return item;
    }

}
