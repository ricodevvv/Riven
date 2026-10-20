package dev.ricodevvv.riven.data.cosmetic;

import org.bukkit.ChatColor;

/**
 * Cosmetic and reward rarities, with their color and a default weight for random rolls.
 */
public enum Rarity {

    COMMON(ChatColor.GREEN, 60),
    UNCOMMON(ChatColor.DARK_GREEN, 25),
    RARE(ChatColor.BLUE, 10),
    EPIC(ChatColor.DARK_PURPLE, 4),
    LEGENDARY(ChatColor.GOLD, 1);

    private final ChatColor color;
    private final int weight;

    Rarity(ChatColor color, int weight) {
        this.color = color;
        this.weight = weight;
    }

    public ChatColor getColor() {
        return this.color;
    }

    public int getWeight() {
        return this.weight;
    }

    /**
     * @param name rarity name, any case
     * @return the rarity, COMMON if the name is wrong
     */
    public static Rarity parse(String name) {
        try {
            return valueOf(name.toUpperCase());
        } catch (Exception e) {
            return COMMON;
        }
    }

}
