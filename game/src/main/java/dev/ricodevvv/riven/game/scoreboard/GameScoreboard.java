package dev.ricodevvv.riven.game.scoreboard;

import dev.ricodevvv.riven.game.GameFramework;
import dev.ricodevvv.riven.game.Game;
import dev.ricodevvv.riven.scoreboard.Sidebar;
import dev.ricodevvv.riven.util.TimeUtil;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

/**
 * Base for the scoreboards of a game. Port of HyriGameScoreboard.
 *
 * @param <G> game type
 */
public class GameScoreboard<G extends Game<?>> extends Sidebar {

    protected final G game;

    public GameScoreboard(G game, Player player, String name) {
        super(player, name, GameFramework.get().getLanguage().get(player, "scoreboard.title").replace("%game%", game.getDisplayName()));
        this.game = game;
    }

    /**
     * Adds the animated address on the given line.
     *
     * @param line line number
     */
    protected void addAddressLine(int line) {
        final String address = GameFramework.get().getServerAddress();

        this.setLine(line, ChatColor.DARK_AQUA + address, new AddressLine(address), 2);
    }

    /**
     * Adds today's date, which refreshes by itself.
     *
     * @param line line number
     */
    protected void addCurrentDateLine(int line) {
        this.setLine(line, ChatColor.GRAY + TimeUtil.getCurrentFormattedDate(), sidebarLine -> sidebarLine.setValue(ChatColor.GRAY + TimeUtil.getCurrentFormattedDate()), 20 * 60);
    }

    /**
     * Translated text for the scoreboard owner.
     *
     * @param key key
     * @return the text
     */
    protected String lang(String key) {
        return GameFramework.get().getLanguage().get(this.player, key);
    }

    public G getGame() {
        return this.game;
    }

}
