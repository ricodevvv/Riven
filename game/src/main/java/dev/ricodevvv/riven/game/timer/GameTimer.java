package dev.ricodevvv.riven.game.timer;

import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Counts the seconds of a game and tells whoever wants to listen.
 */
public class GameTimer extends BukkitRunnable {

    private final List<Consumer<Long>> listeners = new CopyOnWriteArrayList<>();
    private long currentTime;

    @Override
    public void run() {
        this.currentTime++;

        for (Consumer<Long> listener : this.listeners) {
            listener.accept(this.currentTime);
        }
    }

    /**
     * Adds something that runs every second with the current time.
     *
     * @param listener action
     */
    public void addListener(Consumer<Long> listener) {
        this.listeners.add(listener);
    }

    public long getCurrentTime() {
        return this.currentTime;
    }

}
