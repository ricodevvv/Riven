package dev.ricodevvv.riven.game;

import dev.ricodevvv.riven.Riven;
import dev.ricodevvv.riven.game.generator.GeneratorManager;
import dev.ricodevvv.riven.game.util.GameItems;
import dev.ricodevvv.riven.hologram.HologramManager;
import dev.ricodevvv.riven.input.SignInputManager;
import dev.ricodevvv.riven.inventory.InventoryManager;
import dev.ricodevvv.riven.item.ItemManager;
import dev.ricodevvv.riven.language.LanguageManager;
import dev.ricodevvv.riven.module.RivenModule;
import dev.ricodevvv.riven.npc.NPCManager;
import dev.ricodevvv.riven.packet.PacketInterceptor;
import dev.ricodevvv.riven.scoreboard.ScoreboardManager;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * The game engine module. It's the "local" version of Hyrame: instead of depending on Hyriode's network,
 * everything lives inside the server and many games run at the same time (one per arena).
 *
 * <pre>
 * GameFramework framework = GameFramework.init(plugin, languageManager);
 * framework.getGameManager().register(myGame);
 * </pre>
 *
 * {@link #init(JavaPlugin, LanguageManager)} starts Riven if it isn't running yet and installs this module.
 * If you already started Riven, use {@code riven.install(new GameFramework())} instead.
 */
public final class GameFramework implements RivenModule {

    private static GameFramework instance;

    private Riven riven;
    private GeneratorManager generatorManager;
    private GameManager gameManager;

    private Consumer<Player> leaveHandler = player -> player.kickPlayer("");
    private BiConsumer<Player, Game<?>> playAgainHandler = (player, game) -> this.leaveHandler.accept(player);
    private String serverAddress = "play.server.net";
    private Function<Player, String> chatPrefix = player -> "";
    private Function<GamePlayer, String> deathMessagePrefix = killer -> "message.death.";

    /**
     * Starts Riven (if needed) and the game engine. Call it once from your plugin's onEnable.
     *
     * @param plugin owner plugin
     * @param language loaded languages
     * @return the game engine
     */
    public static GameFramework init(JavaPlugin plugin, LanguageManager language) {
        final Riven riven = Riven.isRunning() ? Riven.get() : Riven.init(plugin, language);

        return riven.install(new GameFramework());
    }

    @Override
    public void enable(Riven riven) {
        if (instance != null) {
            throw new IllegalStateException("The game framework is already running");
        }

        this.riven = riven;
        this.generatorManager = new GeneratorManager(riven.getPlugin());
        this.gameManager = new GameManager(riven.getPlugin());

        instance = this;

        this.generatorManager.start();
        this.gameManager.start();

        GameItems.registerDefaults(riven.getItemManager());
    }

    @Override
    public void disable() {
        this.gameManager.stop();
        this.generatorManager.stop();

        instance = null;
    }

    /**
     * Shuts down Riven as a whole: games, generators, NPCs, holograms and the packet interceptor.
     */
    public void shutdown() {
        if (Riven.isRunning()) {
            Riven.get().shutdown();
        }
    }

    /**
     * @return the running game engine
     */
    public static GameFramework get() {
        if (instance == null) {
            throw new IllegalStateException("The game framework isn't running, call GameFramework.init first");
        }
        return instance;
    }

    public Riven getRiven() {
        return this.riven;
    }

    public JavaPlugin getPlugin() {
        return this.riven.getPlugin();
    }

    public LanguageManager getLanguage() {
        return this.riven.getLanguage();
    }

    public PacketInterceptor getPacketInterceptor() {
        return this.riven.getPacketInterceptor();
    }

    public ScoreboardManager getScoreboardManager() {
        return this.riven.getScoreboardManager();
    }

    public InventoryManager getInventoryManager() {
        return this.riven.getInventoryManager();
    }

    public ItemManager getItemManager() {
        return this.riven.getItemManager();
    }

    public HologramManager getHologramManager() {
        return this.riven.getHologramManager();
    }

    public NPCManager getNPCManager() {
        return this.riven.getNPCManager();
    }

    public SignInputManager getSignInputManager() {
        return this.riven.getSignInputManager();
    }

    public GeneratorManager getGeneratorManager() {
        return this.generatorManager;
    }

    public GameManager getGameManager() {
        return this.gameManager;
    }

    /**
     * What happens when a player uses the leave item. Plugins change it to send them to the lobby.
     *
     * @return the current handler
     */
    public Consumer<Player> getLeaveHandler() {
        return this.leaveHandler;
    }

    public void setLeaveHandler(Consumer<Player> leaveHandler) {
        this.leaveHandler = leaveHandler;
    }

    /**
     * What happens when a spectator uses the "play again" item. Plugins change it to find them another game.
     *
     * @return the current handler
     */
    public BiConsumer<Player, Game<?>> getPlayAgainHandler() {
        return this.playAgainHandler;
    }

    public void setPlayAgainHandler(BiConsumer<Player, Game<?>> playAgainHandler) {
        this.playAgainHandler = playAgainHandler;
    }

    /**
     * @return the address shown at the bottom of scoreboards
     */
    public String getServerAddress() {
        return this.serverAddress;
    }

    public void setServerAddress(String serverAddress) {
        this.serverAddress = serverAddress;
    }

    /**
     * @return what goes before the name in game chat (the level, for example)
     */
    public Function<Player, String> getChatPrefix() {
        return this.chatPrefix;
    }

    public void setChatPrefix(Function<Player, String> chatPrefix) {
        this.chatPrefix = chatPrefix;
    }

    /**
     * Where death messages come from depending on the killer (useful for message packs). Keys missing in a
     * pack fall back to "message.death.".
     *
     * @return the key prefix for a killer
     */
    public Function<GamePlayer, String> getDeathMessagePrefix() {
        return this.deathMessagePrefix;
    }

    public void setDeathMessagePrefix(Function<GamePlayer, String> deathMessagePrefix) {
        this.deathMessagePrefix = deathMessagePrefix;
    }

}
