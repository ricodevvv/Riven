package dev.ricodevvv.riven.game.kit;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A kit with as many levels as you want. Level 1 is what you get when you unlock it, the rest are upgrades.
 */
public class Kit {

    private final String id;
    private final String displayName;
    private final ItemStack icon;
    private final List<String> description;
    private final String permission;
    private final boolean autoUnlock;
    private final String category;
    private final List<KitLevel> levels;

    /**
     * @param id kit id
     * @param displayName colored name
     * @param icon menu icon
     * @param description colored description lines
     * @param permission permission that unlocks it for free, can be null
     * @param autoUnlock true if everyone owns it (the default kit)
     * @param category free text to group kits (like "solo" or "team"), can be null
     * @param levels levels sorted from 1 up
     */
    public Kit(String id, String displayName, ItemStack icon, List<String> description, String permission, boolean autoUnlock, String category, List<KitLevel> levels) {
        this.id = id.toLowerCase();
        this.displayName = displayName;
        this.icon = icon;
        this.description = description == null ? new ArrayList<>() : description;
        this.permission = permission;
        this.autoUnlock = autoUnlock;
        this.category = category;
        this.levels = levels == null ? new ArrayList<>() : new ArrayList<>(levels);
    }

    /**
     * Gives the kit to a player. Items go into their slots (only empty ones are touched), armor only
     * replaces empty armor slots and effects are added.
     *
     * @param player player
     * @param level owned level, clamped to the available ones
     */
    public void apply(Player player, int level) {
        final KitLevel kitLevel = this.getLevel(level);

        if (kitLevel == null) {
            return;
        }

        final PlayerInventory inventory = player.getInventory();
        final ItemStack[] contents = kitLevel.getContents();

        for (int i = 0; i < contents.length && i < 36; i++) {
            if (contents[i] == null) {
                continue;
            }

            final ItemStack current = inventory.getItem(i);

            if (current == null || current.getType().name().equals("AIR")) {
                inventory.setItem(i, contents[i]);
            } else {
                inventory.addItem(contents[i]);
            }
        }

        final ItemStack[] armor = kitLevel.getArmor();
        final ItemStack[] worn = inventory.getArmorContents();

        for (int i = 0; i < 4 && i < armor.length; i++) {
            if (armor[i] != null && (worn[i] == null || worn[i].getType().name().equals("AIR"))) {
                worn[i] = armor[i];
            }
        }

        inventory.setArmorContents(worn);

        for (PotionEffect effect : kitLevel.getEffects()) {
            player.addPotionEffect(effect, true);
        }

        player.updateInventory();
    }

    /**
     * @param level wanted level
     * @return that level, the highest available below it, or null if the kit has none
     */
    public KitLevel getLevel(int level) {
        KitLevel result = null;

        for (KitLevel current : this.levels) {
            if (current.getLevel() <= level || result == null) {
                result = current;
            }

            if (current.getLevel() >= level) {
                break;
            }
        }
        return result;
    }

    /**
     * @param player player
     * @return true if the kit is free for them (auto unlock or permission)
     */
    public boolean isFreeFor(Player player) {
        return this.autoUnlock || (this.permission != null && player.hasPermission(this.permission));
    }

    public int getMaxLevel() {
        return this.levels.isEmpty() ? 0 : this.levels.get(this.levels.size() - 1).getLevel();
    }

    public String getId() {
        return this.id;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public ItemStack getIcon() {
        return this.icon.clone();
    }

    public List<String> getDescription() {
        return Collections.unmodifiableList(this.description);
    }

    public String getPermission() {
        return this.permission;
    }

    public boolean isAutoUnlock() {
        return this.autoUnlock;
    }

    public String getCategory() {
        return this.category;
    }

    public List<KitLevel> getLevels() {
        return Collections.unmodifiableList(this.levels);
    }

}
