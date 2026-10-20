package dev.ricodevvv.riven.data.history;

import com.google.gson.JsonParser;
import dev.ricodevvv.riven.data.sql.Database;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Game history in SQL. Every finished game is one row; per-player results go in the JSON data.
 */
public class GameHistoryStore {

    private final Database database;
    private final String table;

    public GameHistoryStore(Database database) {
        this.database = database;
        this.table = database.table("game_history");

        try {
            database.update("CREATE TABLE IF NOT EXISTS " + this.table + " (id VARCHAR(36) NOT NULL PRIMARY KEY, server VARCHAR(64), mode VARCHAR(32), map VARCHAR(64), "
                    + "winner VARCHAR(64), started_at BIGINT, ended_at BIGINT, data " + database.longTextType() + ")");
        } catch (SQLException e) {
            throw new IllegalStateException("Couldn't create the game history table", e);
        }
    }

    public CompletableFuture<Integer> save(GameRecord record) {
        return this.database.updateAsync("INSERT INTO " + this.table + " (id, server, mode, map, winner, started_at, ended_at, data) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                record.getId().toString(), record.getServer(), record.getMode(), record.getMap(), record.getWinner(), record.getStartedAt(), record.getEndedAt(), record.getData().toString());
    }

    /**
     * @param limit how many
     * @return the latest games, newest first
     */
    public CompletableFuture<List<GameRecord>> latest(int limit) {
        return this.database.queryAsync("SELECT id, server, mode, map, winner, started_at, ended_at, data FROM " + this.table + " ORDER BY ended_at DESC LIMIT ?", result -> {
            final List<GameRecord> records = new ArrayList<>();
            final JsonParser parser = new JsonParser();

            while (result.next()) {
                records.add(new GameRecord(UUID.fromString(result.getString(1)), result.getString(2), result.getString(3), result.getString(4), result.getString(5),
                        result.getLong(6), result.getLong(7), result.getString(8) == null ? null : parser.parse(result.getString(8)).getAsJsonObject()));
            }
            return records;
        }, limit);
    }

    /**
     * Deletes games older than a timestamp.
     *
     * @param before cutoff in milliseconds
     * @return the future with the deleted rows
     */
    public CompletableFuture<Integer> prune(long before) {
        return this.database.updateAsync("DELETE FROM " + this.table + " WHERE ended_at < ?", before);
    }

}
