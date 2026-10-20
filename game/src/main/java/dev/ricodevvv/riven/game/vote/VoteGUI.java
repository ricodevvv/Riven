package dev.ricodevvv.riven.game.vote;

import dev.ricodevvv.riven.Riven;
import dev.ricodevvv.riven.inventory.GameInventory;
import dev.ricodevvv.riven.item.ItemBuilder;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Ready to use vote menu: first the categories, then the options of the one you click, with live counts.
 * Texts use the {@code vote.*} keys and fall back to English.
 */
public class VoteGUI extends GameInventory {

    private final VoteSession session;
    private final VoteCategory<?> category;

    /**
     * @param owner viewer
     * @param session votes of their game
     * @param category category to show, null for the category list
     */
    public VoteGUI(Player owner, VoteSession session, VoteCategory<?> category) {
        super(owner, text(owner, category == null ? "vote.menu.title" : "vote.menu.category-title", category == null ? "Votes" : "Vote: %category%")
                .replace("%category%", category == null ? "" : name(owner, category.getNameKey())), 27 + (category == null ? 0 : 9 * Math.max(0, (category.getOptions().size() - 1) / 7)));
        this.session = session;
        this.category = category;
        this.render();
    }

    private void render() {
        this.clear();

        if (this.category == null) {
            final List<VoteCategory<?>> categories = new ArrayList<>(this.session.getCategories());
            final int start = 13 - categories.size() / 2;

            for (int i = 0; i < categories.size(); i++) {
                final VoteCategory<?> current = categories.get(i);
                final VoteOption<?> winner = this.session.getWinner(current.getId());

                this.setItem(Math.max(9, Math.min(17, start + i)), new ItemBuilder(current.getIcon())
                        .withName(ChatColor.GREEN + name(this.owner, current.getNameKey()))
                        .withLore(text(this.owner, "vote.menu.category-lore", "&7Winning: &e%winner%\n\n&eClick to vote")
                                .replace("%winner%", winner == null ? "-" : name(this.owner, winner.getNameKey())))
                        .withAllItemFlags()
                        .build(), event -> new VoteGUI(this.owner, this.session, current).open());
            }
            return;
        }

        final List<? extends VoteOption<?>> options = this.category.getOptions();
        final String voted = this.session.getVote(this.owner.getUniqueId(), this.category.getId());

        for (int i = 0; i < options.size(); i++) {
            final VoteOption<?> option = options.get(i);
            final boolean allowed = this.session.canVote(this.owner, this.category, option);
            final int slot = 10 + (i / 7) * 9 + (i % 7);
            final ItemBuilder builder = new ItemBuilder(option.getIcon())
                    .withName((allowed ? ChatColor.GREEN : ChatColor.RED) + name(this.owner, option.getNameKey()))
                    .withLore(text(this.owner, allowed ? "vote.menu.option-lore" : "vote.menu.option-locked", allowed ? "&7Votes: &e%votes%\n\n&eClick to vote" : "&7Votes: &e%votes%\n\n&cYou can't vote this one")
                            .replace("%votes%", String.valueOf(this.session.getVotes(this.category.getId(), option.getId()))))
                    .withAllItemFlags();

            if (option.getId().equals(voted)) {
                builder.withGlow();
            }

            this.setItem(slot, builder.build(), event -> {
                final VoteSession.Result result = this.session.vote(this.owner, this.category.getId(), option.getId());

                if (result == VoteSession.Result.SUCCESS) {
                    this.owner.playSound(this.owner.getLocation(), Sound.NOTE_PLING, 1.0F, 1.5F);
                    this.owner.sendMessage(text(this.owner, "vote.voted", "&aYou voted &e%option%&a.").replace("%option%", name(this.owner, option.getNameKey())));
                } else if (result == VoteSession.Result.NO_PERMISSION) {
                    this.owner.playSound(this.owner.getLocation(), Sound.VILLAGER_NO, 1.0F, 1.0F);
                    this.owner.sendMessage(text(this.owner, "vote.no-permission", "&cYou can't vote that option."));
                } else if (result == VoteSession.Result.CLOSED) {
                    this.owner.sendMessage(text(this.owner, "vote.closed", "&cVoting is over."));
                    this.owner.closeInventory();
                    return;
                }

                this.render();
            });
        }

        this.setItem(this.size - 5, new ItemBuilder(Material.ARROW).withName(text(this.owner, "vote.menu.back", "&aBack")).build(),
                event -> new VoteGUI(this.owner, this.session, null).open());
    }

    private static String text(Player player, String key, String fallback) {
        return Riven.get().getLanguage().getOrDefault(player, key, fallback);
    }

    private static String name(Player player, String key) {
        return Riven.get().getLanguage().has(key) ? Riven.get().getLanguage().get(player, key) : key;
    }

}
