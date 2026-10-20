package dev.ricodevvv.riven.command;

import dev.ricodevvv.riven.language.LanguageManager;
import dev.ricodevvv.riven.util.Reflection;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.craftbukkit.v1_8_R3.CraftServer;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * Registers {@link RivenCommand}s straight into the server's command map, so they don't need to be in plugin.yml.
 */
public class CommandManager {

    private final JavaPlugin plugin;
    private final LanguageManager language;
    private final List<Command> registered = new ArrayList<>();

    public CommandManager(JavaPlugin plugin, LanguageManager language) {
        this.plugin = plugin;
        this.language = language;
    }

    /**
     * Registers a root command. If plugin.yml already declares one with the same name, the executor and
     * tab completer of that one are replaced instead.
     *
     * @param command root command
     */
    public void register(RivenCommand command) {
        final org.bukkit.command.PluginCommand declared = this.plugin.getCommand(command.getName());

        if (declared != null) {
            declared.setExecutor((sender, bukkitCommand, label, args) -> {
                this.execute(command, sender, label, args);
                return true;
            });
            declared.setTabCompleter((sender, bukkitCommand, label, args) -> command.tabComplete(new CommandContext(sender, label, args, this.language)));
            return;
        }

        final Command wrapper = new Command(command.getName(), command.getDescription(), "/" + command.getName() + " " + command.getUsage(), command.getAliases()) {
            @Override
            public boolean execute(CommandSender sender, String label, String[] args) {
                CommandManager.this.execute(command, sender, label, args);
                return true;
            }

            @Override
            public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
                return command.tabComplete(new CommandContext(sender, alias, args, CommandManager.this.language));
            }
        };

        this.getCommandMap().register(this.plugin.getName().toLowerCase(), wrapper);
        this.registered.add(wrapper);
    }

    private void execute(RivenCommand command, CommandSender sender, String label, String[] args) {
        try {
            command.dispatch(new CommandContext(sender, label, args, this.language));
        } catch (CommandException e) {
            sender.sendMessage(e.getMessage());
        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "Something went wrong running that command. Check the console.");
            this.plugin.getLogger().log(Level.SEVERE, "Error running /" + label, e);
        }
    }

    /**
     * Removes every command registered through this manager from the command map.
     */
    public void unregisterAll() {
        if (this.registered.isEmpty()) {
            return;
        }

        final SimpleCommandMap commandMap = this.getCommandMap();
        final Map<String, Command> known = Reflection.getField("knownCommands", commandMap);

        for (Command command : this.registered) {
            command.unregister(commandMap);
            known.values().removeIf(value -> value == command);
        }

        this.registered.clear();
    }

    private SimpleCommandMap getCommandMap() {
        return ((CraftServer) Bukkit.getServer()).getCommandMap();
    }

}
