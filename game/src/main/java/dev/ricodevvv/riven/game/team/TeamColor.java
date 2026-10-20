package dev.ricodevvv.riven.game.team;

import dev.ricodevvv.riven.game.GameFramework;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.entity.Player;

/**
 * Team colors with their chat and dye colors. Port of HyriGameTeamColor.
 */
public enum TeamColor {

    RED(ChatColor.RED, DyeColor.RED, "red"),
    ORANGE(ChatColor.GOLD, DyeColor.ORANGE, "orange"),
    YELLOW(ChatColor.YELLOW, DyeColor.YELLOW, "yellow"),
    DARK_GREEN(ChatColor.DARK_GREEN, DyeColor.GREEN, "dark-green"),
    GREEN(ChatColor.GREEN, DyeColor.LIME, "green"),
    CYAN(ChatColor.AQUA, DyeColor.CYAN, "cyan"),
    BLUE(ChatColor.BLUE, DyeColor.BLUE, "blue"),
    PINK(ChatColor.LIGHT_PURPLE, DyeColor.PINK, "pink"),
    PURPLE(ChatColor.DARK_PURPLE, DyeColor.PURPLE, "purple"),
    WHITE(ChatColor.WHITE, DyeColor.WHITE, "white"),
    GRAY(ChatColor.GRAY, DyeColor.GRAY, "gray"),
    BLACK(ChatColor.DARK_GRAY, DyeColor.BLACK, "black");

    private final ChatColor chatColor;
    private final DyeColor dyeColor;
    private final String key;

    TeamColor(ChatColor chatColor, DyeColor dyeColor, String key) {
        this.chatColor = chatColor;
        this.dyeColor = dyeColor;
        this.key = key;
    }

    public ChatColor getChatColor() {
        return this.chatColor;
    }

    public DyeColor getDyeColor() {
        return this.dyeColor;
    }

    /**
     * @return the color for leather armor
     */
    public Color getArmorColor() {
        return this.dyeColor.getColor();
    }

    /**
     * @return the wool/clay data value for this color
     */
    @SuppressWarnings("deprecation")
    public byte getWoolData() {
        return this.dyeColor.getWoolData();
    }

    /**
     * @param player player
     * @return the color name in the player's language
     */
    public String getDisplay(Player player) {
        return GameFramework.get().getLanguage().get(player, "color." + this.key);
    }

}
