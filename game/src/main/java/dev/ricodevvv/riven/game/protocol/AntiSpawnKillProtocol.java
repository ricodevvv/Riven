package dev.ricodevvv.riven.game.protocol;

import dev.ricodevvv.riven.game.Game;
import dev.ricodevvv.riven.game.event.GameDeathEvent;
import dev.ricodevvv.riven.game.event.GameRespawnEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A few seconds of protection after respawning. Hitting someone drops it. Port of HyriAntiSpawnKillProtocol.
 */
public class AntiSpawnKillProtocol extends GameProtocol {

    private final Map<UUID, BukkitTask> protections = new ConcurrentHashMap<>();
    private final long time;
    private final boolean hitReset;

    /**
     * @param game game
     * @param time protection ticks
     * @param hitReset true to drop the protection when hitting someone
     */
    public AntiSpawnKillProtocol(Game<?> game, long time, boolean hitReset) {
        super(game, "anti_spawn_kill");
        this.time = time;
        this.hitReset = hitReset;

        this.require(DeathProtocol.class);
    }

    @Override
    protected void enable() {}

    @Override
    protected void disable() {
        for (BukkitTask task : this.protections.values()) {
            task.cancel();
        }

        this.protections.clear();
    }

    /**
     * Protects a player by hand (for example when the game starts).
     *
     * @param player player
     */
    public void protect(Player player) {
        final UUID playerId = player.getUniqueId();
        final BukkitTask old = this.protections.put(playerId, Bukkit.getScheduler().runTaskLater(this.game.getPlugin(), () -> this.protections.remove(playerId), this.time));

        if (old != null) {
            old.cancel();
        }
    }

    public boolean isProtected(Player player) {
        return this.protections.containsKey(player.getUniqueId());
    }

    private void unprotect(UUID playerId) {
        final BukkitTask task = this.protections.remove(playerId);

        if (task != null) {
            task.cancel();
        }
    }

    @EventHandler
    public void onRespawn(GameRespawnEvent event) {
        if (event.getGame() == this.game && event.getGamePlayer().isOnline()) {
            this.protect(event.getGamePlayer().getPlayer());
        }
    }

    @EventHandler
    public void onDeath(GameDeathEvent event) {
        if (event.getGame() == this.game) {
            this.unprotect(event.getGamePlayer().getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player && this.protections.containsKey(event.getEntity().getUniqueId()) && this.isInGame((Player) event.getEntity())
                && event.getCause() != EntityDamageEvent.DamageCause.VOID) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onHit(EntityDamageByEntityEvent event) {
        Player hitter = null;

        if (event.getDamager() instanceof Player) {
            hitter = (Player) event.getDamager();
        } else if (event.getDamager() instanceof Projectile && ((Projectile) event.getDamager()).getShooter() instanceof Player) {
            hitter = (Player) ((Projectile) event.getDamager()).getShooter();
        }

        if (hitter == null || !this.protections.containsKey(hitter.getUniqueId()) || !this.isInGame(hitter)) {
            return;
        }

        if (this.hitReset) {
            this.unprotect(hitter.getUniqueId());
        } else {
            event.setCancelled(true);
        }
    }

}
