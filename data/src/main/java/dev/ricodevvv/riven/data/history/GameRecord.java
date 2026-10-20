package dev.ricodevvv.riven.data.history;

import com.google.gson.JsonObject;

import java.util.UUID;

/**
 * A finished game: where, what, who won, how long and anything else as JSON.
 */
public class GameRecord {

    private final UUID id;
    private final String server;
    private final String mode;
    private final String map;
    private final String winner;
    private final long startedAt;
    private final long endedAt;
    private final JsonObject data;

    public GameRecord(UUID id, String server, String mode, String map, String winner, long startedAt, long endedAt, JsonObject data) {
        this.id = id;
        this.server = server;
        this.mode = mode;
        this.map = map;
        this.winner = winner;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.data = data == null ? new JsonObject() : data;
    }

    public UUID getId() {
        return this.id;
    }

    public String getServer() {
        return this.server;
    }

    public String getMode() {
        return this.mode;
    }

    public String getMap() {
        return this.map;
    }

    public String getWinner() {
        return this.winner;
    }

    public long getStartedAt() {
        return this.startedAt;
    }

    public long getEndedAt() {
        return this.endedAt;
    }

    public long getDuration() {
        return this.endedAt - this.startedAt;
    }

    public JsonObject getData() {
        return this.data;
    }

}
