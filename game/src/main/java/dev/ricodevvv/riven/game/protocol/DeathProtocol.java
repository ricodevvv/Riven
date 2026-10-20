package dev.ricodevvv.riven.game.protocol;

import dev.ricodevvv.riven.game.GameFramework;
import dev.ricodevvv.riven.game.Game;
import dev.ricodevvv.riven.game.GamePlayer;
import dev.ricodevvv.riven.game.GameState;
import dev.ricodevvv.riven.game.event.GameDeathEvent;
import dev.ricodevvv.riven.game.event.GameRespawnEvent;
import dev.ricodevvv.riven.game.util.GameMessages;
import dev.ricodevvv.riven.util.PlayerUtil;
import dev.ricodevvv.riven.util.Title;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Handles death: stops lethal damage, sends the death message and decides if the player respawns
 * (with their countdown screen) or becomes a spectator. Port of HyriDeathProtocol.
 */
public class DeathProtocol extends GameProtocol {

    private final Predicate<GamePlayer> continueDeath;
    private final Screen screen;
    private final Map<UUID, BukkitTask> deathTasks = new ConcurrentHashMap<>();

    private Options options = new Options();

    /**
     * @param game game
     * @param continueDeath true if the player respawns, false if they stay as a spectator
     * @param screen respawn screen, can be null
     */
    public DeathProtocol(Game<?> game, Predicate<GamePlayer> continueDeath, Screen screen) {
        super(game, "death");
        this.continueDeath = continueDeath;
        this.screen = screen;

        this.require(LastHitterProtocol.class);
    }

    @Override
    protected void enable() {}

    @Override
    protected void disable() {
        for (BukkitTask task : this.deathTasks.values()) {
            task.cancel();
        }

        this.deathTasks.clear();
    }

