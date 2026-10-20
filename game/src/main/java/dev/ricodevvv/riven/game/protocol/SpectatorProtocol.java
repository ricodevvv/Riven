package dev.ricodevvv.riven.game.protocol;

import dev.ricodevvv.riven.game.Game;
import dev.ricodevvv.riven.game.GamePlayer;
import dev.ricodevvv.riven.game.GameSpectator;
import dev.ricodevvv.riven.game.event.GameReconnectedEvent;
import dev.ricodevvv.riven.game.event.GameSpectatorEvent;
import dev.ricodevvv.riven.game.util.GameItems;
import dev.ricodevvv.riven.game.GameFramework;
import dev.ricodevvv.riven.game.util.gui.SpectatorInventoryGUI;
import dev.ricodevvv.riven.util.ActionBar;
import dev.ricodevvv.riven.util.PlayerUtil;
import dev.ricodevvv.riven.util.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Applies spectator mode: flight, invisibility, teleport items, settings and leave. Port of HyriSpectatorProtocol,
 * with two new things: following a player in first person (shift + right click on them, or right click in the
 * player menu) and seeing their inventory (right click on them). Press shift to stop following.
 */
public class SpectatorProtocol extends GameProtocol {

    private final Map<UUID, UUID> following = new ConcurrentHashMap<>();
    private BukkitTask task;

    public SpectatorProtocol(Game<?> game) {
        super(game, "spectator");
    }

    @Override
    protected void enable() {
        this.task = Bukkit.getScheduler().runTaskTimer(this.game.getPlugin(), this::tick, 10L, 10L);
    }

    @Override
    protected void disable() {
        if (this.task != null) {
            this.task.cancel();
        }

        for (UUID viewer : new ArrayList<>(this.following.keySet())) {
            final Player player = Bukkit.getPlayer(viewer);

            if (player != null) {
                this.stopFollowing(player);
            }
        }
    }

    /**
     * @param player player
     * @return true if they're watching the game (outside spectator, eliminated or waiting to respawn)
     */
    public boolean isSpectating(Player player) {
        final GamePlayer gamePlayer = this.game.getPlayer(player);

        return this.game.getSpectator(player.getUniqueId()) != null || (gamePlayer != null && gamePlayer.isOnline() && gamePlayer.isSpectator());
    }

    private boolean isPlaying(Player player) {
        final GamePlayer gamePlayer = this.game.getPlayer(player);

        return gamePlayer != null && gamePlayer.isOnline() && !gamePlayer.isSpectator() && !gamePlayer.isDead();
    }

    /**
     * Makes a spectator watch the game through a player's eyes.
     *
     * @param viewer spectator
     * @param target player to follow
     */
    public void follow(Player viewer, Player target) {
        if (!this.isSpectating(viewer) || !this.isPlaying(target) || viewer.equals(target)) {
            return;
        }

        this.following.put(viewer.getUniqueId(), target.getUniqueId());

        viewer.closeInventory();
        viewer.teleport(target.getLocation());
        viewer.setGameMode(GameMode.SPECTATOR);

        Bukkit.getScheduler().runTaskLater(this.game.getPlugin(), () -> {
            if (viewer.isOnline() && target.isOnline() && this.following.containsKey(viewer.getUniqueId())) {
                viewer.setSpectatorTarget(target);
            }
        }, 2L);

        Title.sendTitle(viewer, GameFramework.get().getLanguage().get(viewer, "spectator.follow.title").replace("%player%", target.getName()),
                GameFramework.get().getLanguage().get(viewer, "spectator.follow.subtitle"), 5, 40, 10);
    }

    /**
     * Stops following and goes back to the usual spectator mode.
     *
     * @param viewer spectator
     */
    public void stopFollowing(Player viewer) {
        if (this.following.remove(viewer.getUniqueId()) == null) {
            return;
        }

        if (viewer.getGameMode() == GameMode.SPECTATOR) {
            viewer.setSpectatorTarget(null);
        }

        PlayerUtil.addSpectatorAbilities(viewer);
        ActionBar.send(viewer, "");
    }

    private void tick() {
        for (Map.Entry<UUID, UUID> entry : new ArrayList<>(this.following.entrySet())) {
            final Player viewer = Bukkit.getPlayer(entry.getKey());
            final Player target = Bukkit.getPlayer(entry.getValue());

            if (viewer == null) {
                this.following.remove(entry.getKey());
                continue;
            }

            if (target == null || !this.isPlaying(target) || target.getWorld() != viewer.getWorld() || !this.isSpectating(viewer)) {
                this.stopFollowing(viewer);
                continue;
            }

            if (viewer.getSpectatorTarget() == null && viewer.getGameMode() == GameMode.SPECTATOR && viewer.getLocation().distanceSquared(target.getLocation()) > 4.0D) {
                viewer.setSpectatorTarget(target);
            }

            ActionBar.send(viewer, GameFramework.get().getLanguage().get(viewer, "spectator.follow.actionbar")
                    .replace("%player%", target.getName())
                    .replace("%health%", String.valueOf((int) Math.ceil(target.getHealth())))
                    .replace("%max_health%", String.valueOf((int) target.getMaxHealth())));
        }
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        if (event.isSneaking() && this.following.containsKey(event.getPlayer().getUniqueId())) {
            Bukkit.getScheduler().runTask(this.game.getPlugin(), () -> this.stopFollowing(event.getPlayer()));
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        final Player viewer = event.getPlayer();

        if (!(event.getRightClicked() instanceof Player) || this.game.getPlayer(viewer) == null && this.game.getSpectator(viewer.getUniqueId()) == null) {
            return;
        }

        final Player target = (Player) event.getRightClicked();

        if (!this.isSpectating(viewer) || !this.isPlaying(target)) {
            return;
        }

        event.setCancelled(true);

        if (viewer.isSneaking()) {
            this.follow(viewer, target);
        } else {
            new SpectatorInventoryGUI(this.game, viewer, target).open();
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        this.following.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onSpectator(GameSpectatorEvent event) {
        if (event.getGame() != this.game) {
            return;
        }

        final GameSpectator spectator = event.getSpectator();

        if (!spectator.isOnline()) {
            return;
        }

        final Player player = spectator.getPlayer();

        if (event.getAction() == GameSpectatorEvent.Action.ADD) {
            PlayerUtil.resetPlayer(player, true);
            PlayerUtil.addSpectatorAbilities(player);

            spectator.hide();
            player.spigot().setCollidesWithEntities(false);

            GameItems.give(player, GameItems.SPECTATOR_TELEPORTER, 0);
            GameItems.give(player, GameItems.SPECTATOR_SETTINGS, 4);
            GameItems.give(player, GameItems.PLAY_AGAIN, 7);
            GameItems.give(player, GameItems.LEAVE, 8);
        } else {
            PlayerUtil.resetPlayer(player, true);

            player.setAllowFlight(false);
            player.setFlying(false);
            player.spigot().setCollidesWithEntities(true);

            spectator.show();
        }
    }

    @EventHandler
    public void onReconnected(GameReconnectedEvent event) {
        if (event.getGame() != this.game) {
            return;
        }

        for (GamePlayer gamePlayer : this.game.getPlayers()) {
            if (gamePlayer.isSpectator() && gamePlayer.isOnline()) {
                gamePlayer.hide();
            }
        }

        for (GameSpectator spectator : this.game.getSpectators()) {
            spectator.hide();
        }
    }

}
