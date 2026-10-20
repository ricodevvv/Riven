package dev.ricodevvv.riven.hologram;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Keeps every hologram and decides every half second who sees them.
 */
public class HologramManager implements Listener {

    private final JavaPlugin plugin;
    private final Set<Hologram> holograms = ConcurrentHashMap.newKeySet();
    private BukkitTask task;

    public HologramManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        Bukkit.getPluginManager().registerEvents(this, this.plugin);

        this.task = Bukkit.getScheduler().runTaskTimer(this.plugin, () -> {
            for (Hologram hologram : this.holograms) {
                hologram.tick(Bukkit.getOnlinePlayers());
            }
        }, 10L, 10L);
    }

    public void stop() {
        HandlerList.unregisterAll(this);

        if (this.task != null) {
            this.task.cancel();
        }

        for (Hologram hologram : new ArrayList<>(this.holograms)) {
            hologram.destroy();
        }
    }

    void register(Hologram hologram) {
        this.holograms.add(hologram);
    }

    void unregister(Hologram hologram) {
        this.holograms.remove(hologram);
    }

    private void forget(UUID playerId) {
        for (Hologram hologram : this.holograms) {
            hologram.forget(playerId);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        this.forget(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        this.forget(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        this.forget(event.getPlayer().getUniqueId());
    }

}
