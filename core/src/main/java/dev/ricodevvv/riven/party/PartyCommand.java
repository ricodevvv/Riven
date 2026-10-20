package dev.ricodevvv.riven.party;

import dev.ricodevvv.riven.command.CommandContext;
import dev.ricodevvv.riven.command.RivenCommand;
import dev.ricodevvv.riven.util.Symbols;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Ready to use {@code /party} command. {@code /party <player>} invites straight away, like on Hypixel.
 * Register it with {@code Riven.get().getCommandManager().register(new PartyCommand(parties, "myplugin.party.silentjoin"))}.
 */
public class PartyCommand extends RivenCommand {

    private static final List<String> HELP = Arrays.asList("invite", "accept", "deny", "join", "leave", "kick", "transfer", "open", "disband", "warp", "list", "chat");

    private final PartyManager parties;

    /**
     * @param parties party manager
     * @param silentJoinPermission permission for {@code /party silentjoin}
     * @param aliases command aliases
     */
    public PartyCommand(PartyManager parties, String silentJoinPermission, String... aliases) {
        super("party", aliases.length == 0 ? new String[] {"p"} : aliases);
        this.parties = parties;
        this.playerOnly = true;

        this.child(this.simple("invite", "<player>", 1, context -> parties.invite(context.getPlayer(), context.get(0))));
        this.child(this.simple("accept", "<leader>", 1, context -> parties.accept(context.getPlayer(), context.get(0))));
        this.child(this.simple("deny", "<leader>", 1, context -> parties.deny(context.getPlayer(), context.get(0))));
        this.child(this.simple("join", "<leader>", 1, context -> parties.join(context.getPlayer(), context.get(0))));
        this.child(this.simple("leave", "", 0, context -> parties.leave(context.getPlayer())));
        this.child(this.simple("kick", "<player>", 1, context -> parties.kick(context.getPlayer(), context.get(0))));
        this.child(this.simple("transfer", "<player>", 1, context -> parties.transfer(context.getPlayer(), context.get(0)), "promote"));
        this.child(this.simple("open", "", 0, context -> parties.toggleOpen(context.getPlayer())));
        this.child(this.simple("disband", "", 0, context -> parties.disband(context.getPlayer())));
        this.child(this.simple("warp", "", 0, context -> parties.warp(context.getPlayer())));
        this.child(this.simple("list", "", 0, context -> parties.list(context.getPlayer())));
        this.child(this.simple("chat", "[message]", 0, context -> {
            if (context.size() == 0) {
                parties.toggleChat(context.getPlayer());
            } else {
                parties.chat(context.getPlayer(), context.join(0));
            }
        }));

        final RivenCommand silentJoin = this.simple("silentjoin", "<player>", 1, context -> parties.silentJoin(context.getPlayer(), context.get(0)));

        this.child(silentJoin.setHidden(true).setPermission(silentJoinPermission));
    }

    @Override
    protected void execute(CommandContext context) {
        if (context.size() > 0) {
            this.parties.invite(context.getPlayer(), context.get(0));
            return;
        }

        final Player player = context.getPlayer();

        player.sendMessage(ChatColor.DARK_GRAY.toString() + ChatColor.STRIKETHROUGH + Symbols.HYPHENS_LINE);

        for (String sub : HELP) {
            player.sendMessage(ChatColor.DARK_GRAY + " " + Symbols.DOT_BOLD + " " + ChatColor.BLUE + "/" + context.getLabel() + " " + sub + ChatColor.GRAY + " - "
                    + context.message("party.help." + sub, sub));
        }

        player.sendMessage(ChatColor.DARK_GRAY.toString() + ChatColor.STRIKETHROUGH + Symbols.HYPHENS_LINE);
    }

    @Override
    protected List<String> complete(CommandContext context) {
        final List<String> result = super.complete(context);

        if (context.size() == 1) {
            for (Player online : Bukkit.getOnlinePlayers()) {
                result.add(online.getName());
            }
        }
        return result;
    }

    private RivenCommand simple(String name, String usage, int minArgs, Action action, String... aliases) {
        final PartyManager manager = this.parties;

        return new RivenCommand(name, aliases) {
            {
                this.usage = usage;
                this.minArgs = minArgs;
                this.playerOnly = true;
            }

            @Override
            protected void execute(CommandContext context) {
                action.run(context);
            }

            @Override
            protected List<String> complete(CommandContext context) {
                final List<String> result = new ArrayList<>();

                if (context.size() != 1 || usage.isEmpty()) {
                    return result;
                }

                final Party party = context.isPlayer() ? manager.getParty(context.getPlayer()) : null;

                if ((name.equals("kick") || name.equals("transfer")) && party != null) {
                    for (UUID member : party.getMembers()) {
                        if (!member.equals(context.getPlayer().getUniqueId()) && !party.isHidden(member)) {
                            result.add(party.getName(member));
                        }
                    }
                    return result;
                }

                for (Player online : Bukkit.getOnlinePlayers()) {
                    result.add(online.getName());
                }
                return result;
            }
        };
    }

    @FunctionalInterface
    private interface Action {

        void run(CommandContext context);

    }

}
