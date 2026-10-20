package dev.ricodevvv.riven.command;

import dev.ricodevvv.riven.language.LanguageManager;
import dev.ricodevvv.riven.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;

/**
 * Everything a command needs while running: who sent it, the arguments left for it and quick parsers
 * that throw a {@link CommandException} with a friendly message when the input is wrong.
 */
public class CommandContext {

    private final CommandSender sender;
    private final String label;
    private final String[] args;
    private final LanguageManager language;

    public CommandContext(CommandSender sender, String label, String[] args, LanguageManager language) {
        this.sender = sender;
        this.label = label;
        this.args = args;
        this.language = language;
    }

    /**
     * @return a context for a subcommand, without the first argument
     */
    CommandContext shift() {
        return new CommandContext(this.sender, this.label + " " + this.args[0], Arrays.copyOfRange(this.args, 1, this.args.length), this.language);
    }

    public CommandSender getSender() {
        return this.sender;
    }

    /**
     * @return true if a player sent it
     */
    public boolean isPlayer() {
        return this.sender instanceof Player;
    }

    /**
     * @return the sender as a player
     * @throws CommandException if the console sent it
     */
    public Player getPlayer() {
        if (!(this.sender instanceof Player)) {
            throw new CommandException(this.message("riven.command.player-only", "&cOnly players can do that."));
        }
        return (Player) this.sender;
    }

    /**
     * @return the full label, including parent subcommands (like "sw coins add")
     */
    public String getLabel() {
        return this.label;
    }

    public String[] getArgs() {
        return this.args;
    }

    public int size() {
        return this.args.length;
    }

    public boolean has(int index) {
        return index < this.args.length;
    }

    /**
     * @param index argument index
     * @return the argument
     * @throws CommandException if it's missing
     */
    public String get(int index) {
        if (!this.has(index)) {
            throw new CommandException(this.message("riven.command.missing-argument", "&cMissing arguments."));
        }
        return this.args[index];
    }

    public String get(int index, String fallback) {
        return this.has(index) ? this.args[index] : fallback;
    }

    /**
     * @param index argument index
     * @return the argument as an int
     * @throws CommandException if it's missing or not a number
     */
    public int getInt(int index) {
        final String value = this.get(index);

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new CommandException(LanguageManager.replace(this.message("riven.command.invalid-number", "&c%value% isn't a valid number."), "%value%", value));
        }
    }

    public int getInt(int index, int fallback) {
        return this.has(index) ? this.getInt(index) : fallback;
    }

    public long getLong(int index) {
        final String value = this.get(index);

        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new CommandException(LanguageManager.replace(this.message("riven.command.invalid-number", "&c%value% isn't a valid number."), "%value%", value));
        }
    }

    public double getDouble(int index) {
        final String value = this.get(index);

        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            throw new CommandException(LanguageManager.replace(this.message("riven.command.invalid-number", "&c%value% isn't a valid number."), "%value%", value));
        }
    }

    /**
     * @param index argument index
     * @return the online player with that name
     * @throws CommandException if nobody online matches
     */
    public Player getOnlinePlayer(int index) {
        final String name = this.get(index);
        final Player player = Bukkit.getPlayerExact(name);

        if (player == null) {
            throw new CommandException(LanguageManager.replace(this.message("riven.command.player-not-found", "&c%player% isn't online."), "%player%", name));
        }
        return player;
    }

    /**
     * Online player at the index, or the sender if the argument isn't there.
     *
     * @param index argument index
     * @return the target player
     */
    public Player getOnlinePlayerOrSelf(int index) {
        return this.has(index) ? this.getOnlinePlayer(index) : this.getPlayer();
    }

    @SuppressWarnings("deprecation")
    public OfflinePlayer getOfflinePlayer(int index) {
        return Bukkit.getOfflinePlayer(this.get(index));
    }

    /**
     * @param index argument index
     * @param type enum class
     * @param <E> enum type
     * @return the constant, matched ignoring case
     * @throws CommandException if it doesn't match any
     */
    public <E extends Enum<E>> E getEnum(int index, Class<E> type) {
        final String value = this.get(index);

        for (E constant : type.getEnumConstants()) {
            if (constant.name().equalsIgnoreCase(value)) {
                return constant;
            }
        }
        throw new CommandException(LanguageManager.replace(this.message("riven.command.invalid-option", "&c%value% isn't a valid option."), "%value%", value));
    }

    /**
     * Joins every argument from the index on with spaces.
     *
     * @param from first index
     * @return the joined text
     */
    public String join(int from) {
        if (from >= this.args.length) {
            throw new CommandException(this.message("riven.command.missing-argument", "&cMissing arguments."));
        }
        return String.join(" ", Arrays.copyOfRange(this.args, from, this.args.length));
    }

    /**
     * Checks a permission and throws the usual message if the sender doesn't have it.
     *
     * @param permission permission node
     */
    public void requirePermission(String permission) {
        if (permission != null && !this.sender.hasPermission(permission)) {
            throw new CommandException(this.message("riven.command.no-permission", "&cYou don't have permission to do that."));
        }
    }

    public void reply(String message) {
        this.sender.sendMessage(message);
    }

    /**
     * Sends a language message with placeholder pairs.
     *
     * @param key message key
     * @param replacements placeholder and value pairs
     */
    public void replyKey(String key, Object... replacements) {
        this.sender.sendMessage(this.language.get(this.sender instanceof Player ? (Player) this.sender : null, key, replacements));
    }

    /**
     * @param key message key
     * @param replacements placeholder and value pairs
     * @return the message in the sender's language
     */
    public String lang(String key, Object... replacements) {
        return this.language.get(this.sender instanceof Player ? (Player) this.sender : null, key, replacements);
    }

    /**
     * @param key language key
     * @param fallback text used when no language has the key
     * @return the message in the sender's language
     */
    public String message(String key, String fallback) {
        return this.language == null ? TextUtil.colorize(fallback) : this.language.getOrDefault(this.sender instanceof Player ? (Player) this.sender : null, key, fallback);
    }

}
