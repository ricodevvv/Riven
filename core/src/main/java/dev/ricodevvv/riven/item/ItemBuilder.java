package dev.ricodevvv.riven.item;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Chainable item builder. It works on a copy, so the original item never changes.
 */
public class ItemBuilder {

    private final ItemStack itemStack;
    private ItemMeta itemMeta;

    public ItemBuilder(ItemStack itemStack) {
        this.itemStack = itemStack.clone();
        this.itemMeta = this.itemStack.getItemMeta();
    }

    public ItemBuilder(Material material) {
        this(new ItemStack(material));
    }

    public ItemBuilder(Material material, int amount) {
        this(new ItemStack(material, amount));
    }

    public ItemBuilder(Material material, int amount, int data) {
        this(new ItemStack(material, amount, (short) data));
    }

    /**
     * @return a builder holding an empty player head
     */
    public static ItemBuilder asHead() {
        return new ItemBuilder(Material.SKULL_ITEM, 1, 3);
    }

    public ItemBuilder withName(String name) {
        this.itemMeta.setDisplayName(name);
        return this;
    }

    /**
     * Sets the lore. Lines with line breaks get split.
     *
     * @param inputLore lines
     * @return this builder
     */
    public ItemBuilder withLore(List<String> inputLore) {
        final List<String> lore = new ArrayList<>();

        for (String line : inputLore) {
            lore.addAll(Arrays.asList(line.split("\n")));
        }

        this.itemMeta.setLore(lore);
        return this;
    }

    public ItemBuilder withLore(String... inputLore) {
        return this.withLore(Arrays.asList(inputLore));
    }

    /**
     * Adds lines at the end of the current lore.
     *
     * @param lines new lines
     * @return this builder
     */
    public ItemBuilder appendLore(String... lines) {
        final List<String> lore = this.itemMeta.hasLore() ? new ArrayList<>(this.itemMeta.getLore()) : new ArrayList<>();

        for (String line : lines) {
            lore.addAll(Arrays.asList(line.split("\n")));
        }

        this.itemMeta.setLore(lore);
        return this;
    }

    public ItemBuilder withAmount(int amount) {
        this.itemStack.setAmount(amount);
        return this;
    }

    public ItemBuilder withData(short data) {
        this.itemStack.setDurability(data);
        return this;
    }

    /**
     * Puts a player's skin on the head by name.
     *
     * @param owner player name
     * @return this builder
     */
    public ItemBuilder withSkullOwner(String owner) {
        if (this.itemMeta instanceof SkullMeta) {
            ((SkullMeta) this.itemMeta).setOwner(owner);
        }
        return this;
    }

    /**
     * Puts a base64 texture on the head.
     *
     * @param texture base64 texture
     * @return this builder
     */
    public ItemBuilder withHeadTexture(String texture) {
        if (!(this.itemMeta instanceof SkullMeta) || texture == null) {
            return this;
        }

        try {
            final GameProfile profile = new GameProfile(UUID.randomUUID(), null);

            profile.getProperties().put("textures", new Property("textures", texture));

            final Field profileField = this.itemMeta.getClass().getDeclaredField("profile");

            profileField.setAccessible(true);
            profileField.set(this.itemMeta, profile);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Couldn't set the head texture", e);
        }
        return this;
    }

    public ItemBuilder withItemFlags(ItemFlag... itemFlags) {
        this.itemMeta.addItemFlags(itemFlags);
        return this;
    }

    public ItemBuilder withAllItemFlags() {
        return this.withItemFlags(ItemFlag.values());
    }

    /**
     * Makes the item glow without showing any enchantment.
     *
     * @return this builder
     */
    public ItemBuilder withGlow() {
        this.itemMeta.addEnchant(Enchantment.LURE, 1, true);
        this.itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        return this;
    }

    public ItemBuilder withEnchant(Enchantment enchant, int level) {
        this.itemMeta.addEnchant(enchant, level, true);
        return this;
    }

    public ItemBuilder withEnchant(Enchantment enchant) {
        return this.withEnchant(enchant, 1);
    }

    public ItemBuilder unbreakable() {
        this.itemMeta.spigot().setUnbreakable(true);
        this.itemMeta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        return this;
    }

    /**
     * Replaces the lore lines and name placeholders, like {@code %coins%}.
     *
     * @param placeholder placeholder, with its symbols
     * @param value value
     * @return this builder
     */
    public ItemBuilder replace(String placeholder, Object value) {
        final String text = String.valueOf(value);

        if (this.itemMeta.hasDisplayName()) {
            this.itemMeta.setDisplayName(this.itemMeta.getDisplayName().replace(placeholder, text));
        }

        if (this.itemMeta.hasLore()) {
            final List<String> lore = new ArrayList<>();

            for (String line : this.itemMeta.getLore()) {
                lore.addAll(Arrays.asList(line.replace(placeholder, text).split("\n")));
            }

            this.itemMeta.setLore(lore);
        }
        return this;
    }

    public ItemBuilder withLeatherArmorColor(Color color) {
        if (this.itemMeta instanceof LeatherArmorMeta) {
            ((LeatherArmorMeta) this.itemMeta).setColor(color);
        }
        return this;
    }

    /**
     * Applies the changes and switches to editing the NBT tags.
     *
     * @return NBT editor for the item
     */
    public ItemNBT nbt() {
        return new ItemNBT(this.build());
    }

    /**
     * @return a copy of the item with every change applied
     */
    public ItemStack build() {
        this.itemStack.setItemMeta(this.itemMeta);
        this.itemMeta = this.itemStack.getItemMeta();
        return this.itemStack.clone();
    }

}
