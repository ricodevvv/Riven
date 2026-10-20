package dev.ricodevvv.riven.util;

import net.minecraft.server.v1_8_R3.EntityPlayer;
import net.minecraft.server.v1_8_R3.Packet;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.util.Collection;

/**
 * Shortcuts to send NMS packets without fighting CraftPlayer every time.
 */
public final class PacketUtil {

    private PacketUtil() {}

    /**
     * Sends a packet to a player. Does nothing if they already left.
     *
     * @param player receiver
     * @param packet packet to send
     */
    public static void sendPacket(Player player, Packet<?> packet) {
        if (player == null || packet == null || !player.isOnline() || !(player instanceof CraftPlayer)) {
            return;
        }

        final EntityPlayer handle = ((CraftPlayer) player).getHandle();

        if (handle.playerConnection != null) {
            handle.playerConnection.sendPacket(packet);
        }
    }

    public static void sendPacket(Collection<? extends Player> players, Packet<?> packet) {
        for (Player player : players) {
            sendPacket(player, packet);
        }
    }

    public static void broadcastPacket(Packet<?> packet) {
        sendPacket(Bukkit.getOnlinePlayers(), packet);
    }

    /**
     * @param player player
     * @return their ping in milliseconds, or -1 if it can't be read
     */
    public static int getPing(Player player) {
        return player instanceof CraftPlayer ? ((CraftPlayer) player).getHandle().ping : -1;
    }

}
