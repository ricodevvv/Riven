package dev.ricodevvv.riven.item;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

/**
 * {@link GameItem} registry plus the listener that hands them their clicks.
 */
public class ItemManager implements Listener {

    private final JavaPlugin plugin;
    private final Map<String, GameItem> items = new HashMap<>();

    public ItemManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        Bukkit.getPluginManager().registerEvents(this, this.plugin);
    }

    public void stop() {
        HandlerList.unregisterAll(this);
    }

    public void register(GameItem item) {
        this.items.put(item.getId(), item);
    }

    public GameItem getItem(String id) {
        return this.items.get(id);
    }

    /**
     * Gives a registered item to a player.
     *
     * @param player player
     * @param slot inventory slot
     * @param id item id
     */
    public void give(Player player, int slot, String id) {
        final GameItem item = this.items.get(id);

        if (item == null) {
            throw new IllegalArgumentException("There's no item registered with id " + id);
        }

        player.getInventory().setItem(slot, item.create(player, slot));
    }

    /**
     * Finds which registered item this ItemStack is.
     *
     * @param itemStack item
     * @return the {@link GameItem}, or null if it isn't one of ours
     */
    public GameItem getItem(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType().name().equals("AIR")) {
            return null;
        }

        final ItemNBT nbt = new ItemNBT(itemStack);

        return nbt.hasTag(GameItem.TAG) ? this.items.get(nbt.getString(GameItem.TAG)) : null;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onInteract(PlayerInteractEvent event) {
        final GameItem item = this.getItem(event.getItem());

        if (item == null || event.getAction() == Action.PHYSICAL) {
            return;
        }

        event.setCancelled(true);

        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            item.onRightClick(event);
        } else {
            item.onLeftClick(event);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onClick(InventoryClickEvent event) {
        final GameItem item = this.getItem(event.getCurrentItem());

        if (item != null) {
            event.setCancelled(true);
            item.onInventoryClick(event);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onDrop(PlayerDropItemEvent event) {
        if (this.getItem(event.getItemDrop().getItemStack()) != null) {
            event.setCancelled(true);
        }
    }

}
