package dev.ricodevvv.riven.game.scoreboard;

import dev.ricodevvv.riven.game.Game;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

/**
 * Waiting room scoreboard: map, mode, players and countdown. Port of HyriWaitingScoreboard.
 */
public class WaitingScoreboard extends GameScoreboard<Game<?>> {

    private int time = -1;

    public WaitingScoreboard(Game<?> game, Player player) {
        super(game, player, "waiting");

        this.addCurrentDateLine(0);
        this.addBlankLine(1);
        this.addBlankLine(5);
        this.addBlankLine(7);
        this.addAddressLine(8);

        this.addLines();
    }

    private void addLines() {
        this.setLine(2, this.lang("scoreboard.waiting.map").replace("%map%", this.game.getMapName()));
        this.setLine(3, this.lang("scoreboard.waiting.mode").replace("%mode%", this.game.getType().getDisplayName()));
        this.setLine(4, this.lang("scoreboard.waiting.players")
                .replace("%players%", String.valueOf(this.game.getPlayers().size()))
                .replace("%max_players%", String.valueOf(this.game.getMaxPlayers())));
        this.setLine(6, this.time < 0 ? ChatColor.RED + this.lang("scoreboard.waiting.waiting") : this.lang("scoreboard.waiting.starting").replace("%time%", String.valueOf(this.time)));
    }

    /**
     * Rebuilds the lines and sends them.
     */
    public void update() {
        this.addLines();
        this.updateLines();
    }

    public void setTime(int time) {
        this.time = time;
    }

}
