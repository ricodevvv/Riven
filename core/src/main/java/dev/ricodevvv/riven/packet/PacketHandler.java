package dev.ricodevvv.riven.packet;

import org.bukkit.entity.Player;

/**
 * Listens to packets of a given type. Heads up: it runs on the Netty thread, not the main one.
 */
public interface PacketHandler {

    /**
     * Called when the server gets the packet from the client.
     *
     * @param player player who sent it
     * @param packet NMS packet
     * @return true to cancel the packet
     */
    default boolean onReceive(Player player, Object packet) {
        return false;
    }

    /**
     * Called right before the packet goes to the client.
     *
     * @param player player who's about to get it
     * @param packet NMS packet
     * @return true to cancel the packet
     */
    default boolean onSend(Player player, Object packet) {
        return false;
    }

}
