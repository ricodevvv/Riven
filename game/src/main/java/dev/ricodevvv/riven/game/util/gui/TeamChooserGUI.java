package dev.ricodevvv.riven.game.util.gui;

import dev.ricodevvv.riven.game.GameFramework;
import dev.ricodevvv.riven.game.Game;
import dev.ricodevvv.riven.game.GamePlayer;
import dev.ricodevvv.riven.game.team.GameTeam;
import dev.ricodevvv.riven.game.util.GameItems;
import dev.ricodevvv.riven.inventory.GameInventory;
import dev.ricodevvv.riven.item.ItemBuilder;
import dev.ricodevvv.riven.util.Symbols;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Menu to pick a team in the waiting room. Port of Hyrame's TeamChooserGUI.
 */
public class TeamChooserGUI extends GameInventory {

    private final Game<?> game;

    public TeamChooserGUI(Game<?> game, Player owner) {
        super(owner, lang(owner, "team-chooser.gui.title"), dynamicSize(game.getTeams().size() + 1));
        this.game = game;

        this.build();
    }

    private void build() {
        this.clear();

        for (GameTeam team : this.game.getTeams()) {
            final List<String> lore = new ArrayList<>();

            for (GamePlayer player : team.getPlayers()) {
                lore.add(ChatColor.WHITE + " " + Symbols.HYPHEN_BULLET + " " + ChatColor.GRAY + player.getName());
            }

            final ItemBuilder builder = new ItemBuilder(Material.WOOL, 1, team.getColor() != null ? team.getColor().getWoolData() : 0)
                    .withName(team.getFormattedDisplayName(this.owner) + ChatColor.GRAY + " [" + team.getPlayers().size() + "/" + team.getTeamSize() + "]")
                    .withLore(lore);

            this.addItem(builder.build(), event -> this.join(team));
        }

        this.setItem(this.size - 1, new ItemBuilder(Material.BARRIER).withName(lang(this.owner, "team-chooser.gui.random-team")).build(), event -> this.random());
    }

    private void join(GameTeam team) {
        final GamePlayer gamePlayer = this.game.getPlayer(this.owner);

        if (gamePlayer == null) {
            return;
        }

        if (gamePlayer.isInTeam(team)) {
            this.owner.sendMessage(lang(this.owner, "team-chooser.message.already-in"));
        } else if (team.isFull()) {
            this.owner.sendMessage(lang(this.owner, "team-chooser.message.full"));
        } else {
            gamePlayer.removeFromTeam();
            team.addPlayer(gamePlayer);

            this.owner.sendMessage(lang(this.owner, "team-chooser.message.join").replace("%team%", team.getFormattedDisplayName(this.owner)));

            refresh(this.game);
        }
    }

    private void random() {
        final GamePlayer gamePlayer = this.game.getPlayer(this.owner);

        if (gamePlayer == null) {
            return;
        }

        if (gamePlayer.hasTeam()) {
            gamePlayer.removeFromTeam();

            this.owner.sendMessage(lang(this.owner, "team-chooser.message.join-random"));

            refresh(this.game);
        } else {
            this.owner.sendMessage(lang(this.owner, "team-chooser.message.already-in-random"));
        }
    }

    /**
     * Refreshes every open selector of a game and the wool color of each player.
     *
     * @param game game
     */
    public static void refresh(Game<?> game) {
        for (TeamChooserGUI gui : GameFramework.get().getInventoryManager().getOpenInventories(TeamChooserGUI.class)) {
            if (gui.game == game) {
                gui.build();
            }
        }

        for (GamePlayer gamePlayer : game.getOnlinePlayers()) {
            final int slot = gamePlayer.getPlayer().getInventory().first(Material.WOOL);

            if (slot != -1) {
                GameItems.give(gamePlayer.getPlayer(), GameItems.TEAM_SELECTOR, slot);
            }
        }
    }

}
