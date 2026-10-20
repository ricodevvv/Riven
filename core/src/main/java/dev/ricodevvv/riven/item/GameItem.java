package dev.ricodevvv.riven.item;

import dev.ricodevvv.riven.Riven;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Item with its own behaviour (leave, pick a team, teleporter...). Port of Hyrame's HyriItem.
 * It's identified by an NBT tag, so it keeps working even when the name changes with the language.
 */
public abstract class GameItem {

    public static final String TAG = "RivenItem";

    protected final String id;
    protected final String nameKey;
    protected final String loreKey;
    protected final ItemStack item;

    /**
     * @param id unique item id
     * @param nameKey language key of the name
     * @param loreKey language key of the lore, can be null
     * @param item base item
     */
    public GameItem(String id, String nameKey, String loreKey, ItemStack item) {
        this.id = id;
        this.nameKey = nameKey;
        this.loreKey = loreKey;
        this.item = item;
    }

    /**
     * Builds the item for a given player (in their language).
     *
     * @param player player
     * @param slot slot it goes to
     * @return the item ready to give
     */
    public ItemStack create(Player player, int slot) {
        final ItemBuilder builder = new ItemBuilder(this.item).withName(this.getDisplayName(player)).withAllItemFlags();

        if (this.loreKey != null) {
            builder.withLore(Riven.get().getLanguage().getList(player, this.loreKey));
        }

        final ItemStack itemStack = builder.nbt().setString(TAG, this.id).build();

        return this.onPreGive(player, slot, itemStack);
    }

    /**
     * Lets you tweak the item right before it's given.
     *
     * @param player player
     * @param slot slot
     * @param itemStack built item
     * @return the final item
     */
    public ItemStack onPreGive(Player player, int slot, ItemStack itemStack) {
        return itemStack;
    }

    /**
     * @param player player
     * @return the item name in their language
     */
    protected String getDisplayName(Player player) {
        return Riven.get().getLanguage().get(player, this.nameKey);
    }

    public void onRightClick(PlayerInteractEvent event) {}

    public void onLeftClick(PlayerInteractEvent event) {}

    public void onInventoryClick(InventoryClickEvent event) {}

    public String getId() {
        return this.id;
    }

}
