package dev.ricodevvv.riven.game.util.gui;

import dev.ricodevvv.riven.game.Game;
import dev.ricodevvv.riven.game.GameFramework;
import dev.ricodevvv.riven.game.GamePlayer;
import dev.ricodevvv.riven.game.protocol.SpectatorProtocol;
import dev.ricodevvv.riven.inventory.GameInventory;
import dev.ricodevvv.riven.item.ItemBuilder;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Spectator menu with the heads of the alive players to teleport to them.
 */
public class SpectatorTeleporterGUI extends GameInventory {

    public SpectatorTeleporterGUI(Game<?> game, Player owner) {
        super(owner, lang(owner, "gui.spectator.teleporter.title"), dynamicSize(alive(game).size()));

        for (GamePlayer gamePlayer : alive(game)) {
            final Player target = gamePlayer.getPlayer();

            this.addItem(ItemBuilder.asHead()
                    .withSkullOwner(target.getName())
                    .withName(gamePlayer.formatNameWithTeam())
                    .withLore(GameFramework.get().getLanguage().getList(owner, "gui.spectator.teleporter.lore"))
                    .build(), event -> {
                if (!target.isOnline()) {
                    this.owner.closeInventory();
                    return;
                }

                final SpectatorProtocol protocol = game.getProtocolManager().getProtocol(SpectatorProtocol.class);

                if (event.isShiftClick()) {
                    new SpectatorInventoryGUI(game, this.owner, target).open();
                } else if (event.isRightClick() && protocol != null) {
                    protocol.follow(this.owner, target);
                } else {
                    this.owner.closeInventory();
                    this.owner.teleport(target.getLocation());
                }
            });
        }
    }

    private static List<GamePlayer> alive(Game<?> game) {
        final List<GamePlayer> result = new ArrayList<>();

        for (GamePlayer gamePlayer : game.getOnlinePlayers()) {
            if (!gamePlayer.isSpectator() && !gamePlayer.isDead()) {
                result.add(gamePlayer);
            }
        }
        return result;
    }

}
