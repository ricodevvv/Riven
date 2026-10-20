package dev.ricodevvv.riven.game.vote;

import org.bukkit.inventory.ItemStack;

/**
 * One choice of a vote (OP chests, 5 hearts, night...).
 *
 * @param <T> value the option applies when it wins
 */
public class VoteOption<T> {

    private final String id;
    private final String nameKey;
    private final ItemStack icon;
    private final String permission;
    private final T value;

    /**
     * @param id option id, unique in its category
     * @param nameKey language key (or plain text) of the name
     * @param icon menu icon
     * @param permission needed to vote it, null for everyone
     * @param value whatever the option means for the game
     */
    public VoteOption(String id, String nameKey, ItemStack icon, String permission, T value) {
        this.id = id.toLowerCase();
        this.nameKey = nameKey;
        this.icon = icon;
        this.permission = permission;
        this.value = value;
    }

    public String getId() {
        return this.id;
    }

    public String getNameKey() {
        return this.nameKey;
    }

    public ItemStack getIcon() {
        return this.icon.clone();
    }

    public String getPermission() {
        return this.permission;
    }

    public T getValue() {
        return this.value;
    }

}
