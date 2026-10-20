package dev.ricodevvv.riven.data.leaderboard;

import dev.ricodevvv.riven.data.sql.Database;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Leaderboards in a single SQL table indexed by board and value, so the top N is one cheap query even with
 * thousands of players.
 */
public class SqlLeaderboardStore implements LeaderboardStore {

    private final Database database;
    private final String table;

    public SqlLeaderboardStore(Database database) {
        this.database = database;
        this.table = database.table("leaderboards");

        try {
            database.update("CREATE TABLE IF NOT EXISTS " + this.table + " (board VARCHAR(64) NOT NULL, uuid VARCHAR(36) NOT NULL, name VARCHAR(16) NOT NULL, "
                    + "value BIGINT NOT NULL, PRIMARY KEY (board, uuid))");

            if (database.isSqlite()) {
                database.update("CREATE INDEX IF NOT EXISTS " + this.table + "_value ON " + this.table + " (board, value)");
            } else {
                database.query("SHOW INDEX FROM " + this.table + " WHERE Key_name = 'board_value'", result -> {
                    if (!result.next()) {
                        database.update("CREATE INDEX board_value ON " + this.table + " (board, value)");
                    }
                    return null;
                });
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Couldn't create the leaderboards table", e);
        }
    }

    @Override
    public void submit(String board, UUID uniqueId, String name, long value) {
        final String sql = this.database.isSqlite()
                ? "INSERT OR REPLACE INTO " + this.table + " (board, uuid, name, value) VALUES (?, ?, ?, ?)"
                : "INSERT INTO " + this.table + " (board, uuid, name, value) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE name = VALUES(name), value = VALUES(value)";

        try {
            this.database.update(sql, board, uniqueId.toString(), name, value);
        } catch (SQLException e) {
            throw new IllegalStateException("Couldn't submit to " + board, e);
        }
    }

    @Override
    public List<LeaderboardEntry> top(String board, int limit) {
        try {
            return this.database.query("SELECT uuid, name, value FROM " + this.table + " WHERE board = ? ORDER BY value DESC LIMIT ?", result -> {
                final List<LeaderboardEntry> entries = new ArrayList<>();

                while (result.next()) {
                    entries.add(new LeaderboardEntry(entries.size() + 1, UUID.fromString(result.getString(1)), result.getString(2), result.getLong(3)));
                }
                return entries;
            }, board, limit);
        } catch (SQLException e) {
            throw new IllegalStateException("Couldn't read " + board, e);
        }
    }

    @Override
    public int position(String board, UUID uniqueId) {
        try {
            return this.database.query("SELECT COUNT(*) + 1 FROM " + this.table + " WHERE board = ? AND value > (SELECT value FROM " + this.table + " WHERE board = ? AND uuid = ?)",
                    result -> result.next() ? result.getInt(1) : -1, board, board, uniqueId.toString());
        } catch (SQLException e) {
            throw new IllegalStateException("Couldn't read " + board, e);
        }
    }

    @Override
    public void clear(String board) {
        try {
            this.database.update("DELETE FROM " + this.table + " WHERE board = ?", board);
        } catch (SQLException e) {
            throw new IllegalStateException("Couldn't clear " + board, e);
        }
    }

}