    public DeathProtocol withOptions(Options options) {
        this.options = options;
        return this;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        final Player player = (Player) event.getEntity();
        final GamePlayer gamePlayer = this.getGamePlayer(player);

        if (gamePlayer == null || gamePlayer.isSpectator() || gamePlayer.isDead()) {
            return;
        }

        if (player.getHealth() - event.getFinalDamage() <= 0.0D) {
            event.setDamage(0.0D);
            event.setCancelled(true);

            this.runDeath(this.getReason(event.getCause()), player);
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (this.options.minimumY == null || event.getTo().getY() > this.options.minimumY) {
            return;
        }

        final GamePlayer gamePlayer = this.getGamePlayer(event.getPlayer());

        if (gamePlayer != null && !gamePlayer.isSpectator() && !gamePlayer.isDead() && event.getPlayer().getWorld() == this.game.getWorld()) {
            this.runDeath(GameDeathEvent.Reason.VOID, event.getPlayer());
        }
    }

    private GameDeathEvent.Reason getReason(EntityDamageEvent.DamageCause cause) {
        switch (cause) {
            case FALL:
                return GameDeathEvent.Reason.FALL;
            case VOID:
                return GameDeathEvent.Reason.VOID;
            case BLOCK_EXPLOSION:
                return GameDeathEvent.Reason.BLOCK_EXPLOSION;
            case ENTITY_EXPLOSION:
                return GameDeathEvent.Reason.ENTITY_EXPLOSION;
            case LAVA:
                return GameDeathEvent.Reason.LAVA;
            case FIRE:
            case FIRE_TICK:
                return GameDeathEvent.Reason.FIRE;
            case LIGHTNING:
                return GameDeathEvent.Reason.LIGHTNING;
            default:
                return GameDeathEvent.Reason.PLAYERS;
        }
    }

    /**
     * Kills a player by hand. Normally it happens on its own.
     *
     * @param reason cause
     * @param player player
     */
    public void runDeath(GameDeathEvent.Reason reason, Player player) {
        final GamePlayer gamePlayer = this.getGamePlayer(player);

        if (gamePlayer == null || gamePlayer.isSpectator() || gamePlayer.isDead()) {
            return;
        }

        final LastHitterProtocol lastHitterProtocol = this.game.getProtocolManager().getProtocol(LastHitterProtocol.class);
        final List<LastHitterProtocol.LastHitter> lastHitters = lastHitterProtocol != null ? lastHitterProtocol.getLastHitters(player) : null;

        player.spigot().setCollidesWithEntities(false);
        player.setFallDistance(0.0F);

        gamePlayer.hide();

        if (lastHitters != null && this.options.deathSound) {
            final Player killer = lastHitters.get(0).asPlayer();

            if (killer != null) {
                killer.playSound(killer.getLocation(), Sound.ORB_PICKUP, 1.0F, 1.0F);
            }
        }

        final GameDeathEvent event = gamePlayer.setDead(reason, lastHitters);

        if (this.options.deathMessages) {
            this.game.broadcast(target -> {
                final StringBuilder builder = new StringBuilder(GameMessages.createDeathMessage(gamePlayer, target, reason, lastHitters));

                for (Function<Player, String> message : event.getMessagesToAdd()) {
                    builder.append(" ").append(message.apply(target));
                }
                return builder.toString();
            });
        }

        if (this.continueDeath.test(gamePlayer)) {
            PlayerUtil.resetPlayer(player, true);
            PlayerUtil.addSpectatorAbilities(player);

            if (this.game.getState() != GameState.ENDED && this.screen != null) {
                this.startScreen(gamePlayer);
            }
        } else if (this.game.getState() != GameState.ENDED) {
            PlayerUtil.resetPlayer(player, true);

            player.setAllowFlight(false);
            player.setFlying(false);
            player.spigot().setCollidesWithEntities(true);

            gamePlayer.show();
            gamePlayer.setSpectator(true);
        }

        if (lastHitterProtocol != null) {
            lastHitterProtocol.removeLastHitters(player);
        }
    }

    /**
     * Sends a player to the respawn screen without counting a death. Useful for people reconnecting.
     *
     * @param gamePlayer player
     */
    public void startRespawnScreen(GamePlayer gamePlayer) {
        if (this.screen == null || !gamePlayer.isOnline() || this.game.getState() != GameState.PLAYING) {
            return;
        }

        final Player player = gamePlayer.getPlayer();

        gamePlayer.markDead();

        PlayerUtil.resetPlayer(player, true);
        PlayerUtil.addSpectatorAbilities(player);

        player.spigot().setCollidesWithEntities(false);

        gamePlayer.hide();

        this.startScreen(gamePlayer);
    }

    private void startScreen(GamePlayer gamePlayer) {
        final UUID playerId = gamePlayer.getUniqueId();
        final BukkitTask old = this.deathTasks.remove(playerId);

        if (old != null) {
            old.cancel();
        }

        this.deathTasks.put(playerId, new BukkitRunnable() {

            private int time = screen.time;

            @Override
            public void run() {
                if (!gamePlayer.isOnline() || game.getState() != GameState.PLAYING) {
                    this.cancel();
                    deathTasks.remove(playerId);
                    return;
                }

                final Player player = gamePlayer.getPlayer();

                if (this.time <= 0) {
                    this.cancel();
                    deathTasks.remove(playerId);
                    respawn(gamePlayer);
                    return;
                }

                final String title = GameFramework.get().getLanguage().get(player, "game.death.title");
                final String subtitle = GameFramework.get().getLanguage().get(player, this.time == 1 ? "game.death.subtitle-second" : "game.death.subtitle")
                        .replace("%time%", String.valueOf(this.time));

                Title.sendTitle(player, title, subtitle, 0, 25, 0);

                this.time--;
            }
        }.runTaskTimer(this.game.getPlugin(), 0L, 20L));
    }

    private void respawn(GamePlayer gamePlayer) {
        if (this.game.getState() != GameState.PLAYING || !gamePlayer.isOnline()) {
            return;
        }

        final Player player = gamePlayer.getPlayer();

        gamePlayer.setNotDead();

        player.setAllowFlight(false);
        player.setFlying(false);
        player.spigot().setCollidesWithEntities(true);
        player.setFireTicks(0);

        PlayerUtil.resetPotionEffects(player);

        this.screen.callback.accept(player);

        gamePlayer.show();

        Bukkit.getPluginManager().callEvent(new GameRespawnEvent(this.game, gamePlayer));
    }

    /**
     * Respawn screen: how long it lasts and what to do when it's over.
     */
    public static class Screen {

        private final int time;
        private final Consumer<Player> callback;

        /**
         * @param time seconds to wait
         * @param callback what to do on respawn (teleport, give items...)
         */
        public Screen(int time, Consumer<Player> callback) {
            this.time = time;
            this.callback = callback;
        }

    }

    /**
     * Protocol options.
     */
    public static class Options {

        private Double minimumY;
        private boolean deathSound = true;
        private boolean deathMessages = true;

        /**
         * Height below which the player dies in the void.
         *
         * @param minimumY height
         * @return these options
         */
        public Options withMinimumY(double minimumY) {
            this.minimumY = minimumY;
            return this;
        }

        public Options withDeathSound(boolean deathSound) {
            this.deathSound = deathSound;
            return this;
        }

        public Options withDeathMessages(boolean deathMessages) {
            this.deathMessages = deathMessages;
            return this;
        }

    }

}
