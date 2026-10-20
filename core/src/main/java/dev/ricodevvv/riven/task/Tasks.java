package dev.ricodevvv.riven.task;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;

/**
 * Scheduler shortcuts that remember every repeating task, so a single {@link #cancelAll()} cleans up on disable.
 */
public class Tasks {

    private final JavaPlugin plugin;
    private final Set<BukkitTask> repeating = ConcurrentHashMap.newKeySet();

    public Tasks(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public BukkitTask sync(Runnable runnable) {
        return Bukkit.getScheduler().runTask(this.plugin, this.safe(runnable));
    }

    public BukkitTask async(Runnable runnable) {
        return Bukkit.getScheduler().runTaskAsynchronously(this.plugin, this.safe(runnable));
    }

    public BukkitTask later(Runnable runnable, long delay) {
        return Bukkit.getScheduler().runTaskLater(this.plugin, this.safe(runnable), delay);
    }

    public BukkitTask laterAsync(Runnable runnable, long delay) {
        return Bukkit.getScheduler().runTaskLaterAsynchronously(this.plugin, this.safe(runnable), delay);
    }

    /**
     * Runs a repeating task on the main thread.
     *
     * @param runnable what to run
     * @param delay ticks before the first run
     * @param period ticks between runs
     * @return the task, already tracked by {@link #cancelAll()}
     */
    public BukkitTask timer(Runnable runnable, long delay, long period) {
        return this.track(Bukkit.getScheduler().runTaskTimer(this.plugin, this.safe(runnable), delay, period));
    }

    public BukkitTask timerAsync(Runnable runnable, long delay, long period) {
        return this.track(Bukkit.getScheduler().runTaskTimerAsynchronously(this.plugin, this.safe(runnable), delay, period));
    }

    /**
     * Runs something off the main thread and hands the result back on the main thread.
     *
     * @param supplier work to do async
     * @param callback what to do with the result, on the main thread
     * @param <T> result type
     */
    public <T> void asyncThenSync(Supplier<T> supplier, Consumer<T> callback) {
        this.async(() -> {
            final T result = supplier.get();

            this.sync(() -> callback.accept(result));
        });
    }

    /**
     * Wraps async work in a future, completing it on whatever thread the work ran.
     *
     * @param supplier work to do async
     * @param <T> result type
     * @return the future
     */
    public <T> CompletableFuture<T> supply(Supplier<T> supplier) {
        final CompletableFuture<T> future = new CompletableFuture<>();

        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
            try {
                future.complete(supplier.get());
            } catch (Throwable throwable) {
                future.completeExceptionally(throwable);
            }
        });
        return future;
    }

    /**
     * Cancels every repeating task started through this class.
     */
    public void cancelAll() {
        for (BukkitTask task : this.repeating) {
            task.cancel();
        }

        this.repeating.clear();
    }

    private BukkitTask track(BukkitTask task) {
        this.repeating.removeIf(old -> !Bukkit.getScheduler().isQueued(old.getTaskId()) && !Bukkit.getScheduler().isCurrentlyRunning(old.getTaskId()));
        this.repeating.add(task);
        return task;
    }

    private Runnable safe(Runnable runnable) {
        return () -> {
            try {
                runnable.run();
            } catch (Throwable throwable) {
                this.plugin.getLogger().log(Level.SEVERE, "Task failed", throwable);
            }
        };
    }

}
