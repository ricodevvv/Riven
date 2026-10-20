package dev.ricodevvv.riven.util;

import net.minecraft.server.v1_8_R3.PacketPlayOutEntityEquipment;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

/**
 * Things you do to players all the time: reset them, turn them into spectators, hide their armor...
 */
public final class PlayerUtil {

    private PlayerUtil() {}

    /**
     * Leaves the player with full health, food and no experience.
     *
     * @param player player
     * @param hard true to also clear the inventory and close whatever they have open
     */
    public static void resetPlayer(Player player, boolean hard) {
        if (!(player instanceof CraftPlayer) || !player.isOnline()) {
            return;
        }

        player.setFoodLevel(20);
        player.setSaturation(20.0F);
        player.setMaxHealth(20.0D);
        player.setHealth(20.0D);
        player.setLevel(0);
        player.setExp(0.0F);
        player.setFireTicks(0);
        player.setFallDistance(0.0F);

        resetPotionEffects(player);
        removeArrowsFromBody(player);

        if (hard) {
            player.closeInventory();
            player.setVelocity(new Vector(0.0D, 0.0D, 0.0D));
            player.setItemOnCursor(null);

            resetPlayerInventory(player);
        }
    }

    public static void resetPotionEffects(Player player) {
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
    }

    public static void resetPlayerInventory(Player player) {
        final PlayerInventory inventory = player.getInventory();

        inventory.setArmorContents(null);
        inventory.clear();
    }

    /**
     * Gives the player the spectator basics: flight, night vision and adventure mode.
     *
     * @param player player
     */
    public static void addSpectatorAbilities(Player player) {
        player.getInventory().setHeldItemSlot(0);
        player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 1, false, false));
        player.setGameMode(GameMode.ADVENTURE);
        player.setAllowFlight(true);
        player.setFlying(true);
    }

    /**
     * Removes the arrows stuck in the player's body.
     *
     * @param player player
     */
    public static void removeArrowsFromBody(Player player) {
        ((CraftPlayer) player).getHandle().getDataWatcher().watch(9, (byte) 0);
    }

    /**
     * Hides a player's armor for someone else (used with invisibility).
     *
     * @param player player whose armor gets hidden
     * @param target player that stops seeing it
     */
    public static void hideArmor(Player player, Player target) {
        for (int i = 1; i <= 4; i++) {
            PacketUtil.sendPacket(target, new PacketPlayOutEntityEquipment(player.getEntityId(), i, CraftItemStack.asNMSCopy(new ItemStack(Material.AIR))));
        }
    }

    /**
     * Shows a player's armor again for someone else.
     *
     * @param player player whose armor gets shown
     * @param target player that sees it again
     */
    public static void showArmor(Player player, Player target) {
        final ItemStack[] armor = player.getInventory().getArmorContents();

        for (int i = 1; i <= 4; i++) {
            PacketUtil.sendPacket(target, new PacketPlayOutEntityEquipment(player.getEntityId(), i, CraftItemStack.asNMSCopy(armor[i - 1])));
        }
    }

}
