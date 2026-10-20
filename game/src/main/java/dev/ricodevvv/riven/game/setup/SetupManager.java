package dev.ricodevvv.riven.game.setup;

import dev.ricodevvv.riven.Riven;
import dev.ricodevvv.riven.game.setup.handler.BooleanHandler;
import dev.ricodevvv.riven.game.setup.handler.ConfigOptionHandler;
import dev.ricodevvv.riven.game.setup.handler.LocationConsumer;
import dev.ricodevvv.riven.item.GameItem;
import dev.ricodevvv.riven.item.ItemManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Builds and tracks the guided setup processes (HyriBuild's ConfigManager). Messages use the {@code setup.*}
 * keys of the plugin and fall back to English.
 *
 * <pre>
 * SetupManager setup = new SetupManager(plugin);
 * setup.start();
 * setup.start(player, new SkyWarsMapConfig(islands)).withFinishCallback(config -&gt; save(config));
 * </pre>
 */
public class SetupManager implements Listener {

    public static final String LOCATION_ITEM = "setup_location";
    public static final String YES_ITEM = "setup_yes";
    public static final String NO_ITEM = "setup_no";
    public static final String SKIP_ITEM = "setup_skip";

    private static final Map<String, String> DEFAULTS = new HashMap<>();

    static {
        DEFAULTS.put("setup.process.start", "&7Starting the &b%type% &7setup.\n&8▪ &b%amount% &7values to define.");
        DEFAULTS.put("setup.process.title", "&bSetup");
        DEFAULTS.put("setup.process.finished", "&7The map setup is &adone&7.");
        DEFAULTS.put("setup.process.finished-title", "Done");
        DEFAULTS.put("setup.process.finished-subtitle", "Ready to use");
        DEFAULTS.put("setup.process.validated", "Saved");
        DEFAULTS.put("setup.process.element", "Element");
        DEFAULTS.put("setup.process.value", "Value to define");
        DEFAULTS.put("setup.process.category", "Category");
        DEFAULTS.put("setup.process.description", "Description");
        DEFAULTS.put("setup.process.invalid-number", "&fThat number &cdoesn't work&f, try again.");
        DEFAULTS.put("setup.area.first", "&7Mark the &bfirst corner &7of the area.");
        DEFAULTS.put("setup.area.second", "&7Now the &bsecond corner&7.");
        DEFAULTS.put("setup.area.first-title", "First corner");
        DEFAULTS.put("setup.area.second-title", "Second corner");
        DEFAULTS.put("setup.sign.locations", "Number of\nlocations");
        DEFAULTS.put("setup.sign.areas", "Number of\nareas");
        DEFAULTS.put("setup.sign.value", "Value\n");
        DEFAULTS.put("setup.chat.prompt", "&7Type the value in chat. &8(cancel to stop)");
        DEFAULTS.put("setup.item.location", "&bMark location &7(Right click)");
        DEFAULTS.put("setup.item.yes", "&a✔ Yes &7(Right click)");
        DEFAULTS.put("setup.item.no", "&c✘ No &7(Right click)");
        DEFAULTS.put("setup.item.skip", "&eSkip &7(Right click)");
    }

    private final JavaPlugin plugin;
    private final Map<UUID, ConfigProcess<?>> processes = new ConcurrentHashMap<>();

    public SetupManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Registers the tools (star, yes/no dyes, skip) and the listener.
     */
    public void start() {
        final ItemManager items = Riven.get().getItemManager();

        items.register(new SetupItem(LOCATION_ITEM, "setup.item.location", new ItemStack(Material.NETHER_STAR), player -> {
            final ConfigOptionHandler<?> handler = this.currentHandler(player);

            if (handler instanceof LocationConsumer) {
                ((LocationConsumer) handler).provideLocation(player.getLocation());
            }
        }));

        items.register(new SetupItem(YES_ITEM, "setup.item.yes", new ItemStack(Material.INK_SACK, 1, (short) 10), player -> this.answer(player, true)));
        items.register(new SetupItem(NO_ITEM, "setup.item.no", new ItemStack(Material.INK_SACK, 1, (short) 1), player -> this.answer(player, false)));
        items.register(new SetupItem(SKIP_ITEM, "setup.item.skip", new ItemStack(Material.FEATHER), player -> {
            final ConfigProcess<?> process = this.processes.get(player.getUniqueId());

            if (process != null) {
                process.skip();
            }
        }));

        Bukkit.getPluginManager().registerEvents(this, this.plugin);
    }

    public void stop() {
        HandlerList.unregisterAll(this);
        this.processes.clear();
    }

    private void answer(Player player, boolean value) {
        final ConfigOptionHandler<?> handler = this.currentHandler(player);

        if (handler instanceof BooleanHandler) {
            ((BooleanHandler) handler).complete(value);
        }
    }

    private ConfigOptionHandler<?> currentHandler(Player player) {
        final ConfigProcess<?> process = this.processes.get(player.getUniqueId());

        if (process == null || process.currentStep() == null) {
            return null;
        }
        return process.currentStep().getHandler();
    }

    /**
     * @param player player
     * @param id setup item id
     * @param slot slot
     */
    public void giveItem(Player player, String id, int slot) {
        Riven.get().getItemManager().give(player, slot, id);
    }

    /**
     * Starts a process to fill an existing configuration.
     *
     * @param player builder
     * @param config object annotated with {@link ConfigData}
     * @param <T> configuration type
     * @return the process, already started
     */
    public <T> ConfigProcess<T> start(Player player, T config) {
        if (this.processes.containsKey(player.getUniqueId())) {
            throw new IllegalStateException(player.getName() + " is already setting something up");
        }

        final ConfigData data = config.getClass().getAnnotation(ConfigData.class);

        if (data == null) {
            throw new IllegalArgumentException("The configuration needs the @ConfigData annotation");
        }

        final ConfigProcess<T> process = new ConfigProcess<>(this, player, config, data);

        this.loadFields(process, config, null, null);
        this.processes.put(player.getUniqueId(), process);

        process.start();

        return process;
    }

    private void loadFields(ConfigProcess<?> process, Object target, ConfigOptionCategory category, String listMemberId) {
        try {
            for (Field field : allFields(target.getClass())) {
                if (Modifier.isStatic(field.getModifiers())) {
                    continue;
                }

                final ConfigOptionCategory nested = field.getAnnotation(ConfigOptionCategory.class);

                if (nested != null) {
                    field.setAccessible(true);

                    final Object value = field.get(target);

                    if (value instanceof List) {
                        for (Object element : (List<?>) value) {
                            final String memberId = memberId(element);

                            if (memberId != null) {
                                this.loadFields(process, element, nested, memberId);
                            }
                        }
                    } else if (value != null) {
                        this.loadFields(process, value, nested, null);
                    }
                    continue;
                }

                final ConfigOption option = field.getAnnotation(ConfigOption.class);

                if (option != null) {
                    process.addStep(new ConfigStep(category, target, listMemberId, option, field));
                }
            }
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String memberId(Object element) throws IllegalAccessException {
        for (Field field : allFields(element.getClass())) {
            if (field.getAnnotation(ConfigListMemberId.class) != null && field.getType() == String.class) {
                field.setAccessible(true);
                return (String) field.get(element);
            }
        }
        return null;
    }

    private static List<Field> allFields(Class<?> clazz) {
        if (clazz == null || clazz == Object.class) {
            return Collections.emptyList();
        }

        final List<Field> result = new ArrayList<>(allFields(clazz.getSuperclass()));

        Collections.addAll(result, clazz.getDeclaredFields());
        return result;
    }

    /**
     * Stops the player's process, if any.
     *
     * @param player player
     * @return true if they had one
     */
    public boolean cancel(Player player) {
        final ConfigProcess<?> process = this.processes.remove(player.getUniqueId());

        if (process == null) {
            return false;
        }

        player.getInventory().clear();
        return true;
    }

    /**
     * Text for the builder: the language key if it exists, the English default otherwise, or the text itself.
     *
     * @param player builder
     * @param key key or text
     * @return the text
     */
    public String text(Player player, String key) {
        final String fallback = DEFAULTS.get(key);

        if (fallback != null) {
            return Riven.get().getLanguage().getOrDefault(player, key, fallback);
        }
        return Riven.get().getLanguage().has(key) ? Riven.get().getLanguage().get(player, key) : key;
    }

    void removeProcess(ConfigProcess<?> process) {
        this.processes.remove(process.getPlayer().getUniqueId(), process);
    }

    boolean isRunning(ConfigProcess<?> process) {
        return this.processes.get(process.getPlayer().getUniqueId()) == process;
    }

    public ConfigProcess<?> getProcess(Player player) {
        return this.processes.get(player.getUniqueId());
    }

    public boolean isInSetup(Player player) {
        return this.processes.containsKey(player.getUniqueId());
    }

    public JavaPlugin getPlugin() {
        return this.plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onQuit(PlayerQuitEvent event) {
        this.processes.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (this.processes.containsKey(event.getWhoClicked().getUniqueId()) && event.getClickedInventory() == event.getWhoClicked().getInventory()
                && (event.getSlot() <= 1 || event.getSlot() == 8)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (this.processes.containsKey(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPickup(PlayerPickupItemEvent event) {
        if (this.processes.containsKey(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    private final class SetupItem extends GameItem {

        private final Consumer<Player> action;

        SetupItem(String id, String nameKey, ItemStack item, Consumer<Player> action) {
            super(id, nameKey, null, item);
            this.action = action;
        }

        @Override
        protected String getDisplayName(Player player) {
            return SetupManager.this.text(player, this.nameKey);
        }

        @Override
        public void onRightClick(PlayerInteractEvent event) {
            this.action.accept(event.getPlayer());
        }

    }

}
