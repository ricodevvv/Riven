package dev.ricodevvv.riven.game.util;

import dev.ricodevvv.riven.game.GameFramework;
import dev.ricodevvv.riven.game.Game;
import dev.ricodevvv.riven.game.GamePlayer;
import dev.ricodevvv.riven.game.event.GameDeathEvent;
import dev.ricodevvv.riven.game.protocol.LastHitterProtocol;
import dev.ricodevvv.riven.game.team.GameTeam;
import dev.ricodevvv.riven.util.Symbols;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Common game messages: deaths, game description and final summary. Port of HyriGameMessages.
 */
public final class GameMessages {

    private static final String PREFIX = "message.death.";
    private static final int LINE_WIDTH = Symbols.HYPHENS_LINE.length();

    private GameMessages() {}

    private static String lang(Player player, String key) {
        return GameFramework.get().getLanguage().get(player, key);
    }

    /**
     * Builds the death message from the cause and the last hitter.
     *
     * @param dead the one who died
     * @param target who reads the message
     * @param reason cause
     * @param lastHitters last hitters, can be null
     * @return the finished message
     */
    public static String createDeathMessage(GamePlayer dead, Player target, GameDeathEvent.Reason reason, List<LastHitterProtocol.LastHitter> lastHitters) {
        String result;

        if (lastHitters != null && !lastHitters.isEmpty() && lastHitters.get(0).asGamePlayer() != null) {
            final String prefix = GameFramework.get().getDeathMessagePrefix().apply(lastHitters.get(0).asGamePlayer());

            if (reason == GameDeathEvent.Reason.PLAYERS) {
                if (lastHitters.size() > 1 && lastHitters.get(1).asGamePlayer() != null) {
                    result = lang(target, key(prefix, "players")).replace("%second_killer%", lastHitters.get(1).asGamePlayer().formatNameWithTeam());
                } else {
                    result = lang(target, key(prefix, "player"));
                }
            } else {
                result = lang(target, key(prefix, "player-and-" + reason.getKey()));
            }

            result = result.replace("%killer%", lastHitters.get(0).asGamePlayer().formatNameWithTeam());
        } else {
            result = lang(target, PREFIX + (reason == GameDeathEvent.Reason.PLAYERS ? "unknown" : reason.getKey()));
        }

        return result.replace("%player%", dead.formatNameWithTeam());
    }

    private static String key(String prefix, String name) {
        return GameFramework.get().getLanguage().has(prefix + name) ? prefix + name : PREFIX + name;
    }

    private static BaseComponent[] framed(Game<?> game, List<String> body) {
        final String line = ChatColor.DARK_AQUA.toString() + ChatColor.STRIKETHROUGH + Symbols.HYPHENS_LINE;
        final StringBuilder builder = new StringBuilder(line).append("\n");

        builder.append(center(game.getDisplayName())).append(ChatColor.AQUA).append(ChatColor.BOLD).append(game.getDisplayName()).append(ChatColor.RESET).append("\n\n");

        for (String bodyLine : body) {
            builder.append(bodyLine).append(ChatColor.RESET).append("\n");
        }

        return TextComponent.fromLegacyText(builder.append(line).toString());
    }

    private static String center(String text) {
        final int length = ChatColor.stripColor(text).length();
        final StringBuilder spaces = new StringBuilder();

        for (int i = 0; i < Math.max(0, (LINE_WIDTH - length) / 2); i++) {
            spaces.append(" ");
        }
        return spaces.toString();
    }

    /**
     * Framed message with the game description.
     *
     * @param game game
     * @param target who reads it
     * @return the message
     */
    public static BaseComponent[] createDescription(Game<?> game, Player target) {
        return framed(game, Collections.singletonList(lang(target, game.getDescriptionKey())));
    }

    /**
     * End of game summary: winner, stats and rewards.
     *
     * @param game game
     * @param target who reads it
     * @param winner winning team
     * @param stats stats lines, can be null
     * @param rewards rewards line, can be null
     * @return the message
     */
    public static BaseComponent[] createWinMessage(Game<?> game, Player target, GameTeam winner, List<String> stats, String rewards) {
        final List<String> body = new ArrayList<>();
        final String winnerLine = lang(target, "game.end.winner").replace("%winner%", winner.getFormattedDisplayName(target));

        body.add(center(winnerLine) + winnerLine);

        if (stats != null && !stats.isEmpty()) {
            body.add("");

            for (String stat : stats) {
                body.add(center(stat) + stat);
            }
        }

        if (rewards != null) {
            body.add("");
            body.add(center(rewards) + rewards);
        }

        return framed(game, body);
    }

}
