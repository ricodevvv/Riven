package dev.ricodevvv.riven.game.protocol;

import dev.ricodevvv.riven.game.GameFramework;
import dev.ricodevvv.riven.game.Game;
import dev.ricodevvv.riven.game.GamePlayer;
import dev.ricodevvv.riven.game.event.GameJoinEvent;
import dev.ricodevvv.riven.game.event.GameLeaveEvent;
import dev.ricodevvv.riven.game.scoreboard.WaitingScoreboard;
import dev.ricodevvv.riven.game.util.GameItems;
import dev.ricodevvv.riven.util.PlayerUtil;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;

/**
 * The waiting room: cleans the player up, gives the team selector and the leave item,
 * sets the waiting scoreboard and announces who joins and leaves. Port of HyriWaitingProtocol.
 */
public class WaitingProtocol extends GameProtocol {

    private final boolean teamSelector;

    public WaitingProtocol(Game<?> game, boolean teamSelector) {
        super(game, "waiting");
        this.teamSelector = teamSelector;
    }

    @Override
    protected void enable() {
        if (this.game.getStartingTimer() != null) {
            this.game.getStartingTimer().addListener(time -> {
                for (WaitingScoreboard scoreboard : GameFramework.get().getScoreboardManager().getSidebars(WaitingScoreboard.class)) {
                    if (scoreboard.getGame() == this.game) {
                        scoreboard.setTime(time);
                        scoreboard.update();
                    }
                }
            });
        }
    }

    @Override
    protected void disable() {
        if (this.game.getStartingTimer() != null) {
            this.game.getStartingTimer().clearListeners();
        }

        for (GamePlayer gamePlayer : this.game.getOnlinePlayers()) {
            gamePlayer.getPlayer().setCanPickupItems(true);
        }

        for (WaitingScoreboard scoreboard : GameFramework.get().getScoreboardManager().getSidebars(WaitingScoreboard.class)) {
            if (scoreboard.getGame() == this.game) {
                scoreboard.hide();
            }
        }
    }

    @EventHandler
    public void onJoin(GameJoinEvent event) {
        if (event.getGame() != this.game) {
            return;
        }

        final Player player = event.getGamePlayer().getPlayer();

        PlayerUtil.resetPlayer(player, true);

        player.setCanPickupItems(false);
        player.setGameMode(GameMode.ADVENTURE);

        if (this.teamSelector && this.game.isUsingTeams() && !this.game.getTeams().isEmpty() && this.game.getTeams().get(0).getTeamSize() > 1) {
            GameItems.give(player, GameItems.TEAM_SELECTOR, 0);
        }

        GameItems.give(player, GameItems.LEAVE, 8);

        new WaitingScoreboard(this.game, player).show();

        this.updateScoreboards();

        if (event.isSilent()) {
            return;
        }

        this.game.broadcast(target -> GameFramework.get().getLanguage().get(target, "game.join")
                .replace("%player%", player.getDisplayName())
                .replace("%counter_color%", (this.game.canStart() ? ChatColor.GREEN : ChatColor.RED).toString())
                .replace("%current_players%", String.valueOf(this.game.getPlayers().size()))
                .replace("%max_players%", String.valueOf(this.game.getMaxPlayers())));
    }

    @EventHandler
    public void onLeave(GameLeaveEvent event) {
        if (event.getGame() != this.game) {
            return;
        }

        this.updateScoreboards();

        this.game.broadcast(target -> GameFramework.get().getLanguage().get(target, "game.left")
                .replace("%player%", event.getGamePlayer().getName())
                .replace("%counter_color%", (this.game.canStart() ? ChatColor.GREEN : ChatColor.RED).toString())
                .replace("%current_players%", String.valueOf(this.game.getPlayers().size()))
                .replace("%max_players%", String.valueOf(this.game.getMaxPlayers())));
    }

    private void updateScoreboards() {
        for (WaitingScoreboard scoreboard : GameFramework.get().getScoreboardManager().getSidebars(WaitingScoreboard.class)) {
            if (scoreboard.getGame() == this.game) {
                scoreboard.update();
            }
        }
    }

}
