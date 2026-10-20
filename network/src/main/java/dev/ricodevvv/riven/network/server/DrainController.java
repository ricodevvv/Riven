package dev.ricodevvv.riven.network.server;

import com.google.gson.JsonObject;
import dev.ricodevvv.riven.network.NetworkMessenger;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.function.IntSupplier;

/**
 * Graceful restarts: once draining, the server stops taking new games (the load balancer skips it), waits for
 * the running games and players to finish and then runs the "empty" action, which shuts down by default.
 * Any server can ask another to drain through the network.
 */
public class DrainController {

    private static final String DRAIN = "server:drain";

    private final JavaPlugin plugin;
    private final ServerRegistry registry;
    private final NetworkMessenger messenger;
    private IntSupplier runningGames = () -> 0;
    private Runnable onEmpty = Bukkit::shutdown;
    private BukkitTask task;

    public DrainController(JavaPlugin plugin, ServerRegistry registry, NetworkMessenger messenger) {
        this.plugin = plugin;
        this.registry = registry;
        this.messenger = messenger;
    }

    public void start() {
        this.messenger.on(DRAIN, message -> this.drain());
    }

    /**
     * @param runningGames how many games are still being played here
     */
    public void setRunningGames(IntSupplier runningGames) {
        this.runningGames = runningGames;
    }

    /**
     * @param onEmpty what to do once nothing is left, shutdown by default
     */
    public void setOnEmpty(Runnable onEmpty) {
        this.onEmpty = onEmpty;
    }

    /**
     * Starts draining this server.
     */
    public void drain() {
        if (this.registry.isDraining()) {
            return;
        }

        this.registry.setDraining(true);
        this.plugin.getLogger().info("Draining: no new games, waiting for the current ones to end");

        this.task = Bukkit.getScheduler().runTaskTimer(this.plugin, () -> {
            if (this.runningGames.getAsInt() == 0 && Bukkit.getOnlinePlayers().isEmpty()) {
                this.task.cancel();
                this.plugin.getLogger().info("Drained, running the empty action");
                this.onEmpty.run();
            }
        }, 100L, 100L);
    }

    /**
     * Stops draining and takes games again.
     */
    public void cancel() {
        if (this.task != null) {
            this.task.cancel();
        }

        this.registry.setDraining(false);
    }

    /**
     * Asks another server to drain.
     *
     * @param server server id
     */
    public void drainRemote(String server) {
        this.messenger.send(DRAIN, new JsonObject(), server);
    }

    public boolean isDraining() {
        return this.registry.isDraining();
    }

}
