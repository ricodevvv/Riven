package dev.ricodevvv.riven.util;

import net.minecraft.server.v1_8_R3.ChatComponentText;
import net.minecraft.server.v1_8_R3.PacketPlayOutChat;
import org.bukkit.entity.Player;

/**
 * Messages on the action bar (right above the hotbar).
 */
public final class ActionBar {

    private ActionBar() {}

    /**
     * Shows a message on the player's action bar.
     *
     * @param player who gets it
     * @param message text, null clears it
     */
    public static void send(Player player, String message) {
        PacketUtil.sendPacket(player, new PacketPlayOutChat(new ChatComponentText(message == null ? "" : message), (byte) 2));
    }

}
