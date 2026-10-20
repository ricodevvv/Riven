package dev.ricodevvv.riven.game.util.gui;

import dev.ricodevvv.riven.game.Game;
import dev.ricodevvv.riven.inventory.GameInventory;
import dev.ricodevvv.riven.item.ItemBuilder;
import dev.ricodevvv.riven.util.Symbols;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;

/**
 * What a spectator sees of a player's inventory: the whole inventory, armor, health and effects.
 * It refreshes every second while open and nothing can be touched.
 */
public class SpectatorInventoryGUI extends GameInventory {

    private final Game<?> game;
    private final Player target;
    private BukkitTask task;

    public SpectatorInventoryGUI(Game<?> game, Player owner, Player target) {
        super(owner, lang(owner, "gui.spectator.inventory.title").replace("%player%", target.getName()), 54);
        this.game = game;
        this.target = target;
        this.fill();
    }

    private void fill() {
        final PlayerInventory inventory = this.target.getInventory();

        for (int slot = 9; slot < 36; slot++) {
            this.setItem(slot - 9, inventory.getItem(slot));
        }

        for (int slot = 0; slot < 9; slot++) {
            this.setItem(27 + slot, inventory.getItem(slot));
        }

        this.setHorizontalLine(36, 44, pane(15));

        final ItemStack[] armor = inventory.getArmorContents();

        for (int i = 0; i < armor.length; i++) {
            this.setItem(48 - i, armor[i]);
        }

        final List<String> lore = new ArrayList<>();

        lore.add(ChatColor.GRAY + lang(this.owner, "gui.spectator.inventory.health") + ": " + ChatColor.RED + (int) Math.ceil(this.target.getHealth()) + "/" + (int) this.target.getMaxHealth() + " " + Symbols.HEART);

        for (PotionEffect effect : this.target.getActivePotionEffects()) {
            lore.add(ChatColor.DARK_GRAY + Symbols.DOT_BOLD + " " + ChatColor.AQUA + effect.getType().getName() + " " + (effect.getAmplifier() + 1));
        }

        this.setItem(50, new ItemBuilder(Material.SPECKLED_MELON).withName(ChatColor.GREEN + this.target.getName()).withLore(lore).build());
        this.setItem(51, this.target.getItemInHand());
    }

    @Override
    public void open() {
        super.open();

        this.task = Bukkit.getScheduler().runTaskTimer(this.game.getPlugin(), () -> {
            if (!this.target.isOnline() || !this.owner.isOnline() || this.owner.getOpenInventory().getTopInventory().getHolder() != this) {
                this.task.cancel();
                return;
            }
            this.fill();
        }, 20L, 20L);
    }

    @Override
    public void onClose(InventoryCloseEvent event) {
        if (this.task != null) {
            this.task.cancel();
        }
    }

}
