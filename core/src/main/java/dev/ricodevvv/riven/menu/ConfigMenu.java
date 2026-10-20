package dev.ricodevvv.riven.menu;

import dev.ricodevvv.riven.inventory.GameInventory;
import dev.ricodevvv.riven.item.ItemBuilder;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link MenuConfig} opened for one player. It renders the file's items, the code buttons and the
 * paginated list, and runs the actions on click.
 */
public class ConfigMenu extends GameInventory {

    private final MenuRegistry registry;
    private final MenuConfig config;
    private final MenuContext context;
    private int page;

    public ConfigMenu(Player owner, MenuRegistry registry, MenuConfig config, MenuContext context) {
        super(owner, registry.text(owner, context, config.getTitle()), config.getRows() * 9);
        this.registry = registry;
        this.config = config;
        this.context = context;
        this.render();
    }

    /**
     * Redraws the whole menu, keeping the current page.
     */
    public void render() {
        this.clear();

        if (this.config.getFill() != null) {
            for (int i = 0; i < this.size; i++) {
                this.setItem(i, this.config.getFill());
            }
        }

        for (MenuItemConfig item : this.config.getItems().values()) {
            if (item.getPermission() != null && !this.owner.hasPermission(item.getPermission())) {
                continue;
            }

            for (int slot : item.getSlots()) {
                if (slot >= 0 && slot < this.size) {
                    this.renderItem(slot, item);
                }
            }
        }

        this.renderList();
    }

    private void renderItem(int slot, MenuItemConfig item) {
        final MenuButton button = item.getButton() == null ? null : this.registry.getButton(item.getButton());
        ItemStack stack = this.prepare(item.getItem());

        if (button != null) {
            stack = button.render(this.owner, this, stack);
        }

        if (stack == null) {
            this.setItem(slot, null);
            return;
        }

        this.setItem(slot, stack, event -> {
            final List<String> actions = new ArrayList<>(item.getActions());

            actions.addAll(event.isRightClick() ? item.getRightActions() : item.getLeftActions());

            if (button != null) {
                button.click(this.owner, this, event);
            }

            this.registry.runActions(this.owner, this, actions);
        });
    }

    private void renderList() {
        final List<Integer> slots = this.config.getListSlots();

        if (slots.isEmpty()) {
            return;
        }

        final List<MenuEntry> entries = this.context.getEntries().apply(this.owner);
        final int perPage = slots.size();
        final int pages = Math.max(1, (entries.size() + perPage - 1) / perPage);

        this.page = Math.max(0, Math.min(this.page, pages - 1));

        for (int i = 0; i < perPage; i++) {
            final int index = this.page * perPage + i;
            final int slot = slots.get(i);

            if (index < entries.size()) {
                final MenuEntry entry = entries.get(index);

                this.setItem(slot, entry.getItem(), entry.getClick());
            } else if (this.config.getFill() == null) {
                this.setItem(slot, null);
            }
        }

        if (this.page > 0 && this.config.getPreviousPage() != null) {
            this.renderPageButton(this.config.getPreviousPage(), -1);
        }

        if (this.page < pages - 1 && this.config.getNextPage() != null) {
            this.renderPageButton(this.config.getNextPage(), 1);
        }
    }

    private void renderPageButton(MenuItemConfig item, int direction) {
        for (int slot : item.getSlots()) {
            this.setItem(slot, this.prepare(item.getItem()), event -> {
                this.page += direction;
                this.render();
            });
        }
    }

    private ItemStack prepare(ItemStack item) {
        if (item == null) {
            return null;
        }

        final ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return item;
        }

        if (meta.hasDisplayName()) {
            meta.setDisplayName(this.registry.text(this.owner, this.context, meta.getDisplayName()));
        }

        if (meta.hasLore()) {
            final List<String> lore = new ArrayList<>();

            for (String line : meta.getLore()) {
                lore.add(this.registry.text(this.owner, this.context, line));
            }
            meta.setLore(lore);
        }

        if (meta instanceof SkullMeta && ((SkullMeta) meta).hasOwner() && ((SkullMeta) meta).getOwner().indexOf('%') != -1) {
            ((SkullMeta) meta).setOwner(this.registry.text(this.owner, this.context, ((SkullMeta) meta).getOwner()));
        }

        item.setItemMeta(meta);
        return new ItemBuilder(item).build();
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
    }

    public MenuConfig getConfig() {
        return this.config;
    }

    public MenuContext getContext() {
        return this.context;
    }

    public int getPage() {
        return this.page;
    }

    public void setPage(int page) {
        this.page = page;
        this.render();
    }

}
