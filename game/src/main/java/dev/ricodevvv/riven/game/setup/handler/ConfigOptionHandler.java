package dev.ricodevvv.riven.game.setup.handler;

import dev.ricodevvv.riven.game.setup.ConfigProcess;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;

/**
 * Asks the builder for a value (HyriBuild's ConfigOptionHandler). The process doesn't move on until
 * {@link #complete(Object)} gets called.
 *
 * @param <T> value type
 */
public abstract class ConfigOptionHandler<T> {

    protected final ConfigProcess<?> process;
    protected final Player player;
    private final CompletableFuture<T> completion = new CompletableFuture<>();

    public ConfigOptionHandler(ConfigProcess<?> process) {
        this.process = process;
        this.player = process.getPlayer();
    }

    /**
     * Called when the step starts: hand the builder their tool here.
     */
    public abstract void handle();

    /**
     * Ends the step with the chosen value.
     *
     * @param value value
     */
    public void complete(T value) {
        this.completion.complete(value);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public CompletableFuture<Object> getCompletion() {
        return (CompletableFuture) this.completion;
    }

}
