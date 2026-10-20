package dev.ricodevvv.riven.item;

import net.minecraft.server.v1_8_R3.NBTTagCompound;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;

/**
 * Reads and writes NBT tags on an item. Used to flag special items (permanent ones, shop items, etc.).
 */
public class ItemNBT {

    private final net.minecraft.server.v1_8_R3.ItemStack nmsItemStack;
    private final NBTTagCompound tag;

    public ItemNBT(ItemStack itemStack) {
        this.nmsItemStack = CraftItemStack.asNMSCopy(itemStack);
        this.tag = this.nmsItemStack == null ? new NBTTagCompound() : (this.nmsItemStack.hasTag() ? this.nmsItemStack.getTag() : new NBTTagCompound());
    }

    /**
     * Shortcut to check if an item has a boolean tag set to true.
     *
     * @param itemStack item
     * @param tag tag
     * @return true if it exists and it's true
     */
    public static boolean is(ItemStack itemStack, String tag) {
        if (itemStack == null || itemStack.getType() == org.bukkit.Material.AIR) {
            return false;
        }

        final ItemNBT nbt = new ItemNBT(itemStack);

        return nbt.hasTag(tag) && nbt.getBoolean(tag);
    }

    public String getString(String key) {
        return this.tag.getString(key);
    }

    public ItemNBT setString(String key, String value) {
        this.tag.setString(key, value);
        return this;
    }

    public int getInt(String key) {
        return this.tag.getInt(key);
    }

    public ItemNBT setInt(String key, int value) {
        this.tag.setInt(key, value);
        return this;
    }

    public boolean getBoolean(String key) {
        return this.tag.getBoolean(key);
    }

    public ItemNBT setBoolean(String key, boolean value) {
        this.tag.setBoolean(key, value);
        return this;
    }

    public ItemNBT removeTag(String key) {
        this.tag.remove(key);
        return this;
    }

    public boolean hasTag(String key) {
        return this.tag.hasKey(key);
    }

    /**
     * @return a copy of the item with the tags applied
     */
    public ItemStack build() {
        if (this.nmsItemStack == null) {
            return null;
        }

        this.nmsItemStack.setTag(this.tag);

        return CraftItemStack.asBukkitCopy(this.nmsItemStack);
    }

    /**
     * @return an {@link ItemBuilder} from the tagged item
     */
    public ItemBuilder toBuilder() {
        return new ItemBuilder(this.build());
    }

}
