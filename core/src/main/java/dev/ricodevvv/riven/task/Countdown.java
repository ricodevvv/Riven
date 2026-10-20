package dev.ricodevvv.riven.task;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.function.IntConsumer;

/**
 * Counts down from N to 0, one second per step. Handy for refill timers, final events and anything that ticks.
 */
public class Countdown extends BukkitRunnable {

    private final IntConsumer onTick;
    private final Runnable onFinish;
    private int remaining;

    /**
     * @param seconds starting seconds
     * @param onTick runs every second with the seconds left (before reaching zero)
     * @param onFinish runs once when it hits zero
     */
    public Countdown(int seconds, IntConsumer onTick, Runnable onFinish) {
        this.remaining = seconds;
        this.onTick = onTick;
        this.onFinish = onFinish;
    }

    /**
     * Starts the countdown.
     *
     * @param plugin owner plugin
     * @return this countdown
     */
    public Countdown start(JavaPlugin plugin) {
        this.runTaskTimer(plugin, 0L, 20L);
        return this;
    }

    @Override
    public void run() {
        if (this.remaining <= 0) {
            this.cancel();

            if (this.onFinish != null) {
                this.onFinish.run();
            }
            return;
        }

        if (this.onTick != null) {
            this.onTick.accept(this.remaining);
        }

        this.remaining--;
    }

    public int getRemaining() {
        return this.remaining;
    }

    public void setRemaining(int remaining) {
        this.remaining = remaining;
    }

}
