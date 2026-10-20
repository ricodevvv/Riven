package dev.ricodevvv.riven.game.protocol;

import dev.ricodevvv.riven.game.Game;
import dev.ricodevvv.riven.game.GamePlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Remembers who hit whom for a few seconds to know who gets the kill.
 * Port of HyriLastHitterProtocol, now it also counts arrows, fireballs and TNT.
 */
public class LastHitterProtocol extends GameProtocol {

    /**
     * Metadata with the UUID of whoever caused an explosion (TNT or fireball), to know who gets the kill.
     */
    public static final String EXPLOSION_SOURCE = "ubw-explosion-source";

    private final Map<UUID, List<LastHitter>> lastHitters = new ConcurrentHashMap<>();
    private final long removeTime;

    /**
     * @param game game
     * @param removeTime ticks a hit is remembered for
     */
    public LastHitterProtocol(Game<?> game, long removeTime) {
        super(game, "last_hitter");
        this.removeTime = removeTime;
    }

    @Override
    protected void enable() {}

    @Override
    protected void disable() {
        for (List<LastHitter> hitters : this.lastHitters.values()) {
            for (LastHitter hitter : hitters) {
                hitter.cancelRemoveTask();
            }
        }

        this.lastHitters.clear();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        final Player player = (Player) event.getEntity();
        final Player hitter = this.resolveDamager(event.getDamager());

        if (hitter == null || hitter == player) {
            return;
        }

        final GamePlayer victimPlayer = this.getGamePlayer(player);
        final GamePlayer hitterPlayer = this.getGamePlayer(hitter);

        if (victimPlayer == null || hitterPlayer == null || hitterPlayer.isSpectator()) {
            return;
        }

        if (this.game.areInSameTeam(player, hitter) && !hitterPlayer.getTeam().isFriendlyFire()) {
            return;
        }

        this.addHit(player, hitter);
    }

    private Player resolveDamager(Entity damager) {
        if (damager instanceof Player) {
            return (Player) damager;
        }

        if (damager instanceof Projectile && ((Projectile) damager).getShooter() instanceof Player) {
            return (Player) ((Projectile) damager).getShooter();
        }

        if (damager instanceof TNTPrimed && ((TNTPrimed) damager).getSource() instanceof Player) {
            return (Player) ((TNTPrimed) damager).getSource();
        }
        return explosionSource(damager);
    }

    /**
     * @param entity TNT or fireball
     * @return the player who placed or threw it, if they're still online
     */
    public static Player explosionSource(Entity entity) {
        for (MetadataValue value : entity.getMetadata(EXPLOSION_SOURCE)) {
            if (value.value() instanceof UUID) {
                return Bukkit.getPlayer((UUID) value.value());
            }
        }
        return null;
    }

    /**
     * Records a hit by hand (for example from a fireball or a knockback).
     *
     * @param player the one who got hit
     * @param hitter the one who hit
     */
    public void addHit(Player player, Player hitter) {
        final List<LastHitter> hitters = this.lastHitters.computeIfAbsent(player.getUniqueId(), uuid -> new ArrayList<>());
        LastHitter lastHitter = null;

        for (LastHitter current : hitters) {
            current.last = false;

            if (current.getUniqueId().equals(hitter.getUniqueId())) {
                lastHitter = current;
            }
        }

        if (lastHitter == null) {
            lastHitter = new LastHitter(this.game, hitter.getUniqueId());
            hitters.add(lastHitter);
        } else {
            lastHitter.hits++;
        }

        lastHitter.last = true;

        this.scheduleRemove(player.getUniqueId(), lastHitter);
    }

    private void scheduleRemove(UUID playerId, LastHitter hitter) {
        hitter.cancelRemoveTask();
        hitter.removeTask = Bukkit.getScheduler().runTaskLater(this.game.getPlugin(), () -> {
            final List<LastHitter> hitters = this.lastHitters.get(playerId);

            if (hitters != null) {
                hitters.remove(hitter);

                if (hitters.isEmpty()) {
                    this.lastHitters.remove(playerId);
                }
            }
        }, this.removeTime);
    }

    /**
     * @param player player
     * @return the last people who hit them sorted by amount of hits, or null if nobody did
     */
    public List<LastHitter> getLastHitters(Player player) {
        final List<LastHitter> hitters = this.lastHitters.get(player.getUniqueId());

        if (hitters == null || hitters.isEmpty()) {
            return null;
        }

        final List<LastHitter> sorted = new ArrayList<>(hitters);

        sorted.sort((first, second) -> {
            if (first.last != second.last) {
                return first.last ? -1 : 1;
            }
            return second.hits - first.hits;
        });
        return sorted;
    }

    public void removeLastHitters(Player player) {
        final List<LastHitter> hitters = this.lastHitters.remove(player.getUniqueId());

        if (hitters != null) {
            for (LastHitter hitter : hitters) {
                hitter.cancelRemoveTask();
            }
        }
    }

    /**
     * Someone who hit a player recently.
     */
    public static class LastHitter {

        private final Game<?> game;
        private final UUID player;
        private int hits = 1;
        private boolean last;
        private BukkitTask removeTask;

        LastHitter(Game<?> game, UUID player) {
            this.game = game;
            this.player = player;
        }

        public UUID getUniqueId() {
            return this.player;
        }

        public Player asPlayer() {
            return Bukkit.getPlayer(this.player);
        }

        public GamePlayer asGamePlayer() {
            return this.game.getPlayer(this.player);
        }

        public int getHits() {
            return this.hits;
        }

        public boolean isLast() {
            return this.last;
        }

        void cancelRemoveTask() {
            if (this.removeTask != null) {
                this.removeTask.cancel();
            }
        }

    }

}
