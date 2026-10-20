package dev.ricodevvv.riven.config;

import dev.ricodevvv.riven.item.ItemBuilder;
import dev.ricodevvv.riven.util.TextUtil;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Reads and writes items in config. Two formats are supported.
 *
 * <p>Section format:</p>
 * <pre>
 * material: DIAMOND_SWORD
 * data: 0
 * amount: 1
 * name: "&amp;bSword"
 * lore: ["&amp;7Sharp"]
 * enchantments: ["DAMAGE_ALL:2"]
 * glow: true
 * unbreakable: true
 * skull: "base64 texture or player name"
 * color: "255,0,0"
 * effects: ["SPEED:200:1"]
 * </pre>
 *
 * <p>Line format, handy for lists: {@code DIAMOND_SWORD:0 1 name:&bSword enchant:DAMAGE_ALL:2 glow}</p>
 */
public final class ItemParser {

    private ItemParser() {}

    /**
     * @param section item section
     * @return the item, or null if the material is missing or wrong
     */
    @SuppressWarnings("deprecation")
    public static ItemStack parse(ConfigurationSection section) {
        if (section == null) {
            return null;
        }

        final Material material = material(section.getString("material"));

        if (material == null) {
            return null;
        }

        final ItemBuilder builder = new ItemBuilder(material, Math.max(1, section.getInt("amount", 1)), section.getInt("data", 0));

        if (section.contains("name")) {
            builder.withName(TextUtil.colorize(section.getString("name")));
        }

        if (section.contains("lore")) {
            builder.withLore(TextUtil.colorize(section.getStringList("lore")));
        }

        for (String enchant : section.getStringList("enchantments")) {
            final String[] parts = enchant.split(":");
            final Enchantment enchantment = Enchantment.getByName(parts[0].toUpperCase());

            if (enchantment != null) {
                builder.withEnchant(enchantment, parts.length > 1 ? parseInt(parts[1], 1) : 1);
            }
        }

        if (section.getBoolean("glow")) {
            builder.withGlow();
        }

        if (section.getBoolean("unbreakable")) {
            builder.unbreakable();
        }

        if (section.getBoolean("hide-flags")) {
            builder.withAllItemFlags();
        }

        final String skull = section.getString("skull");

        if (skull != null) {
            if (skull.length() > 16) {
                builder.withHeadTexture(skull);
            } else {
                builder.withSkullOwner(skull);
            }
        }

        final Color color = color(section.getString("color"));

        if (color != null) {
            builder.withLeatherArmorColor(color);
        }

        final ItemStack item = builder.build();

        applyEffects(item, section.getStringList("effects"));

        return item;
    }

    /**
     * Parses the line format: material[:data] [amount] and then any of {@code name:}, {@code lore:} (lines split
     * with |), {@code enchant:NAME:level}, {@code glow}, {@code unbreakable}, {@code color:r,g,b},
     * {@code skull:texture}, {@code effect:TYPE:ticks:amplifier}. Underscores in names become spaces.
     *
     * @param line item line
     * @return the item, or null if the material is wrong
     */
    @SuppressWarnings("deprecation")
    public static ItemStack parse(String line) {
        if (line == null || line.trim().isEmpty()) {
            return null;
        }

        final String[] parts = line.trim().split(" ");
        final String[] head = parts[0].split(":");
        final Material material = material(head[0]);

        if (material == null) {
            return null;
        }

        int index = 1;
        int amount = 1;

        if (parts.length > 1 && parts[1].matches("\\d+")) {
            amount = Integer.parseInt(parts[1]);
            index = 2;
        }

        final ItemBuilder builder = new ItemBuilder(material, amount, head.length > 1 ? parseInt(head[1], 0) : 0);
        final List<String> effects = new ArrayList<>();

        for (int i = index; i < parts.length; i++) {
            final String part = parts[i];
            final String lower = part.toLowerCase();

            if (lower.startsWith("name:")) {
                builder.withName(TextUtil.colorize(part.substring(5).replace('_', ' ')));
            } else if (lower.startsWith("lore:")) {
                final List<String> lore = new ArrayList<>();

                for (String loreLine : part.substring(5).split("\\|")) {
                    lore.add(TextUtil.colorize(loreLine.replace('_', ' ')));
                }
                builder.withLore(lore);
            } else if (lower.startsWith("enchant:")) {
                final String[] enchant = part.substring(8).split(":");
                final Enchantment enchantment = Enchantment.getByName(enchant[0].toUpperCase());

                if (enchantment != null) {
                    builder.withEnchant(enchantment, enchant.length > 1 ? parseInt(enchant[1], 1) : 1);
                }
            } else if (lower.equals("glow")) {
                builder.withGlow();
            } else if (lower.equals("unbreakable")) {
                builder.unbreakable();
            } else if (lower.startsWith("color:")) {
                final Color color = color(part.substring(6));

                if (color != null) {
                    builder.withLeatherArmorColor(color);
                }
            } else if (lower.startsWith("skull:")) {
                builder.withHeadTexture(part.substring(6));
            } else if (lower.startsWith("effect:")) {
                effects.add(part.substring(7));
            }
        }

        final ItemStack item = builder.build();

        applyEffects(item, effects);

        return item;
    }

