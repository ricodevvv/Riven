package dev.ricodevvv.riven.game.util;

import dev.ricodevvv.riven.game.GameFramework;
import dev.ricodevvv.riven.game.Game;
import dev.ricodevvv.riven.game.GamePlayer;
import dev.ricodevvv.riven.game.util.gui.SpectatorSettingsGUI;
import dev.ricodevvv.riven.game.util.gui.SpectatorTeleporterGUI;
import dev.ricodevvv.riven.game.util.gui.TeamChooserGUI;
import dev.ricodevvv.riven.item.GameItem;
import dev.ricodevvv.riven.item.ItemManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

/**
 * The items every game has: leave, pick a team and the spectator ones. Port of Hyrame Impl's items.
 */
public final class GameItems {

    public static final String TEAM_SELECTOR = "team_selector";
    public static final String LEAVE = "leave_game";
    public static final String SPECTATOR_SETTINGS = "spectator_settings";
    public static final String SPECTATOR_TELEPORTER = "spectator_teleporter";
    public static final String PLAY_AGAIN = "play_again";

    private GameItems() {}

    /**
     * Registers the default items. The framework does it on start.
     *
     * @param itemManager item registry
     */
    public static void registerDefaults(ItemManager itemManager) {
        itemManager.register(new GameItem(LEAVE, "item.global.leave", null, new ItemStack(Material.BED)) {
            @Override
            public void onRightClick(PlayerInteractEvent event) {
                GameFramework.get().getLeaveHandler().accept(event.getPlayer());
            }
        });

        itemManager.register(new GameItem(TEAM_SELECTOR, "item.global.team-selector", null, new ItemStack(Material.WOOL)) {
            @Override
            public void onRightClick(PlayerInteractEvent event) {
                final Game<?> game = GameFramework.get().getGameManager().getGame(event.getPlayer());

                if (game != null && game.getPlayer(event.getPlayer()) != null) {
                    new TeamChooserGUI(game, event.getPlayer()).open();
                }
            }

            @Override
            public ItemStack onPreGive(Player player, int slot, ItemStack itemStack) {
                final Game<?> game = GameFramework.get().getGameManager().getGame(player);
                final GamePlayer gamePlayer = game == null ? null : game.getPlayer(player);

                if (gamePlayer != null && gamePlayer.hasTeam() && gamePlayer.getTeam().getColor() != null) {
                    itemStack.setDurability(gamePlayer.getTeam().getColor().getWoolData());
                }
                return itemStack;
            }
        });

        itemManager.register(new GameItem(SPECTATOR_TELEPORTER, "item.spectator.teleporter", null, new ItemStack(Material.COMPASS)) {
            @Override
            public void onRightClick(PlayerInteractEvent event) {
                final Game<?> game = GameFramework.get().getGameManager().getGame(event.getPlayer());

                if (game != null) {
                    new SpectatorTeleporterGUI(game, event.getPlayer()).open();
                }
            }
        });

        itemManager.register(new GameItem(SPECTATOR_SETTINGS, "item.spectator.settings", null, new ItemStack(Material.REDSTONE_COMPARATOR)) {
            @Override
            public void onRightClick(PlayerInteractEvent event) {
                new SpectatorSettingsGUI(event.getPlayer()).open();
            }
        });

        itemManager.register(new GameItem(PLAY_AGAIN, "item.spectator.play-again", null, new ItemStack(Material.PAPER)) {
            @Override
            public void onRightClick(PlayerInteractEvent event) {
                GameFramework.get().getPlayAgainHandler().accept(event.getPlayer(), GameFramework.get().getGameManager().getGame(event.getPlayer()));
            }
        });
    }

    /**
     * Gives one of these items to a player.
     *
     * @param player player
     * @param id item id
     * @param slot slot
     */
    public static void give(Player player, String id, int slot) {
        GameFramework.get().getItemManager().give(player, slot, id);
    }

}
