package dev.ricodevvv.riven.game.vote;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Something players vote on (chest type, health, time, final event...), with its options and the one used
 * when nobody votes.
 *
 * @param <T> option value type
 */
public class VoteCategory<T> {

    private final String id;
    private final String nameKey;
    private final ItemStack icon;
    private final List<VoteOption<T>> options = new ArrayList<>();
    private String defaultOption;

    /**
     * @param id category id
     * @param nameKey language key (or plain text) of the name
     * @param icon menu icon
     */
    public VoteCategory(String id, String nameKey, ItemStack icon) {
        this.id = id.toLowerCase();
        this.nameKey = nameKey;
        this.icon = icon;
    }

    /**
     * Adds an option. The first one added is the default until {@link #setDefault(String)} says otherwise.
     *
     * @param option option
     * @return this category
     */
    public VoteCategory<T> option(VoteOption<T> option) {
        this.options.add(option);

        if (this.defaultOption == null) {
            this.defaultOption = option.getId();
        }
        return this;
    }

    public VoteCategory<T> setDefault(String optionId) {
        this.defaultOption = optionId.toLowerCase();
        return this;
    }

    public VoteOption<T> getOption(String id) {
        for (VoteOption<T> option : this.options) {
            if (option.getId().equalsIgnoreCase(id)) {
                return option;
            }
        }
        return null;
    }

    public VoteOption<T> getDefault() {
        final VoteOption<T> option = this.getOption(this.defaultOption);

        return option != null ? option : (this.options.isEmpty() ? null : this.options.get(0));
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

    public List<VoteOption<T>> getOptions() {
        return Collections.unmodifiableList(this.options);
    }

}