    /**
     * Writes an item into a section using the section format.
     *
     * @param section target section
     * @param item item
     */
    @SuppressWarnings("deprecation")
    public static void write(ConfigurationSection section, ItemStack item) {
        section.set("material", item.getType().name());

        if (item.getDurability() != 0) {
            section.set("data", item.getDurability());
        }

        if (item.getAmount() != 1) {
            section.set("amount", item.getAmount());
        }

        final ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return;
        }

        if (meta.hasDisplayName()) {
            section.set("name", meta.getDisplayName().replace('§', '&'));
        }

        if (meta.hasLore()) {
            final List<String> lore = new ArrayList<>();

            for (String line : meta.getLore()) {
                lore.add(line.replace('§', '&'));
            }
            section.set("lore", lore);
        }

        if (meta.hasEnchants()) {
            final List<String> enchants = new ArrayList<>();

            for (Map.Entry<Enchantment, Integer> entry : meta.getEnchants().entrySet()) {
                enchants.add(entry.getKey().getName() + ":" + entry.getValue());
            }
            section.set("enchantments", enchants);
        }

        if (meta.spigot().isUnbreakable()) {
            section.set("unbreakable", true);
        }

        if (meta instanceof LeatherArmorMeta) {
            final Color color = ((LeatherArmorMeta) meta).getColor();

            section.set("color", color.getRed() + "," + color.getGreen() + "," + color.getBlue());
        }

        if (meta instanceof SkullMeta && ((SkullMeta) meta).hasOwner()) {
            section.set("skull", ((SkullMeta) meta).getOwner());
        }

        if (meta instanceof PotionMeta && ((PotionMeta) meta).hasCustomEffects()) {
            final List<String> effects = new ArrayList<>();

            for (PotionEffect effect : ((PotionMeta) meta).getCustomEffects()) {
                effects.add(effect.getType().getName() + ":" + effect.getDuration() + ":" + effect.getAmplifier());
            }
            section.set("effects", effects);
        }
    }

    /**
     * Writes an item in the line format read by {@link #parse(String)}. Spaces in names become underscores.
     *
     * @param item item
     * @return the line
     */
    @SuppressWarnings("deprecation")
    public static String toLine(ItemStack item) {
        final StringBuilder builder = new StringBuilder(item.getType().name());

        if (item.getDurability() != 0) {
            builder.append(':').append(item.getDurability());
        }

        if (item.getAmount() != 1) {
            builder.append(' ').append(item.getAmount());
        }

        final ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return builder.toString();
        }

        if (meta.hasDisplayName()) {
            builder.append(" name:").append(meta.getDisplayName().replace('\u00a7', '&').replace(' ', '_'));
        }

        if (meta.hasLore()) {
            final List<String> lore = new ArrayList<>();

            for (String line : meta.getLore()) {
                lore.add(line.replace('\u00a7', '&').replace(' ', '_'));
            }
            builder.append(" lore:").append(String.join("|", lore));
        }

        for (Map.Entry<Enchantment, Integer> entry : meta.getEnchants().entrySet()) {
            builder.append(" enchant:").append(entry.getKey().getName()).append(':').append(entry.getValue());
        }

        if (meta.spigot().isUnbreakable()) {
            builder.append(" unbreakable");
        }

        if (meta instanceof LeatherArmorMeta) {
            final Color color = ((LeatherArmorMeta) meta).getColor();

            builder.append(" color:").append(color.getRed()).append(',').append(color.getGreen()).append(',').append(color.getBlue());
        }

        if (meta instanceof PotionMeta) {
            for (PotionEffect effect : ((PotionMeta) meta).getCustomEffects()) {
                builder.append(" effect:").append(effect.getType().getName()).append(':').append(effect.getDuration()).append(':').append(effect.getAmplifier());
            }
        }
        return builder.toString();
    }

    /**
     * @param name material name or legacy numeric id
     * @return the material, or null
     */
    @SuppressWarnings("deprecation")
    public static Material material(String name) {
        if (name == null) {
            return null;
        }

        final String clean = name.trim().toUpperCase();

        if (clean.matches("\\d+")) {
            return Material.getMaterial(Integer.parseInt(clean));
        }
        return Material.matchMaterial(clean);
    }

    private static void applyEffects(ItemStack item, List<String> effects) {
        if (effects.isEmpty() || !(item.getItemMeta() instanceof PotionMeta)) {
            return;
        }

        final PotionMeta meta = (PotionMeta) item.getItemMeta();

        for (String effect : effects) {
            final String[] parts = effect.split(":");
            final PotionEffectType type = PotionEffectType.getByName(parts[0].toUpperCase());

            if (type != null) {
                meta.addCustomEffect(new PotionEffect(type, parts.length > 1 ? parseInt(parts[1], 200) : 200, parts.length > 2 ? parseInt(parts[2], 0) : 0), true);
            }
        }

        item.setItemMeta(meta);
    }

    private static Color color(String text) {
        if (text == null) {
            return null;
        }

        final String[] rgb = text.split(",");

        if (rgb.length != 3) {
            return null;
        }
        return Color.fromRGB(parseInt(rgb[0].trim(), 0), parseInt(rgb[1].trim(), 0), parseInt(rgb[2].trim(), 0));
    }

    private static int parseInt(String text, int fallback) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

}
