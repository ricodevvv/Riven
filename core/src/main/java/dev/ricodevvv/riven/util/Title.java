package dev.ricodevvv.riven.util;

import net.minecraft.server.v1_8_R3.ChatComponentText;
import net.minecraft.server.v1_8_R3.PacketPlayOutPlayerListHeaderFooter;
import net.minecraft.server.v1_8_R3.PacketPlayOutTitle;
import org.bukkit.entity.Player;

/**
 * Titles, subtitles and tab header/footer using 1.8 packets.
 */
public final class Title {

    private Title() {}

    /**
     * Sends a title and subtitle to a player.
     *
     * @param player receiver
     * @param title title, can be null
     * @param subTitle subtitle, can be null
     * @param fadeIn fade in ticks
     * @param stay ticks it stays on screen
     * @param fadeOut fade out ticks
     */
    public static void sendTitle(Player player, String title, String subTitle, int fadeIn, int stay, int fadeOut) {
        PacketUtil.sendPacket(player, new PacketPlayOutTitle(fadeIn, stay, fadeOut));
        PacketUtil.sendPacket(player, new PacketPlayOutTitle(PacketPlayOutTitle.EnumTitleAction.TITLE, new ChatComponentText(title == null ? "" : title)));

        if (subTitle != null) {
            PacketUtil.sendPacket(player, new PacketPlayOutTitle(PacketPlayOutTitle.EnumTitleAction.SUBTITLE, new ChatComponentText(subTitle)));
        }
    }

    public static void clearTitle(Player player) {
        PacketUtil.sendPacket(player, new PacketPlayOutTitle(PacketPlayOutTitle.EnumTitleAction.CLEAR, null));
    }

    /**
     * Sets the tab list header and footer.
     *
     * @param player receiver
     * @param header header text
     * @param footer footer text
     */
    public static void setTabTitle(Player player, String header, String footer) {
        final PacketPlayOutPlayerListHeaderFooter packet = new PacketPlayOutPlayerListHeaderFooter(new ChatComponentText(header == null ? "" : header));

        Reflection.setField("b", packet, new ChatComponentText(footer == null ? "" : footer));

        PacketUtil.sendPacket(player, packet);
    }

}
