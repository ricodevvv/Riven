package dev.ricodevvv.riven.menu;

import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Extra data for one opening of a menu: text replacements and the entries of its paginated list.
 */
public class MenuContext {

    private final Map<String, String> replacements = new LinkedHashMap<>();
    private Function<Player, List<MenuEntry>> entries = player -> Collections.emptyList();
    private String previousMenu;

    /**
     * @param placeholder placeholder with its symbols, like "%cosmetic%"
     * @param value value
     * @return this context
     */
    public MenuContext with(String placeholder, Object value) {
        this.replacements.put(placeholder, String.valueOf(value));
        return this;
    }

    /**
     * Sets the elements shown in the {@code list-slots} of the menu.
     *
     * @param entries entries for a viewer
     * @return this context
     */
    public MenuContext entries(Function<Player, List<MenuEntry>> entries) {
        this.entries = entries;
        return this;
    }

    MenuContext previous(String previousMenu) {
        this.previousMenu = previousMenu;
        return this;
    }

    public String apply(String text) {
        String result = text;

        for (Map.Entry<String, String> entry : this.replacements.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }
        return result;
    }

    public Function<Player, List<MenuEntry>> getEntries() {
        return this.entries;
    }

    public String getPreviousMenu() {
        return this.previousMenu;
    }

}
