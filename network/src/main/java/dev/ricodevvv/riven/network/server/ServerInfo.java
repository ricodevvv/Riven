package dev.ricodevvv.riven.network.server;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Snapshot of a server as seen by the rest of the network.
 */
public class ServerInfo {

    private final String id;
    private final String group;
    private final ServerMode mode;
    private final int players;
    private final int maxPlayers;
    private final int maxGames;
    private final boolean draining;
    private final long heartbeat;
    private final List<GameInfo> games;

    /**
     * @param id server name on the proxy
     * @param group group of servers, like "skywars"
     * @param mode lobby or game
     * @param players online players
     * @param maxPlayers player capacity
     * @param maxGames how many games it can run at once, 0 if it can't create games
     * @param draining true if it doesn't take new games
     * @param heartbeat last heartbeat time
     * @param games its games
     */
    public ServerInfo(String id, String group, ServerMode mode, int players, int maxPlayers, int maxGames, boolean draining, long heartbeat, List<GameInfo> games) {
        this.id = id;
        this.group = group;
        this.mode = mode;
        this.players = players;
        this.maxPlayers = maxPlayers;
        this.maxGames = maxGames;
        this.draining = draining;
        this.heartbeat = heartbeat;
        this.games = games == null ? new ArrayList<>() : new ArrayList<>(games);
    }

    public JsonObject toJson() {
        final JsonObject json = new JsonObject();
        final JsonArray games = new JsonArray();

        for (GameInfo game : this.games) {
            games.add(game.toJson());
        }

        json.addProperty("id", this.id);
        json.addProperty("group", this.group);
        json.addProperty("mode", this.mode.name());
        json.addProperty("players", this.players);
        json.addProperty("maxPlayers", this.maxPlayers);
        json.addProperty("maxGames", this.maxGames);
        json.addProperty("draining", this.draining);
        json.addProperty("heartbeat", this.heartbeat);
        json.add("games", games);
        return json;
    }

    public static ServerInfo fromJson(JsonObject json) {
        final List<GameInfo> games = new ArrayList<>();

        if (json.has("games")) {
            for (JsonElement element : json.getAsJsonArray("games")) {
                games.add(GameInfo.fromJson(element.getAsJsonObject()));
            }
        }

        return new ServerInfo(json.get("id").getAsString(), json.get("group").getAsString(), ServerMode.valueOf(json.get("mode").getAsString()),
                json.get("players").getAsInt(), json.get("maxPlayers").getAsInt(), json.get("maxGames").getAsInt(),
                json.get("draining").getAsBoolean(), json.get("heartbeat").getAsLong(), games);
    }

    /**
     * @param timeoutMillis how long without heartbeat counts as dead
     * @return true if it sent a heartbeat recently
     */
    public boolean isAlive(long timeoutMillis) {
        return System.currentTimeMillis() - this.heartbeat <= timeoutMillis;
    }

    /**
     * @return true if it can still start one more game
     */
    public boolean canCreateGame() {
        return this.mode == ServerMode.GAME && !this.draining && this.maxGames > 0 && this.games.size() < this.maxGames;
    }

    public String getId() {
        return this.id;
    }

    public String getGroup() {
        return this.group;
    }

    public ServerMode getMode() {
        return this.mode;
    }

    public int getPlayers() {
        return this.players;
    }

    public int getMaxPlayers() {
        return this.maxPlayers;
    }

    public int getMaxGames() {
        return this.maxGames;
    }

    public boolean isDraining() {
        return this.draining;
    }

    public long getHeartbeat() {
        return this.heartbeat;
    }

    public List<GameInfo> getGames() {
        return Collections.unmodifiableList(this.games);
    }

    /**
     * @param mode game mode, null for any
     * @return players in games of that mode
     */
    public int getPlayersIn(String mode) {
        int total = 0;

        for (GameInfo game : this.games) {
            if (mode == null || game.getMode().equalsIgnoreCase(mode)) {
                total += game.getPlayers();
            }
        }
        return total;
    }

}
