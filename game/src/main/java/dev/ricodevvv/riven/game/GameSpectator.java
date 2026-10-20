package dev.ricodevvv.riven.game;

import dev.ricodevvv.riven.game.event.GameSpectatorEvent;
import dev.ricodevvv.riven.util.PacketUtil;
import net.minecraft.server.v1_8_R3.PacketPlayOutPlayerInfo;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Someone watching a game. Players extend this too, since they start watching when they die.
 */
public class GameSpectator {

    protected final Game<?> game;
    protected final UUID uniqueId;
    protected final String name;
    protected Player player;

    private boolean spectator;

    public GameSpectator(Game<?> game, Player player) {
        this.game = game;
        this.player = player;
        this.uniqueId = player.getUniqueId();
        this.name = player.getName();
    }

    /**
     * Hides the player from the rest of the game but keeps them in the tab.
     */
    public void hide() {
        if (!this.isOnline()) {
            return;
        }

        for (Player target : this.game.getViewers()) {
            if (target.equals(this.player)) {
                continue;
            }

            target.hidePlayer(this.player);

            PacketUtil.sendPacket(target, new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER, ((CraftPlayer) this.player).getHandle()));
        }
    }

    /**
     * Shows the player again to the rest of the game.
     */
    public void show() {
        if (!this.isOnline()) {
            return;
        }

        for (Player target : this.game.getViewers()) {
            if (!target.equals(this.player)) {
                target.showPlayer(this.player);
            }
        }
    }

    /**
     * @return true if the player is still on the server
     */
    public boolean isOnline() {
        return this.player != null && this.player.isOnline();
    }

    public Game<?> getGame() {
        return this.game;
    }

    public UUID getUniqueId() {
        return this.uniqueId;
    }

    public String getName() {
        return this.name;
    }

    /**
     * @return the bukkit player. If they disconnected it may be a stale reference, check {@link #isOnline()}
     */
    public Player getPlayer() {
        return this.player;
    }

    public boolean isSpectator() {
        return this.spectator;
    }

    /**
     * Changes spectator mode and fires {@link GameSpectatorEvent} so the protocols apply it.
     *
     * @param spectator true to make them a spectator
     */
    public void setSpectator(boolean spectator) {
        this.spectator = spectator;

        Bukkit.getPluginManager().callEvent(new GameSpectatorEvent(this.game, this, spectator ? GameSpectatorEvent.Action.ADD : GameSpectatorEvent.Action.REMOVE));
    }

}
