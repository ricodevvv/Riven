package dev.ricodevvv.riven.game.util.gui;

import dev.ricodevvv.riven.inventory.GameInventory;
import dev.ricodevvv.riven.item.ItemBuilder;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Spectator settings: flight speed. Port of Hyrame Impl's SpectatorSettingsItem.
 */
public class SpectatorSettingsGUI extends GameInventory {

    public SpectatorSettingsGUI(Player owner) {
        super(owner, lang(owner, "gui.spectator.settings.title"), 27);

        this.setItem(11, new ItemBuilder(Material.LEATHER_BOOTS).withName(ChatColor.GREEN + "Speed 0").withAllItemFlags().build(), event -> this.speed(-1));
        this.setItem(12, new ItemBuilder(Material.CHAINMAIL_BOOTS).withName(ChatColor.BLUE + "Speed I").withAllItemFlags().build(), event -> this.speed(0));
        this.setItem(13, new ItemBuilder(Material.IRON_BOOTS).withName(ChatColor.YELLOW + "Speed II").withAllItemFlags().build(), event -> this.speed(1));
        this.setItem(14, new ItemBuilder(Material.GOLD_BOOTS).withName(ChatColor.GOLD + "Speed III").withAllItemFlags().build(), event -> this.speed(2));
        this.setItem(15, new ItemBuilder(Material.DIAMOND_BOOTS).withName(ChatColor.RED + "Speed IV").withAllItemFlags().build(), event -> this.speed(3));
    }

    private void speed(int amplifier) {
        this.owner.removePotionEffect(PotionEffectType.SPEED);
        this.owner.setFlySpeed((amplifier + 2) * 0.1F);

        if (amplifier >= 0) {
            this.owner.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, amplifier, false, false));
        }

        this.owner.closeInventory();
    }

}
