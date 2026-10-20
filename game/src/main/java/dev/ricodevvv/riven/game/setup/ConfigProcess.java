package dev.ricodevvv.riven.game.setup;

import dev.ricodevvv.riven.game.setup.handler.ConfigOptionHandler;
import dev.ricodevvv.riven.util.Symbols;
import dev.ricodevvv.riven.util.Title;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

/**
 * HyriBuild's setup process: walks every {@link ConfigOption} field one by one, explains to the builder what to
 * put there and hands them the right tool (star, yes/no, sign or chat).
 *
 * @param <T> configuration type
 */
public class ConfigProcess<T> {

    private final SetupManager manager;
    private final Player player;
    private final T config;
    private final ConfigData data;
    private final Queue<ConfigStep> steps = new ConcurrentLinkedQueue<>();

    private ConfigStep currentStep;
    private Consumer<T> finishCallback;
    private boolean started;
    private int initialSize;

    public ConfigProcess(SetupManager manager, Player player, T config, ConfigData data) {
        this.manager = manager;
        this.player = player;
        this.config = config;
        this.data = data;
    }

    /**
     * Starts the process with the first value.
     */
    public void start() {
        if (this.started) {
            throw new IllegalStateException("The process already started");
        }

        this.started = true;
        this.initialSize = this.steps.size();

        this.player.getInventory().clear();

        this.sendMessage(this.text("setup.process.start")
                .replace("%amount%", String.valueOf(this.initialSize))
                .replace("%type%", this.text(this.data.name())));

        Title.sendTitle(this.player, this.text("setup.process.title"), ChatColor.GRAY + this.text(this.data.name()), 5, 60, 10);

        this.next();
    }

    private void next() {
        this.currentStep = this.steps.poll();

        if (this.currentStep == null) {
            this.sendMessage(this.text("setup.process.finished"));

            Title.sendTitle(this.player, ChatColor.GREEN + Symbols.TICK_BOLD + " " + this.text("setup.process.finished-title"), this.text("setup.process.finished-subtitle"), 5, 60, 10);

            this.manager.removeProcess(this);

            if (this.finishCallback != null) {
                this.finishCallback.accept(this.config);
            }
            return;
        }

        final ConfigStep step = this.currentStep;
        final ConfigOption option = step.getOption();
        final ConfigOptionHandler<?> handler = option.type().newHandler(this);

        step.setHandler(handler);

        this.sendStep(step);

        handler.getCompletion().thenAccept(value -> {
            if (this.currentStep != step || !this.manager.isRunning(this)) {
                return;
            }

            Title.sendTitle(this.player, ChatColor.GREEN + Symbols.TICK_BOLD + " " + this.text("setup.process.validated"), "", 2, 30, 2);

            if (value != null) {
                step.write(value);
            }

            this.next();
        });

        handler.handle();

        if (option.optional()) {
            this.manager.giveItem(this.player, SetupManager.SKIP_ITEM, 8);
        }
    }

    /**
     * Skips the current step if it's optional, leaving the field as it was.
     *
     * @return true if it got skipped
     */
    public boolean skip() {
        final ConfigStep step = this.currentStep;

        if (step == null || !step.getOption().optional() || step.getHandler() == null) {
            return false;
        }

        this.player.getInventory().clear();
        step.getHandler().getCompletion().complete(null);
        return true;
    }

    private void sendStep(ConfigStep step) {
        final ConfigOption option = step.getOption();
        final List<BaseComponent> components = new ArrayList<>();

        components.addAll(Arrays.asList(TextComponent.fromLegacyText(ChatColor.DARK_GRAY.toString() + ChatColor.STRIKETHROUGH + Symbols.HYPHENS_LINE + "\n")));

        if (step.getListMemberId() != null) {
            components.addAll(Arrays.asList(TextComponent.fromLegacyText(this.line("setup.process.element", step.getListMemberId()) + "\n")));
        }

        components.add(this.hoverLine("setup.process.value", this.text(option.name()), option.id()));

        if (step.getCategory() != null) {
            components.addAll(Arrays.asList(TextComponent.fromLegacyText("\n")));
            components.add(this.hoverLine("setup.process.category", this.text(step.getCategory().name()), step.getCategory().id()));
        }

        components.addAll(Arrays.asList(TextComponent.fromLegacyText("\n" + ChatColor.DARK_GRAY + " " + Symbols.DOT_BOLD + " " + ChatColor.GRAY + this.text("setup.process.description") + ": " + ChatColor.WHITE + this.text(option.description()))));
        components.addAll(Arrays.asList(TextComponent.fromLegacyText("\n" + ChatColor.DARK_GRAY + " " + Symbols.DOT_BOLD + " " + ChatColor.GRAY + (this.initialSize - this.steps.size()) + "/" + this.initialSize)));
        components.addAll(Arrays.asList(TextComponent.fromLegacyText("\n" + ChatColor.DARK_GRAY + ChatColor.STRIKETHROUGH + Symbols.HYPHENS_LINE)));

        this.player.spigot().sendMessage(components.toArray(new BaseComponent[0]));
    }

    private String line(String key, String value) {
        return ChatColor.DARK_GRAY + " " + Symbols.DOT_BOLD + " " + ChatColor.GRAY + this.text(key) + ": " + ChatColor.AQUA + value;
    }

    private TextComponent hoverLine(String key, String value, String id) {
        final TextComponent component = new TextComponent(TextComponent.fromLegacyText(this.line(key, value)));

        component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(ChatColor.GRAY + "Id: " + ChatColor.AQUA + id)));
        return component;
    }

    /**
     * Sends a framed message, like HyriBuild did.
     *
     * @param message text (line breaks allowed)
     */
    public void sendMessage(String message) {
        final String frame = ChatColor.DARK_GRAY.toString() + ChatColor.STRIKETHROUGH + Symbols.HYPHENS_LINE;

        this.player.sendMessage(frame);

        for (String line : message.split("\n")) {
            this.player.sendMessage(line);
        }

        this.player.sendMessage(frame);
    }

    /**
     * Translates a key for the builder. If it isn't a key, the text comes back as is.
     *
     * @param key key or text
     * @return the final text
     */
    public String text(String key) {
        return this.manager.text(this.player, key);
    }

    /**
     * @param finishCallback what to do with the finished configuration
     * @return this process
     */
    public ConfigProcess<T> withFinishCallback(Consumer<T> finishCallback) {
        this.finishCallback = finishCallback;
        return this;
    }

    /**
     * Adds a value to define. Only before starting.
     *
     * @param step value
     */
    public void addStep(ConfigStep step) {
        if (this.started) {
            throw new IllegalStateException("Steps can't be added once the process started");
        }

        this.steps.add(step);
    }

    public ConfigStep currentStep() {
        return this.currentStep;
    }

    public Player getPlayer() {
        return this.player;
    }

    public T getConfig() {
        return this.config;
    }

    public SetupManager getManager() {
        return this.manager;
    }

    public int getInitialSize() {
        return this.initialSize;
    }

    public int getRemaining() {
        return this.steps.size();
    }

}
