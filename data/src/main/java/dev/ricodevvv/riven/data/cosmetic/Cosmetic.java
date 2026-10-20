package dev.ricodevvv.riven.data.cosmetic;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

/**
 * One cosmetic of any type (cage, balloon, trail, kill sound...). What it actually does is up to the plugin:
 * everything type specific stays in {@link #getData()}, the raw config section.
 */
public class Cosmetic {

    private final String type;
    private final String id;
    private final String name;
    private final ItemStack icon;
    private final long price;
    private final String currency;
    private final Rarity rarity;
    private final boolean autoUnlock;
    private final boolean purchasable;
    private final ConfigurationSection data;

    public Cosmetic(String type, String id, String name, ItemStack icon, long price, String currency, Rarity rarity, boolean autoUnlock, boolean purchasable, ConfigurationSection data) {
        this.type = type.toLowerCase();
        this.id = id.toLowerCase();
        this.name = name;
        this.icon = icon;
        this.price = price;
        this.currency = currency;
        this.rarity = rarity;
        this.autoUnlock = autoUnlock;
        this.purchasable = purchasable;
        this.data = data;
    }

    /**
     * @param prefix permission prefix of the plugin, like "skywars"
     * @return the permission that unlocks it: {@code <prefix>.cosmetic.<type>.<id>}
     */
    public String getPermission(String prefix) {
        return prefix + ".cosmetic." + this.type + "." + this.id;
    }

    public String getType() {
        return this.type;
    }

    public String getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public ItemStack getIcon() {
        return this.icon.clone();
    }

    public long getPrice() {
        return this.price;
    }

    public String getCurrency() {
        return this.currency;
    }

    public Rarity getRarity() {
        return this.rarity;
    }

    public boolean isAutoUnlock() {
        return this.autoUnlock;
    }

    public boolean isPurchasable() {
        return this.purchasable;
    }

    public ConfigurationSection getData() {
        return this.data;
    }

}
