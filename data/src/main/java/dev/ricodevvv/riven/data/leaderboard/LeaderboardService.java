package dev.ricodevvv.riven.data.leaderboard;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Keeps a cached top N of every registered board and refreshes it in the background, so placeholders and
 * holograms never hit the database. Values are submitted through {@link #submit(String, UUID, String, long)},
 * usually from a profile save listener.
 */
public class LeaderboardService {

    private final JavaPlugin plugin;
    private final LeaderboardStore store;
    private final int size;
    private final Set<String> boards = ConcurrentHashMap.newKeySet();
    private final Map<String, List<LeaderboardEntry>> cache = new ConcurrentHashMap<>();
    private BukkitTask task;

    /**
     * @param plugin owner plugin
     * @param store where values live
     * @param size how many rows to cache per board
     */
    public LeaderboardService(JavaPlugin plugin, LeaderboardStore store, int size) {
        this.plugin = plugin;
        this.store = store;
        this.size = size;
    }

    public LeaderboardService board(String board) {
        this.boards.add(board.toLowerCase());
        return this;
    }

    /**
     * Starts refreshing every {@code seconds}.
     *
     * @param seconds refresh period
     */
    public void start(int seconds) {
        this.task = Bukkit.getScheduler().runTaskTimerAsynchronously(this.plugin, this::refresh, 40L, seconds * 20L);
    }

    public void stop() {
        if (this.task != null) {
            this.task.cancel();
        }

        if (this.store instanceof MemoryLeaderboardStore) {
            ((MemoryLeaderboardStore) this.store).flush();
        }
    }

    /**
     * Reloads every board from the store. Blocking, it's called async by the timer.
     */
    public void refresh() {
        for (String board : this.boards) {
            try {
                this.cache.put(board, this.store.top(board, this.size));
            } catch (Exception e) {
                this.plugin.getLogger().log(Level.WARNING, "Couldn't refresh leaderboard " + board, e);
            }
        }

        if (this.store instanceof MemoryLeaderboardStore) {
            ((MemoryLeaderboardStore) this.store).flush();
        }
    }

    /**
     * Stores a value without blocking.
     *
     * @param board board id
     * @param uniqueId player
     * @param name name
     * @param value value
     */
    public void submit(String board, UUID uniqueId, String name, long value) {
        final String key = board.toLowerCase();

        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
            try {
                this.store.submit(key, uniqueId, name, value);
            } catch (Exception e) {
                this.plugin.getLogger().log(Level.WARNING, "Couldn't submit to leaderboard " + key, e);
            }
        });
    }

    /**
     * @param board board id
     * @return cached rows, highest first
     */
    public List<LeaderboardEntry> getTop(String board) {
        return this.cache.getOrDefault(board.toLowerCase(), Collections.emptyList());
    }

    /**
     * @param board board id
     * @param position 1 based position
     * @return the cached row, or null
     */
    public LeaderboardEntry getEntry(String board, int position) {
        final List<LeaderboardEntry> top = this.getTop(board);

        return position >= 1 && position <= top.size() ? top.get(position - 1) : null;
    }

    public LeaderboardStore getStore() {
        return this.store;
    }

    public Set<String> getBoards() {
        return Collections.unmodifiableSet(this.boards);
    }

}
