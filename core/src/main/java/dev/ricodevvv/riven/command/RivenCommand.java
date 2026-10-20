package dev.ricodevvv.riven.command;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A command or a subcommand. Build trees by adding children: {@code /sw coins add 10 Notch} is a
 * "sw" command with a "coins" child that has an "add" child. Each level checks its own permission.
 *
 * <pre>
 * public class CoinsAdd extends RivenCommand {
 *     public CoinsAdd() {
 *         super("add");
 *         this.permission = "skywars.coins";
 *         this.usage = "&lt;amount&gt; [player]";
 *     }
 *
 *     protected void execute(CommandContext context) {
 *         Player target = context.getOnlinePlayerOrSelf(1);
 *         economy.add(target, context.getInt(0));
 *     }
 * }
 * </pre>
 */
public abstract class RivenCommand {

    protected final String name;
    protected final List<String> aliases = new ArrayList<>();
    protected final Map<String, RivenCommand> children = new LinkedHashMap<>();

    protected String permission;
    protected String description = "";
    protected String usage = "";
    protected boolean playerOnly;
    protected boolean hidden;
    protected int minArgs;

    protected RivenCommand(String name, String... aliases) {
        this.name = name.toLowerCase();
        this.aliases.addAll(Arrays.asList(aliases));
    }

    /**
     * Runs the command once permission, sender type and argument count are checked. By default it prints
     * the list of subcommands.
     *
     * @param context command context
     */
    protected void execute(CommandContext context) {
        this.sendHelp(context);
    }

    /**
     * Tab completion for this level. By default it suggests subcommands and online player names.
     *
     * @param context command context, the last argument is the one being typed
     * @return suggestions
     */
    protected List<String> complete(CommandContext context) {
        final List<String> result = new ArrayList<>();

        if (context.size() == 1 && !this.children.isEmpty()) {
            for (RivenCommand child : this.children.values()) {
                if (!child.hidden && child.canUse(context.getSender())) {
                    result.add(child.name);
                }
            }
            return result;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            result.add(player.getName());
        }
        return result;
    }

    /**
     * Adds a subcommand.
     *
     * @param child subcommand
     * @return this command, to keep chaining
     */
    public RivenCommand child(RivenCommand child) {
        this.children.put(child.name, child);

        for (String alias : child.aliases) {
            this.children.putIfAbsent(alias.toLowerCase(), child);
        }
        return this;
    }

    final void dispatch(CommandContext context) {
        if (context.size() > 0) {
            final RivenCommand child = this.children.get(context.getArgs()[0].toLowerCase());

            if (child != null) {
                child.dispatch(context.shift());
                return;
            }
        }

        context.requirePermission(this.permission);

        if (this.playerOnly) {
            context.getPlayer();
        }

        if (context.size() < this.minArgs) {
            throw new CommandException(context.message("riven.command.usage", "&cUsage: /%command% %usage%")
                    .replace("%command%", context.getLabel())
                    .replace("%usage%", this.usage));
        }

        this.execute(context);
    }

    final List<String> tabComplete(CommandContext context) {
        if (context.size() > 1) {
            final RivenCommand child = this.children.get(context.getArgs()[0].toLowerCase());

            if (child != null) {
                return child.canUse(context.getSender()) ? child.tabComplete(context.shift()) : Collections.emptyList();
            }
        }

        if (!this.canUse(context.getSender())) {
            return Collections.emptyList();
        }

        final String typing = context.size() == 0 ? "" : context.getArgs()[context.size() - 1].toLowerCase();
        final List<String> result = new ArrayList<>();

        for (String suggestion : this.complete(context)) {
            if (suggestion.toLowerCase().startsWith(typing)) {
                result.add(suggestion);
            }
        }
        return result;
    }

    /**
     * Prints every visible subcommand with its usage and description.
     *
     * @param context command context
     */
    protected void sendHelp(CommandContext context) {
        final List<RivenCommand> visible = new ArrayList<>();

        for (Map.Entry<String, RivenCommand> entry : this.children.entrySet()) {
            final RivenCommand child = entry.getValue();

            if (entry.getKey().equals(child.name) && !child.hidden && child.canUse(context.getSender())) {
                visible.add(child);
            }
        }

        if (visible.isEmpty()) {
            context.reply(ChatColor.RED + "/" + context.getLabel() + " " + this.usage);
            return;
        }

        context.reply(context.message("riven.command.help-header", "&8&m-----&r &b/%command% &8&m-----").replace("%command%", context.getLabel()));

        for (RivenCommand child : visible) {
            context.reply(ChatColor.AQUA + "/" + context.getLabel() + " " + child.name
                    + (child.usage.isEmpty() ? "" : " " + ChatColor.GRAY + child.usage)
                    + (child.description.isEmpty() ? "" : ChatColor.DARK_GRAY + " - " + ChatColor.WHITE + child.description));
        }
    }

    /**
     * @param sender sender
     * @return true if the sender has this command's permission (or it doesn't have one)
     */
    public boolean canUse(CommandSender sender) {
        return this.permission == null || sender.hasPermission(this.permission);
    }

    public String getName() {
        return this.name;
    }

    public RivenCommand setPermission(String permission) {
        this.permission = permission;
        return this;
    }

    public RivenCommand setDescription(String description) {
        this.description = description;
        return this;
    }

    public RivenCommand setUsage(String usage) {
        this.usage = usage;
        return this;
    }

    public RivenCommand setHidden(boolean hidden) {
        this.hidden = hidden;
        return this;
    }

    public RivenCommand setPlayerOnly(boolean playerOnly) {
        this.playerOnly = playerOnly;
        return this;
    }

    public RivenCommand setMinArgs(int minArgs) {
        this.minArgs = minArgs;
        return this;
    }

    public List<String> getAliases() {
        return this.aliases;
    }

    public String getPermission() {
        return this.permission;
    }

    public String getDescription() {
        return this.description;
    }

    public String getUsage() {
        return this.usage;
    }

    public Map<String, RivenCommand> getChildren() {
        return Collections.unmodifiableMap(this.children);
    }

}
