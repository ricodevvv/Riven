package dev.ricodevvv.riven;

import dev.ricodevvv.riven.command.CommandManager;
import dev.ricodevvv.riven.hologram.HologramManager;
import dev.ricodevvv.riven.input.ChatInputManager;
import dev.ricodevvv.riven.input.SignInputManager;
import dev.ricodevvv.riven.inventory.InventoryManager;
import dev.ricodevvv.riven.item.ItemManager;
import dev.ricodevvv.riven.language.LanguageManager;
import dev.ricodevvv.riven.menu.MenuRegistry;
import dev.ricodevvv.riven.module.RivenModule;
import dev.ricodevvv.riven.npc.NPCManager;
import dev.ricodevvv.riven.packet.PacketInterceptor;
import dev.ricodevvv.riven.party.PartyManager;
import dev.ricodevvv.riven.placeholder.PlaceholderRegistry;
import dev.ricodevvv.riven.scoreboard.ScoreboardManager;
import dev.ricodevvv.riven.task.Tasks;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

/**
 * Entry point of Riven. Call {@link #init(JavaPlugin, LanguageManager)} on enable, install the modules you need
 * and call {@link #shutdown()} on disable.
 *
 * <pre>
 * Riven riven = Riven.init(this, language);
 * riven.install(new GameFramework());
 * riven.install(new DataModule(database));
 * </pre>
 *
 * Riven keeps a static instance, so if two plugins on the same server shade it, relocate it in both.
 */
public final class Riven {

    private static Riven instance;

    private final JavaPlugin plugin;
    private final LanguageManager language;
    private final PacketInterceptor packetInterceptor;
    private final ScoreboardManager scoreboardManager;
    private final InventoryManager inventoryManager;
    private final ItemManager itemManager;
    private final HologramManager hologramManager;
    private final NPCManager npcManager;
    private final SignInputManager signInputManager;
    private final ChatInputManager chatInputManager;
    private final CommandManager commandManager;
    private final PlaceholderRegistry placeholders;
    private final MenuRegistry menuRegistry;
    private final PartyManager partyManager;
    private final Tasks tasks;
    private final List<RivenModule> modules = new ArrayList<>();

    private Riven(JavaPlugin plugin, LanguageManager language) {
        this.plugin = plugin;
        this.language = language;
        this.tasks = new Tasks(plugin);
        this.packetInterceptor = new PacketInterceptor(plugin);
        this.scoreboardManager = new ScoreboardManager(plugin);
        this.inventoryManager = new InventoryManager(plugin);
        this.itemManager = new ItemManager(plugin);
        this.hologramManager = new HologramManager(plugin);
        this.npcManager = new NPCManager(plugin, this.packetInterceptor);
        this.signInputManager = new SignInputManager(plugin, this.packetInterceptor);
        this.chatInputManager = new ChatInputManager(plugin);
        this.commandManager = new CommandManager(plugin, language);
        this.placeholders = new PlaceholderRegistry(plugin);
        this.menuRegistry = new MenuRegistry(plugin, this.placeholders);
        this.partyManager = new PartyManager(plugin);
    }

    /**
     * Starts Riven. Call it once from your plugin's onEnable.
     *
     * @param plugin owner plugin
     * @param language loaded languages
     * @return the new instance
     */
    public static Riven init(JavaPlugin plugin, LanguageManager language) {
        if (instance != null) {
            throw new IllegalStateException("Riven is already running");
        }

        instance = new Riven(plugin, language);
        instance.start();

        return instance;
    }

    /**
     * @return true if {@link #init(JavaPlugin, LanguageManager)} was already called
     */
    public static boolean isRunning() {
        return instance != null;
    }

    /**
     * @return the running instance
     */
    public static Riven get() {
        if (instance == null) {
            throw new IllegalStateException("Riven isn't running, call Riven.init first");
        }
        return instance;
    }

    private void start() {
        this.packetInterceptor.start();
        this.scoreboardManager.start();
        this.inventoryManager.start();
        this.itemManager.start();
        this.hologramManager.start();
        this.npcManager.start();
        this.signInputManager.start();
        this.chatInputManager.start();
        this.placeholders.start();
        this.partyManager.start();
    }

    /**
     * Installs and enables a module. If it blows up while enabling, the error goes up and nothing is kept.
     *
     * @param module module
     * @param <T> module type
     * @return the same module
     */
    public <T extends RivenModule> T install(T module) {
        if (this.getModule(module.getClass()) != null) {
            throw new IllegalStateException(module.getName() + " is already installed");
        }

        module.enable(this);

        this.modules.add(module);
        return module;
    }

    /**
     * @param moduleClass module class (or a parent of it)
     * @param <T> module type
     * @return the installed module, or null
     */
    public <T extends RivenModule> T getModule(Class<T> moduleClass) {
        for (RivenModule module : this.modules) {
            if (moduleClass.isInstance(module)) {
                return moduleClass.cast(module);
            }
        }
        return null;
    }

    /**
     * Same as {@link #getModule(Class)} but fails loudly when it's missing.
     *
     * @param moduleClass module class
     * @param <T> module type
     * @return the installed module
     */
    public <T extends RivenModule> T requireModule(Class<T> moduleClass) {
        final T module = this.getModule(moduleClass);

        if (module == null) {
            throw new IllegalStateException(moduleClass.getSimpleName() + " isn't installed");
        }
        return module;
    }

    public List<RivenModule> getModules() {
        return Collections.unmodifiableList(this.modules);
    }

    /**
     * Shuts everything down: modules first (in reverse order) and then the core managers.
     */
    public void shutdown() {
        for (int i = this.modules.size() - 1; i >= 0; i--) {
            final RivenModule module = this.modules.get(i);

            try {
                module.disable();
            } catch (Exception e) {
                this.plugin.getLogger().log(Level.SEVERE, "Error disabling " + module.getName(), e);
            }
        }

        this.modules.clear();
        this.partyManager.stop();
        this.commandManager.unregisterAll();
        this.placeholders.stop();
        this.chatInputManager.stop();
        this.npcManager.stop();
        this.hologramManager.stop();
        this.signInputManager.stop();
        this.itemManager.stop();
        this.inventoryManager.stop();
        this.scoreboardManager.stop();
        this.packetInterceptor.stop();
        this.tasks.cancelAll();

        instance = null;
    }

    public JavaPlugin getPlugin() {
        return this.plugin;
    }

    public LanguageManager getLanguage() {
        return this.language;
    }

    public PacketInterceptor getPacketInterceptor() {
        return this.packetInterceptor;
    }

    public ScoreboardManager getScoreboardManager() {
        return this.scoreboardManager;
    }

    public InventoryManager getInventoryManager() {
        return this.inventoryManager;
    }

    public ItemManager getItemManager() {
        return this.itemManager;
    }

    public HologramManager getHologramManager() {
        return this.hologramManager;
    }

    public NPCManager getNPCManager() {
        return this.npcManager;
    }

    public SignInputManager getSignInputManager() {
        return this.signInputManager;
    }

    public ChatInputManager getChatInputManager() {
        return this.chatInputManager;
    }

    public CommandManager getCommandManager() {
        return this.commandManager;
    }

    public PlaceholderRegistry getPlaceholders() {
        return this.placeholders;
    }

    public MenuRegistry getMenuRegistry() {
        return this.menuRegistry;
    }

    public PartyManager getPartyManager() {
        return this.partyManager;
    }

    public Tasks getTasks() {
        return this.tasks;
    }

}
