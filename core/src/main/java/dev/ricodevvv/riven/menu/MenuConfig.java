package dev.ricodevvv.riven.menu;

import dev.ricodevvv.riven.config.ItemParser;
import dev.ricodevvv.riven.util.TextUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A menu read from {@code menus/<id>.yml}:
 *
 * <pre>
 * title: "&amp;8Cosmetics"
 * rows: 6
 * fill: "STAINED_GLASS_PANE:7 name:&amp;r"
 * list-slots: ["10-16", "19-25"]
 * previous-page: { slot: 45, material: ARROW, name: "&amp;aPrevious" }
 * next-page: { slot: 53, material: ARROW, name: "&amp;aNext" }
 * items:
 *   close:
 *     slot: 49
 *     material: BARRIER
 *     name: "&amp;cClose"
 *     actions: ["[close]"]
 * </pre>
 *
 * Actions: {@code [close]}, {@code [open] menu}, {@code [back]}, {@code [command] cmd}, {@code [console] cmd},
 * {@code [message] text}, {@code [sound] SOUND:vol:pitch}, {@code [button] id} plus any action registered
 * in {@link MenuRegistry}. {@code %player%} and any placeholder work in names, lores and actions.
 */
public class MenuConfig {

    private final String id;
    private final String title;
    private final int rows;
    private final ItemStack fill;
    private final List<Integer> listSlots;
    private final MenuItemConfig previousPage;
    private final MenuItemConfig nextPage;
    private final Map<String, MenuItemConfig> items;
    private final List<String> openActions;

    public MenuConfig(String id, String title, int rows, ItemStack fill, List<Integer> listSlots, MenuItemConfig previousPage, MenuItemConfig nextPage, Map<String, MenuItemConfig> items, List<String> openActions) {
        this.id = id;
        this.title = title;
        this.rows = rows;
        this.fill = fill;
        this.listSlots = listSlots;
        this.previousPage = previousPage;
        this.nextPage = nextPage;
        this.items = items;
        this.openActions = openActions;
    }

    /**
     * @param id menu id
     * @param section root section of the file
     * @return the menu
     */
    public static MenuConfig parse(String id, ConfigurationSection section) {
        final Map<String, MenuItemConfig> items = new LinkedHashMap<>();
        final ConfigurationSection itemsSection = section.getConfigurationSection("items");

        if (itemsSection != null) {
            for (String key : itemsSection.getKeys(false)) {
                final ConfigurationSection itemSection = itemsSection.getConfigurationSection(key);

                if (itemSection != null) {
                    final MenuItemConfig item = MenuItemConfig.parse(key, itemSection);

                    if (item != null) {
                        items.put(key, item);
                    }
                }
            }
        }

        final List<Integer> listSlots = new ArrayList<>();

        for (String slots : section.getStringList("list-slots")) {
            listSlots.addAll(MenuItemConfig.parseSlots(slots));
        }

        final ConfigurationSection previous = section.getConfigurationSection("previous-page");
        final ConfigurationSection next = section.getConfigurationSection("next-page");

        return new MenuConfig(id,
                TextUtil.colorize(section.getString("title", id)),
                Math.max(1, Math.min(6, section.getInt("rows", 6))),
                section.contains("fill") ? ItemParser.parse(section.getString("fill")) : null,
                listSlots,
                previous == null ? null : MenuItemConfig.parse("previous-page", previous),
                next == null ? null : MenuItemConfig.parse("next-page", next),
                items,
                section.getStringList("open-actions"));
    }

    public String getId() {
        return this.id;
    }

    public String getTitle() {
        return this.title;
    }

    public int getRows() {
        return this.rows;
    }

    public ItemStack getFill() {
        return this.fill == null ? null : this.fill.clone();
    }

    public List<Integer> getListSlots() {
        return Collections.unmodifiableList(this.listSlots);
    }

    public MenuItemConfig getPreviousPage() {
        return this.previousPage;
    }

    public MenuItemConfig getNextPage() {
        return this.nextPage;
    }

    public Map<String, MenuItemConfig> getItems() {
        return Collections.unmodifiableMap(this.items);
    }

    public MenuItemConfig getItem(String key) {
        return this.items.get(key);
    }

    public List<String> getOpenActions() {
        return this.openActions;
    }

}
