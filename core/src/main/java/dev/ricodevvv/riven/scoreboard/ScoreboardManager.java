package dev.ricodevvv.riven.scoreboard;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks each player's sidebar and ticks the animated lines.
 */
public class ScoreboardManager implements Listener {

    private final JavaPlugin plugin;
    private final Map<UUID, Sidebar> sidebars = new ConcurrentHashMap<>();
    private BukkitTask task;

    public ScoreboardManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        Bukkit.getPluginManager().registerEvents(this, this.plugin);

        this.task = Bukkit.getScheduler().runTaskTimer(this.plugin, () -> {
            for (Sidebar sidebar : this.sidebars.values()) {
                sidebar.onTick();
            }
        }, 1L, 1L);
    }

    public void stop() {
        HandlerList.unregisterAll(this);

        if (this.task != null) {
            this.task.cancel();
        }

        for (Sidebar sidebar : new ArrayList<>(this.sidebars.values())) {
            sidebar.hide();
        }
    }

    void setSidebar(Player player, Sidebar sidebar) {
        final Sidebar old = this.sidebars.put(player.getUniqueId(), sidebar);

        if (old != null && old != sidebar) {
            old.hide();
            this.sidebars.put(player.getUniqueId(), sidebar);
        }
    }

    void removeSidebar(Player player, Sidebar sidebar) {
        this.sidebars.remove(player.getUniqueId(), sidebar);
    }

    /**
     * Removes whatever sidebar the player has.
     *
     * @param player player
     */
    public void removeSidebar(Player player) {
        final Sidebar sidebar = this.sidebars.get(player.getUniqueId());

        if (sidebar != null) {
            sidebar.hide();
        }
    }

    public Sidebar getSidebar(Player player) {
        return this.sidebars.get(player.getUniqueId());
    }

    /**
     * Every sidebar of a given type.
     *
     * @param sidebarClass sidebar class
     * @param <T> type
     * @return active sidebars of that type
     */
    public <T extends Sidebar> List<T> getSidebars(Class<T> sidebarClass) {
        final List<T> result = new ArrayList<>();

        for (Sidebar sidebar : this.sidebars.values()) {
            if (sidebarClass.isInstance(sidebar)) {
                result.add(sidebarClass.cast(sidebar));
            }
        }
        return result;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        this.sidebars.remove(event.getPlayer().getUniqueId());
    }

}
