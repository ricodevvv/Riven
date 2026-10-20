package dev.ricodevvv.riven.input;

import dev.ricodevvv.riven.util.PacketUtil;
import net.minecraft.server.v1_8_R3.BlockPosition;
import net.minecraft.server.v1_8_R3.Blocks;
import net.minecraft.server.v1_8_R3.ChatMessage;
import net.minecraft.server.v1_8_R3.Container;
import net.minecraft.server.v1_8_R3.ContainerAnvil;
import net.minecraft.server.v1_8_R3.EntityHuman;
import net.minecraft.server.v1_8_R3.EntityPlayer;
import net.minecraft.server.v1_8_R3.PacketPlayOutCloseWindow;
import net.minecraft.server.v1_8_R3.PacketPlayOutOpenWindow;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_8_R3.event.CraftEventFactory;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Anvil where the player types some text (map name, builders...). Trimmed down version of Hyrame's AnvilGUI.
 */
public class AnvilInput {

    private static final int INPUT_SLOT = 0;
    private static final int OUTPUT_SLOT = 2;

    private final Plugin plugin;
    private final Player player;
    private final String initialText;
    private final BiConsumer<Player, String> completeCallback;
    private final Consumer<Player> closeCallback;
    private final Handler handler = new Handler();

    private Inventory inventory;
    private int containerId;
    private boolean open;
    private boolean completed;

    /**
     * @param plugin owner plugin
     * @param player player typing
     * @param initialText starting text
     * @param completeCallback what to do with the text
     * @param closeCallback what to do if they close it without confirming, can be null
     */
    public AnvilInput(Plugin plugin, Player player, String initialText, BiConsumer<Player, String> completeCallback, Consumer<Player> closeCallback) {
        this.plugin = plugin;
        this.player = player;
        this.initialText = initialText == null || initialText.isEmpty() ? " " : initialText;
        this.completeCallback = completeCallback;
        this.closeCallback = closeCallback;
    }

    public void open() {
        final EntityPlayer handle = ((CraftPlayer) this.player).getHandle();

        CraftEventFactory.handleInventoryCloseEvent(handle);

        handle.activeContainer = handle.defaultContainer;

        Bukkit.getPluginManager().registerEvents(this.handler, this.plugin);

        final Container container = new AnvilContainer(handle);

        this.inventory = container.getBukkitView().getTopInventory();

        final ItemStack paper = new ItemStack(Material.PAPER);
        final ItemMeta meta = paper.getItemMeta();

        meta.setDisplayName(this.initialText);
        paper.setItemMeta(meta);

        this.inventory.setItem(INPUT_SLOT, paper);
        this.containerId = handle.nextContainerCounter();

        PacketUtil.sendPacket(this.player, new PacketPlayOutOpenWindow(this.containerId, "minecraft:anvil", new ChatMessage(Blocks.ANVIL.a() + ".name")));

        handle.activeContainer = container;
        container.windowId = this.containerId;
        container.addSlotListener(handle);

        this.open = true;
    }

    private void close(boolean sendPacket) {
        if (!this.open) {
            return;
        }

        this.open = false;

        HandlerList.unregisterAll(this.handler);

        this.inventory.clear();

        if (sendPacket) {
            final EntityPlayer handle = ((CraftPlayer) this.player).getHandle();

            CraftEventFactory.handleInventoryCloseEvent(handle);

            handle.activeContainer = handle.defaultContainer;

            PacketUtil.sendPacket(this.player, new PacketPlayOutCloseWindow(this.containerId));
        }

        if (!this.completed && this.closeCallback != null) {
            this.closeCallback.accept(this.player);
        }
    }

    private class Handler implements Listener {

        @EventHandler
        public void onClick(InventoryClickEvent event) {
            if (!event.getInventory().equals(inventory)) {
                return;
            }

            event.setCancelled(true);

            if (event.getRawSlot() != OUTPUT_SLOT) {
                return;
            }

            final ItemStack result = inventory.getItem(OUTPUT_SLOT);

            if (result == null || !result.hasItemMeta() || result.getItemMeta().getDisplayName() == null) {
                return;
            }

            final String text = result.getItemMeta().getDisplayName().trim();

            if (text.isEmpty()) {
                return;
            }

            completed = true;
            close(true);

            Bukkit.getScheduler().runTask(plugin, () -> completeCallback.accept(player, text));
        }

        @EventHandler
        public void onDrag(InventoryDragEvent event) {
            if (event.getInventory().equals(inventory)) {
                event.setCancelled(true);
            }
        }

        @EventHandler
        public void onClose(InventoryCloseEvent event) {
            if (open && event.getInventory().equals(inventory)) {
                close(false);
            }
        }

    }

    private static class AnvilContainer extends ContainerAnvil {

        AnvilContainer(EntityHuman human) {
            super(human.inventory, human.world, new BlockPosition(0, 0, 0), human);
        }

        @Override
        public boolean a(EntityHuman human) {
            return true;
        }

        @Override
        public void b(EntityHuman human) {}

    }

}
