package dev.ricodevvv.riven.text;

import dev.ricodevvv.riven.util.TextUtil;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Chat messages with hover and click actions.
 *
 * <p>Build them in code:</p>
 * <pre>
 * new FancyMessage().text("&amp;eNotch invited you. ")
 *         .then("&amp;a[ACCEPT]").hover("&amp;7Click to join").command("/party accept Notch")
 *         .send(player);
 * </pre>
 *
 * <p>Or write them in language files with {@code {{text|action:value|action:value}}} blocks, where the action is
 * {@code run}, {@code suggest}, {@code url} or {@code hover}:</p>
 * <pre>
 * party-invite: "&amp;eNotch invited you. {{&amp;a[ACCEPT]|run:/party accept Notch|hover:&amp;7Click to join}}"
 * </pre>
 */
public class FancyMessage {

    private static final Pattern BLOCK = Pattern.compile("\\{\\{(.+?)\\}\\}");

    private final List<TextComponent> parts = new ArrayList<>();

    /**
     * Parses the {@code {{text|action:value}}} format. Plain text around the blocks stays as is.
     *
     * @param text message, with or without blocks
     * @return the message
     */
    public static FancyMessage parse(String text) {
        final FancyMessage message = new FancyMessage();

        if (text == null) {
            return message;
        }

        final Matcher matcher = BLOCK.matcher(text);
        int last = 0;
        String color = "";

        while (matcher.find()) {
            final String before = text.substring(last, matcher.start());

            if (!before.isEmpty()) {
                message.then(color + before);
                color = lastColors(TextUtil.colorize(color + before));
            }

            final String[] pieces = matcher.group(1).split("\\|");

            message.then(pieces[0]);

            for (int i = 1; i < pieces.length; i++) {
                final int split = pieces[i].indexOf(':');

                if (split == -1) {
                    continue;
                }

                final String action = pieces[i].substring(0, split).trim().toLowerCase();
                final String value = pieces[i].substring(split + 1);

                switch (action) {
                    case "run":
                        message.command(value);
                        break;
                    case "suggest":
                        message.suggest(value);
                        break;
                    case "url":
                        message.url(value);
                        break;
                    case "hover":
                        message.hover(value.replace("\\n", "\n"));
                        break;
                    default:
                        break;
                }
            }

            last = matcher.end();
        }

        if (last < text.length()) {
            message.then(color + text.substring(last));
        }
        return message;
    }

    /**
     * @param text text that may contain {@code {{...}}} blocks
     * @return true if it has at least one block
     */
    public static boolean isFancy(String text) {
        return text != null && BLOCK.matcher(text).find();
    }

    /**
     * Sends a text that may or may not be fancy: plain texts go as normal messages.
     *
     * @param sender receiver
     * @param text message
     */
    public static void sendAuto(CommandSender sender, String text) {
        if (sender instanceof Player && isFancy(text)) {
            parse(text).send((Player) sender);
        } else {
            sender.sendMessage(text == null ? "" : BLOCK.matcher(text).replaceAll("$1").replaceAll("\\|[a-z]+:[^|}]*", ""));
        }
    }

    public FancyMessage text(String text) {
        return this.then(text);
    }

    /**
     * Starts a new part of the message. Actions after this call apply to this part.
     *
     * @param text colored text ({@code &} codes work)
     * @return this message
     */
    public FancyMessage then(String text) {
        final TextComponent component = new TextComponent(TextComponent.fromLegacyText(TextUtil.colorize(text)));

        this.parts.add(component);
        return this;
    }

    public FancyMessage hover(String text) {
        this.last().setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(TextUtil.colorize(text))));
        return this;
    }

    public FancyMessage hover(List<String> lines) {
        return this.hover(String.join("\n", lines));
    }

    public FancyMessage command(String command) {
        this.last().setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command));
        return this;
    }

    public FancyMessage suggest(String text) {
        this.last().setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, text));
        return this;
    }

    public FancyMessage url(String url) {
        this.last().setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url));
        return this;
    }

    /**
     * @return the message as bungee components
     */
    public BaseComponent[] build() {
        return this.parts.toArray(new BaseComponent[0]);
    }

    public void send(Player player) {
        player.spigot().sendMessage(this.build());
    }

    public void send(Iterable<? extends Player> players) {
        final BaseComponent[] components = this.build();

        for (Player player : players) {
            player.spigot().sendMessage(components);
        }
    }

    /**
     * @return the message as plain legacy text, without actions
     */
    public String toLegacyText() {
        return TextComponent.toLegacyText(this.build());
    }

    private TextComponent last() {
        if (this.parts.isEmpty()) {
            this.then("");
        }
        return this.parts.get(this.parts.size() - 1);
    }

    private static String lastColors(String text) {
        return org.bukkit.ChatColor.getLastColors(text);
    }

}
