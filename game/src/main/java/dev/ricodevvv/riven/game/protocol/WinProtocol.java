package dev.ricodevvv.riven.game.protocol;

import dev.ricodevvv.riven.game.GameFramework;
import dev.ricodevvv.riven.game.Game;
import dev.ricodevvv.riven.game.GamePlayer;
import dev.ricodevvv.riven.game.GameSpectator;
import dev.ricodevvv.riven.game.event.GameWinEvent;
import dev.ricodevvv.riven.game.team.GameTeam;
import dev.ricodevvv.riven.util.PlayerUtil;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * On win: stops deaths, turns everyone into a spectator and sends the victory and defeat titles. Port of HyriWinProtocol.
 */
public class WinProtocol extends GameProtocol {

    public WinProtocol(Game<?> game) {
        super(game, "win");
    }

    @Override
    protected void enable() {}

    @Override
    protected void disable() {}

    @EventHandler
    public void onWin(GameWinEvent event) {
        if (event.getGame() != this.game) {
            return;
        }

        this.game.getProtocolManager().disableProtocol(AntiSpawnKillProtocol.class);
        this.game.getProtocolManager().disableProtocol(DeathProtocol.class);

        final GameTeam winner = event.getWinner();
        final List<GameSpectator> everyone = new ArrayList<>(this.game.getPlayers());

        everyone.addAll(this.game.getSpectators());

        for (GameSpectator spectator : everyone) {
            if (!spectator.isOnline()) {
                continue;
            }

            PlayerUtil.resetPotionEffects(spectator.getPlayer());

            if (!spectator.isSpectator()) {
                spectator.setSpectator(true);
            }

            Bukkit.getScheduler().runTaskLater(this.game.getPlugin(), spectator::show, 1L);
        }

        for (GameTeam team : this.game.getTeams()) {
            if (team != winner) {
                team.sendTitle(target -> GameFramework.get().getLanguage().get(target, "game.defeat"), target -> "", 0, 60, 5);
            }
        }

        winner.sendTitle(target -> GameFramework.get().getLanguage().get(target, "game.victory"), target -> "", 0, 20 * 8, 5);

        for (GamePlayer gamePlayer : this.game.getOnlinePlayers()) {
            gamePlayer.getPlayer().setAllowFlight(true);
            gamePlayer.getPlayer().setFlying(true);
        }
    }

}
