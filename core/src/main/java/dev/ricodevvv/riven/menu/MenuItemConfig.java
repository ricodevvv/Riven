package dev.ricodevvv.riven.menu;

import dev.ricodevvv.riven.config.ItemParser;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * One button of a {@link MenuConfig}: where it goes, what it looks like and what it does on click.
 */
public class MenuItemConfig {

    private final String key;
    private final List<Integer> slots;
    private final ItemStack item;
    private final List<String> actions;
    private final List<String> leftActions;
    private final List<String> rightActions;
    private final String permission;
    private final String button;

    public MenuItemConfig(String key, List<Integer> slots, ItemStack item, List<String> actions, List<String> leftActions, List<String> rightActions, String permission, String button) {
        this.key = key;
        this.slots = slots;
        this.item = item;
        this.actions = actions;
        this.leftActions = leftActions;
        this.rightActions = rightActions;
        this.permission = permission;
        this.button = button;
    }

    /**
     * @param key item key inside the menu
     * @param section item section
     * @return the parsed item, or null if the item part is broken
     */
    public static MenuItemConfig parse(String key, ConfigurationSection section) {
        final ItemStack item = ItemParser.parse(section);

        if (item == null && section.getString("button") == null) {
            return null;
        }

        final List<Integer> slots = new ArrayList<>();

        if (section.contains("slot")) {
            slots.add(section.getInt("slot"));
        }

        for (String slot : section.getStringList("slots")) {
            slots.addAll(parseSlots(slot));
        }

        return new MenuItemConfig(key, slots, item,
                section.getStringList("actions"),
                section.getStringList("left-actions"),
                section.getStringList("right-actions"),
                section.getString("permission"),
                section.getString("button"));
    }

    /**
     * Parses "10", "10-16" or "10,12,14".
     *
     * @param text slot text
     * @return the slots
     */
    public static List<Integer> parseSlots(String text) {
        final List<Integer> result = new ArrayList<>();

        for (String part : text.split(",")) {
            final String trimmed = part.trim();

            try {
                if (trimmed.contains("-")) {
                    final String[] range = trimmed.split("-");
                    final int from = Integer.parseInt(range[0].trim());
                    final int to = Integer.parseInt(range[1].trim());

                    for (int i = Math.min(from, to); i <= Math.max(from, to); i++) {
                        result.add(i);
                    }
                } else if (!trimmed.isEmpty()) {
                    result.add(Integer.parseInt(trimmed));
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return result;
    }

    public String getKey() {
        return this.key;
    }

    public List<Integer> getSlots() {
        return Collections.unmodifiableList(this.slots);
    }

    public ItemStack getItem() {
        return this.item == null ? null : this.item.clone();
    }

    public List<String> getActions() {
        return this.actions;
    }

    public List<String> getLeftActions() {
        return this.leftActions;
    }

    public List<String> getRightActions() {
        return this.rightActions;
    }

    public String getPermission() {
        return this.permission;
    }

    /**
     * @return id of a code-made button that renders this item, or null
     */
    public String getButton() {
        return this.button;
    }

}
