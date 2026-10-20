package dev.ricodevvv.riven.menu;

import dev.ricodevvv.riven.config.SoundData;
import dev.ricodevvv.riven.placeholder.PlaceholderRegistry;
import dev.ricodevvv.riven.text.FancyMessage;
import dev.ricodevvv.riven.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Loads the editable menus from {@code menus/*.yml}, opens them and runs their actions.
 *
 * <pre>
 * registry.loadFolder("menus", "main", "cosmetics");
 * registry.registerButton("coins", new CoinsButton());
 * registry.open(player, "cosmetics", new MenuContext().entries(p -&gt; cosmeticEntries(p)));
 * </pre>
 */
public class MenuRegistry {

    private final JavaPlugin plugin;
    private final PlaceholderRegistry placeholders;
    private final Map<String, MenuConfig> menus = new ConcurrentHashMap<>();
    private final Map<String, MenuButton> buttons = new ConcurrentHashMap<>();
    private final Map<String, MenuAction> actions = new ConcurrentHashMap<>();

    public MenuRegistry(JavaPlugin plugin, PlaceholderRegistry placeholders) {
        this.plugin = plugin;
        this.placeholders = placeholders;
    }

    /**
     * Loads every yml in a folder of the plugin. Files listed in {@code bundled} are copied from the jar
     * ({@code <folder>/<name>.yml}) first if they don't exist.
     *
     * @param folder folder inside the plugin data folder
     * @param bundled menus that ship inside the jar
     */
    public void loadFolder(String folder, String... bundled) {
        final File directory = new File(this.plugin.getDataFolder(), folder);

        if (!directory.exists() && !directory.mkdirs()) {
            this.plugin.getLogger().warning("Couldn't create " + directory);
        }

        for (String name : bundled) {
            final String resource = folder + "/" + name + ".yml";

            if (!new File(this.plugin.getDataFolder(), resource).exists() && this.plugin.getResource(resource) != null) {
                this.plugin.saveResource(resource, false);
            }
        }

        final File[] files = directory.listFiles((dir, name) -> name.endsWith(".yml"));

        if (files == null) {
            return;
        }

        for (File file : files) {
            final String id = file.getName().substring(0, file.getName().length() - 4).toLowerCase();

            try {
                final YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                final java.io.InputStream defaults = this.plugin.getResource(folder + "/" + file.getName());

                if (defaults != null) {
                    config.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(defaults, StandardCharsets.UTF_8)));
                }

                this.menus.put(id, MenuConfig.parse(id, config));
            } catch (Exception e) {
                this.plugin.getLogger().log(Level.SEVERE, "Couldn't load menu " + file.getName(), e);
            }
        }
    }

    public void register(MenuConfig menu) {
        this.menus.put(menu.getId(), menu);
    }

    public void registerButton(String id, MenuButton button) {
        this.buttons.put(id.toLowerCase(), button);
    }

    /**
     * Adds a custom action, used in files as {@code [name] argument}.
     *
     * @param name action name without brackets
     * @param action what it does
     */
    public void registerAction(String name, MenuAction action) {
        this.actions.put(name.toLowerCase(), action);
    }

    public MenuConfig getMenu(String id) {
        return this.menus.get(id.toLowerCase());
    }

    public Collection<MenuConfig> getMenus() {
        return Collections.unmodifiableCollection(this.menus.values());
    }

    MenuButton getButton(String id) {
        return this.buttons.get(id.toLowerCase());
    }

    public boolean open(Player player, String id) {
        return this.open(player, id, new MenuContext());
    }

    /**
     * Opens a menu for a player.
     *
     * @param player viewer
     * @param id menu id
     * @param context replacements and list entries
     * @return false if the menu doesn't exist
     */
    public boolean open(Player player, String id, MenuContext context) {
        final MenuConfig config = this.getMenu(id);

        if (config == null) {
            this.plugin.getLogger().warning("Menu " + id + " doesn't exist");
            return false;
        }

        if (context.getPreviousMenu() == null) {
            final Object holder = player.getOpenInventory().getTopInventory().getHolder();

            if (holder instanceof ConfigMenu) {
                context.previous(((ConfigMenu) holder).getConfig().getId());
            }
        }

        this.runActions(player, null, config.getOpenActions());

        new ConfigMenu(player, this, config, context).open();
        return true;
    }

    /**
     * Runs a list of {@code [tag] argument} actions.
     *
     * @param player player
     * @param menu open menu, can be null
     * @param list actions
     */
    public void runActions(Player player, ConfigMenu menu, List<String> list) {
        for (String raw : list) {
            final String line = this.text(player, menu == null ? null : menu.getContext(), raw).trim();

            if (!line.startsWith("[")) {
                continue;
            }

            final int end = line.indexOf(']');

            if (end == -1) {
                continue;
            }

            final String tag = line.substring(1, end).toLowerCase();
            final String argument = line.substring(end + 1).trim();

            switch (tag) {
                case "close":
                    player.closeInventory();
                    break;
                case "open":
                    this.open(player, argument, new MenuContext());
                    break;
                case "back":
                    if (menu != null && menu.getContext().getPreviousMenu() != null) {
                        this.open(player, menu.getContext().getPreviousMenu());
                    } else {
                        player.closeInventory();
                    }
                    break;
                case "refresh":
                    if (menu != null) {
                        menu.render();
                    }
                    break;
                case "command":
                case "player":
                    player.chat("/" + argument);
                    break;
                case "console":
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), argument);
                    break;
                case "message":
                    FancyMessage.sendAuto(player, TextUtil.colorize(argument));
                    break;
                case "sound":
                    final SoundData sound = SoundData.parse(argument);

                    if (sound != null) {
                        sound.play(player);
                    }
                    break;
                default:
                    final MenuAction action = this.actions.get(tag);

                    if (action != null) {
                        action.run(player, menu, argument);
                    } else {
                        this.plugin.getLogger().warning("Unknown menu action [" + tag + "]");
                    }
                    break;
            }
        }
    }

    String text(Player player, MenuContext context, String text) {
        String result = text.replace("%player%", player.getName());

        if (context != null) {
            result = context.apply(result);
        }
        return TextUtil.colorize(this.placeholders.apply(player, result));
    }

}
