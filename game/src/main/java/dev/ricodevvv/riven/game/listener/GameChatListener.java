package dev.ricodevvv.riven.game.listener;

import dev.ricodevvv.riven.game.GameFramework;
import dev.ricodevvv.riven.game.Game;
import dev.ricodevvv.riven.game.GameManager;
import dev.ricodevvv.riven.game.GamePlayer;
import dev.ricodevvv.riven.game.GameState;
import dev.ricodevvv.riven.game.team.GameTeam;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.function.Function;

/**
 * Game chat. Port of Hyrame Impl's GameChatHandler: in teams of more than one you talk to your team
 * and "!" in front talks to everyone; spectators have their own chat. Each game only hears itself.
 */
public class GameChatListener implements Listener {

    private static final String FORMAT = "%s" + ChatColor.WHITE + ": %s";

    private final GameManager gameManager;

    public GameChatListener(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    private static String lang(Player player, String key) {
        return GameFramework.get().getLanguage().get(player, key);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        final Player player = event.getPlayer();
        final Game<?> game = this.gameManager.getGame(player);

        if (game == null) {
            return;
        }

        event.setCancelled(true);

        final String message = event.getMessage();
        final String name = GameFramework.get().getChatPrefix().apply(player) + player.getDisplayName();
        final GamePlayer gamePlayer = game.getPlayer(player);

        if (gamePlayer == null || gamePlayer.isSpectator()) {
            final String prefix = gamePlayer != null && gamePlayer.getTeam() != null ? this.teamTag(gamePlayer.getTeam(), null) : "";

            game.sendMessageToSpectators(target -> String.format(FORMAT, prefix + name, message));
            return;
        }

        final GameTeam team = gamePlayer.getTeam();

        if (game.getState() != GameState.PLAYING || team == null || team.getColor() == null) {
            game.broadcast(target -> String.format(FORMAT, (team != null && team.getColor() != null ? this.teamTag(team, target) : "") + name, message));
            return;
        }

        if (team.getTeamSize() == 1) {
            game.broadcast(target -> String.format(FORMAT, this.teamTag(team, target) + name, message));
        } else if (message.startsWith("!")) {
            final String global = message.substring(1).trim();

            if (!global.isEmpty()) {
                game.broadcast(target -> String.format(FORMAT, lang(target, "game.global-message") + this.teamTag(team, target) + name, global));
            }
        } else {
            final Function<Player, String> format = target -> String.format(FORMAT, lang(target, "game.team-message") + name, message);

            team.sendMessage(format);
        }
    }

    private String teamTag(GameTeam team, Player target) {
        final ChatColor color = team.getColor() != null ? team.getColor().getChatColor() : ChatColor.GRAY;

        return color + "[" + team.getDisplayName(target) + color + "] ";
    }

}
