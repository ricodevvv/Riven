package dev.ricodevvv.riven.data.leaderboard;

import java.util.List;
import java.util.UUID;

/**
 * Where leaderboard values live. Every method blocks, call them off the main thread.
 */
public interface LeaderboardStore {

    /**
     * Stores (or updates) a player's value on a board.
     *
     * @param board board id, like "wins" or "season:2026:kills"
     * @param uniqueId player
     * @param name player name
     * @param value value
     */
    void submit(String board, UUID uniqueId, String name, long value);

    /**
     * @param board board id
     * @param limit how many rows
     * @return top rows, highest first
     */
    List<LeaderboardEntry> top(String board, int limit);

    /**
     * @param board board id
     * @param uniqueId player
     * @return their position (1 based), or -1 if they're not on it
     */
    int position(String board, UUID uniqueId);

    /**
     * Wipes a board (for a new season).
     *
     * @param board board id
     */
    void clear(String board);

}
