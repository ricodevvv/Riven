package dev.ricodevvv.riven.game.listener;

import dev.ricodevvv.riven.game.Game;
import dev.ricodevvv.riven.game.GameManager;
import dev.ricodevvv.riven.game.GamePlayer;
import dev.ricodevvv.riven.game.GameState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

/**
 * Basic rules of any game: no breaking while waiting, spectators don't touch anything,
 * no friendly fire... Port of Hyrame Impl's GameHandler, but asking which game each player is in.
 */
public class GameListener implements Listener {

    private final GameManager gameManager;

    public GameListener(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    private boolean isSpectating(Game<?> game, Player player) {
        final GamePlayer gamePlayer = game.getPlayer(player);

        return game.getSpectator(player.getUniqueId()) != null || (gamePlayer != null && (gamePlayer.isSpectator() || gamePlayer.isDead()));
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onClick(InventoryClickEvent event) {
        final Player player = (Player) event.getWhoClicked();
        final Game<?> game = this.gameManager.getGame(player);

        if (game != null && (game.getState() != GameState.PLAYING || this.isSpectating(game, player))) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onItemSpawn(ItemSpawnEvent event) {
        final Game<?> game = this.gameManager.getGame(event.getEntity().getWorld());

        if (game != null && game.getState() != GameState.PLAYING) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onInteract(PlayerInteractEvent event) {
        final Game<?> game = this.gameManager.getGame(event.getPlayer());

        if (game != null && this.isSpectating(game, event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        final Game<?> game = this.gameManager.getGame(event.getPlayer());

        if (game != null && this.isSpectating(game, event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onDrop(PlayerDropItemEvent event) {
        final Game<?> game = this.gameManager.getGame(event.getPlayer());

        if (game != null && (game.getState() != GameState.PLAYING || this.isSpectating(game, event.getPlayer()))) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPickup(PlayerPickupItemEvent event) {
        final Game<?> game = this.gameManager.getGame(event.getPlayer());

        if (game != null && (game.getState() != GameState.PLAYING || this.isSpectating(game, event.getPlayer()))) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        final Player player = (Player) event.getEntity();
        final Game<?> game = this.gameManager.getGame(player);

        if (game != null && (game.getState() != GameState.PLAYING || this.isSpectating(game, player))) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onDamageByEntity(EntityDamageByEntityEvent event) {
        Player attacker = null;
        final Entity damager = event.getDamager();

        if (damager instanceof Player) {
            attacker = (Player) damager;
        } else if (damager instanceof Projectile && ((Projectile) damager).getShooter() instanceof Player) {
            attacker = (Player) ((Projectile) damager).getShooter();
        }

        if (attacker == null) {
            return;
        }

        final Game<?> game = this.gameManager.getGame(attacker);

        if (game == null) {
            return;
        }

        if (this.isSpectating(game, attacker) || game.getState() != GameState.PLAYING) {
            event.setCancelled(true);
            return;
        }

        if (event.getEntity() instanceof Player && attacker != event.getEntity()) {
            final Player target = (Player) event.getEntity();
            final GamePlayer gameAttacker = game.getPlayer(attacker);

            if (!game.contains(target)) {
                event.setCancelled(true);
            } else if (gameAttacker != null && game.areInSameTeam(attacker, target) && !gameAttacker.getTeam().isFriendlyFire()) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onBreak(BlockBreakEvent event) {
        final Game<?> game = this.gameManager.getGame(event.getPlayer());

        if (game != null && (game.getState() != GameState.PLAYING || this.isSpectating(game, event.getPlayer()))) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlace(BlockPlaceEvent event) {
        final Game<?> game = this.gameManager.getGame(event.getPlayer());

        if (game != null && (game.getState() != GameState.PLAYING || this.isSpectating(game, event.getPlayer()))) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        final Game<?> game = this.gameManager.getGame(event.getWorld());

        if (game != null && !game.isDestroyed()) {
            event.setCancelled(true);
        }
    }

}
