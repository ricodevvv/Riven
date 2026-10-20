package dev.ricodevvv.riven.inventory;

import dev.ricodevvv.riven.Riven;
import dev.ricodevvv.riven.item.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Menu with per-slot actions. Port of Hyrame's HyriInventory: create it, put items with their click and open it.
 */
public abstract class GameInventory implements InventoryHolder {

    private static final int MAX_TITLE_LENGTH = 32;

    protected final Player owner;
    protected final String name;
    protected final int size;
    protected final Inventory inventory;
    protected boolean cancelClickEvent = true;

    private final Map<Integer, Consumer<InventoryClickEvent>> clickConsumers = new HashMap<>();

    public GameInventory(Player owner, String name, int size) {
        this.owner = owner;
        this.name = name.length() > MAX_TITLE_LENGTH ? name.substring(0, MAX_TITLE_LENGTH) : name;
        this.size = size;
        this.inventory = Bukkit.createInventory(this, size, this.name);
    }

    /**
     * Translated text for titles and item names.
     *
     * @param player player
     * @param key message key
     * @return the message in their language
     */
    protected static String lang(Player player, String key) {
        return Riven.get().getLanguage().get(player, key);
    }

    /**
     * Inventory size (multiple of 9) that fits the given amount of items.
     *
     * @param amount amount of items
     * @return size between 9 and 54
     */
    protected static int dynamicSize(int amount) {
        final int size = ((amount / 9) + (amount % 9 == 0 ? 0 : 1)) * 9;

        return Math.max(9, Math.min(54, size));
    }

    public void setItem(int slot, ItemStack itemStack) {
        this.clickConsumers.remove(slot);
        this.inventory.setItem(slot, itemStack);
    }

    public void setItem(int slot, ItemStack itemStack, Consumer<InventoryClickEvent> clickConsumer) {
        this.setItem(slot, itemStack);

        if (clickConsumer != null) {
            this.clickConsumers.put(slot, clickConsumer);
        }
    }

    public void addItem(ItemStack itemStack, Consumer<InventoryClickEvent> clickConsumer) {
        final int slot = this.inventory.firstEmpty();

        if (slot != -1) {
            this.setItem(slot, itemStack, clickConsumer);
        }
    }

    /**
     * Fills a row of consecutive slots.
     *
     * @param startSlot first slot
     * @param endSlot last slot (included)
     * @param itemStack item
     */
    public void setHorizontalLine(int startSlot, int endSlot, ItemStack itemStack) {
        for (int i = startSlot; i <= endSlot; i++) {
            this.setItem(i, itemStack);
        }
    }

    /**
     * Fills a column of slots.
     *
     * @param startSlot first slot
     * @param endSlot last slot (included)
     * @param itemStack item
     */
    public void setVerticalLine(int startSlot, int endSlot, ItemStack itemStack) {
        for (int i = startSlot; i <= endSlot; i += 9) {
            this.setItem(i, itemStack);
        }
    }

    /**
     * Fills the empty slots.
     *
     * @param itemStack filler item
     */
    public void setFill(ItemStack itemStack) {
        for (int i = 0; i < this.size; i++) {
            if (this.inventory.getItem(i) == null) {
                this.setItem(i, itemStack);
            }
        }
    }

    /**
     * Nameless decoration pane.
     *
     * @param color glass data value
     * @return the pane
     */
    protected static ItemStack pane(int color) {
        return new ItemBuilder(Material.STAINED_GLASS_PANE, 1, color).withName(" ").build();
    }

    /**
     * Clears the inventory and every action.
     */
    public void clear() {
        this.inventory.clear();
        this.clickConsumers.clear();
    }

    public void open() {
        this.owner.openInventory(this.inventory);
    }

    /**
     * Fills the border of the menu (first and last row plus both sides).
     *
     * @param itemStack border item
     */
    public void setBorder(ItemStack itemStack) {
        for (int i = 0; i < this.size; i++) {
            final int column = i % 9;

            if (i < 9 || i >= this.size - 9 || column == 0 || column == 8) {
                this.setItem(i, itemStack);
            }
        }
    }

    public void onOpen(InventoryOpenEvent event) {}

    public void onClose(InventoryCloseEvent event) {}

    public void onClick(InventoryClickEvent event) {}

    @Override
    public Inventory getInventory() {
        return this.inventory;
    }

    public Player getOwner() {
        return this.owner;
    }

    public String getName() {
        return this.name;
    }

    public int getSize() {
        return this.size;
    }

    Map<Integer, Consumer<InventoryClickEvent>> getClickConsumers() {
        return this.clickConsumers;
    }

    public boolean isCancelClickEvent() {
        return this.cancelClickEvent;
    }

}
