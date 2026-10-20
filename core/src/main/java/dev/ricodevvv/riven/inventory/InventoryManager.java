package dev.ricodevvv.riven.inventory;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Dispatches clicks to the open {@link GameInventory} menus.
 */
public class InventoryManager implements Listener {

    private final JavaPlugin plugin;

    public InventoryManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        Bukkit.getPluginManager().registerEvents(this, this.plugin);
    }

    public void stop() {
        HandlerList.unregisterAll(this);

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof GameInventory) {
                player.closeInventory();
            }
        }
    }

    /**
     * Every open menu of a type.
     *
     * @param inventoryClass menu class
     * @param <T> type
     * @return the menus open right now
     */
    public <T extends GameInventory> List<T> getOpenInventories(Class<T> inventoryClass) {
        final List<T> result = new ArrayList<>();

        for (Player player : Bukkit.getOnlinePlayers()) {
            final Object holder = player.getOpenInventory().getTopInventory().getHolder();

            if (inventoryClass.isInstance(holder)) {
                result.add(inventoryClass.cast(holder));
            }
        }
        return result;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof GameInventory) || event.getClickedInventory() == null) {
            return;
        }

        final GameInventory inventory = (GameInventory) event.getInventory().getHolder();

        event.setCancelled(inventory.isCancelClickEvent());

        inventory.onClick(event);

        final Consumer<InventoryClickEvent> consumer = inventory.getClickConsumers().get(event.getRawSlot());

        if (consumer != null && event.getRawSlot() < inventory.getSize()) {
            consumer.accept(event);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof GameInventory && ((GameInventory) event.getInventory().getHolder()).isCancelClickEvent()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onOpen(InventoryOpenEvent event) {
        if (event.getInventory().getHolder() instanceof GameInventory) {
            ((GameInventory) event.getInventory().getHolder()).onOpen(event);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof GameInventory) {
            ((GameInventory) event.getInventory().getHolder()).onClose(event);
        }
    }

}
