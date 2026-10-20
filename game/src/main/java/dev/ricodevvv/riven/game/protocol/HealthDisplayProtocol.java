package dev.ricodevvv.riven.game.protocol;

import dev.ricodevvv.riven.game.Game;
import dev.ricodevvv.riven.game.GamePlayer;
import dev.ricodevvv.riven.game.event.GameJoinEvent;
import dev.ricodevvv.riven.game.event.GameLeaveEvent;
import dev.ricodevvv.riven.game.event.GameReconnectedEvent;
import dev.ricodevvv.riven.game.event.GameRespawnEvent;
import dev.ricodevvv.riven.game.event.GameSpectatorEvent;
import dev.ricodevvv.riven.util.PacketUtil;
import dev.ricodevvv.riven.util.Reflection;
import dev.ricodevvv.riven.util.Symbols;
import net.minecraft.server.v1_8_R3.IScoreboardCriteria;
import net.minecraft.server.v1_8_R3.Packet;
import net.minecraft.server.v1_8_R3.PacketPlayOutScoreboardDisplayObjective;
import net.minecraft.server.v1_8_R3.PacketPlayOutScoreboardObjective;
import net.minecraft.server.v1_8_R3.PacketPlayOutScoreboardScore;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;

/**
 * Shows health below the name using packets, only to the people in the game.
 * Hyrame used the main scoreboard, which would mix games in multi-arena.
 */
public class HealthDisplayProtocol extends GameProtocol {

    private static final String OBJECTIVE = "ubwhealth";
    private static final int BELOW_NAME_SLOT = 2;

    public HealthDisplayProtocol(Game<?> game) {
        super(game, "health_display");
    }

    @Override
    protected void enable() {
        for (Player viewer : this.game.getViewers()) {
            this.show(viewer);
        }
    }

    @Override
    protected void disable() {
        for (Player viewer : this.game.getViewers()) {
            this.hide(viewer);
        }
    }

    private void show(Player viewer) {
        final PacketPlayOutScoreboardObjective objective = new PacketPlayOutScoreboardObjective();

        Reflection.setField("a", objective, OBJECTIVE);
        Reflection.setField("b", objective, ChatColor.RED + Symbols.HEART);
        Reflection.setField("c", objective, IScoreboardCriteria.EnumScoreboardHealthDisplay.INTEGER);
        Reflection.setField("d", objective, 0);

        final PacketPlayOutScoreboardDisplayObjective display = new PacketPlayOutScoreboardDisplayObjective();

        Reflection.setField("a", display, BELOW_NAME_SLOT);
        Reflection.setField("b", display, OBJECTIVE);

        PacketUtil.sendPacket(viewer, objective);
        PacketUtil.sendPacket(viewer, display);

        for (GamePlayer gamePlayer : this.game.getOnlinePlayers()) {
            PacketUtil.sendPacket(viewer, this.scorePacket(gamePlayer.getPlayer()));
        }
    }

    private void hide(Player viewer) {
        final PacketPlayOutScoreboardObjective objective = new PacketPlayOutScoreboardObjective();

        Reflection.setField("a", objective, OBJECTIVE);
        Reflection.setField("b", objective, "");
        Reflection.setField("c", objective, IScoreboardCriteria.EnumScoreboardHealthDisplay.INTEGER);
        Reflection.setField("d", objective, 1);

        PacketUtil.sendPacket(viewer, objective);
    }

    private Packet<?> scorePacket(Player player) {
        final PacketPlayOutScoreboardScore packet = new PacketPlayOutScoreboardScore();

        Reflection.setField("a", packet, player.getName());
        Reflection.setField("b", packet, OBJECTIVE);
        Reflection.setField("c", packet, (int) Math.ceil(player.getHealth()));
        Reflection.setField("d", packet, PacketPlayOutScoreboardScore.EnumScoreboardAction.CHANGE);

        return packet;
    }

    /**
     * Sends a player's current health to the whole game.
     *
     * @param player player
     */
    public void update(Player player) {
        Bukkit.getScheduler().runTask(this.game.getPlugin(), () -> {
            if (player.isOnline()) {
                PacketUtil.sendPacket(this.game.getViewers(), this.scorePacket(player));
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player && this.getGamePlayer((Player) event.getEntity()) != null) {
            this.update((Player) event.getEntity());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRegain(EntityRegainHealthEvent event) {
        if (event.getEntity() instanceof Player && this.getGamePlayer((Player) event.getEntity()) != null) {
            this.update((Player) event.getEntity());
        }
    }

    @EventHandler
    public void onRespawn(GameRespawnEvent event) {
        if (event.getGame() == this.game && event.getGamePlayer().isOnline()) {
            this.update(event.getGamePlayer().getPlayer());
        }
    }

    @EventHandler
    public void onJoin(GameJoinEvent event) {
        if (event.getGame() == this.game) {
            this.show(event.getGamePlayer().getPlayer());
        }
    }

    @EventHandler
    public void onReconnected(GameReconnectedEvent event) {
        if (event.getGame() == this.game) {
            this.show(event.getGamePlayer().getPlayer());
        }
    }

    @EventHandler
    public void onSpectator(GameSpectatorEvent event) {
        if (event.getGame() != this.game || event.getSpectator() instanceof GamePlayer || !event.getSpectator().isOnline()) {
            return;
        }

        if (event.getAction() == GameSpectatorEvent.Action.ADD) {
            this.show(event.getSpectator().getPlayer());
        } else {
            this.hide(event.getSpectator().getPlayer());
        }
    }

    @EventHandler
    public void onLeave(GameLeaveEvent event) {
        if (event.getGame() == this.game && event.getGamePlayer().getPlayer() != null) {
            this.hide(event.getGamePlayer().getPlayer());
        }
    }

}
