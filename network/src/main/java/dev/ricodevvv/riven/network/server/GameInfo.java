package dev.ricodevvv.riven.network.server;

import com.google.gson.JsonObject;

/**
 * Public state of one game, shared with the network through the heartbeat.
 */
public class GameInfo {

    private String id;
    private String mode;
    private String map;
    private String state;
    private int players;
    private int maxPlayers;
    private boolean joinable;

    GameInfo() {}

    /**
     * @param id game id, unique in its server
     * @param mode game mode, like SOLO
     * @param map map name
     * @param state free state text (WAITING, STARTING, PLAYING...)
     * @param players players in
     * @param maxPlayers max players
     * @param joinable true if players can still join
     */
    public GameInfo(String id, String mode, String map, String state, int players, int maxPlayers, boolean joinable) {
        this.id = id;
        this.mode = mode;
        this.map = map;
        this.state = state;
        this.players = players;
        this.maxPlayers = maxPlayers;
        this.joinable = joinable;
    }

    public JsonObject toJson() {
        final JsonObject json = new JsonObject();

        json.addProperty("id", this.id);
        json.addProperty("mode", this.mode);
        json.addProperty("map", this.map);
        json.addProperty("state", this.state);
        json.addProperty("players", this.players);
        json.addProperty("maxPlayers", this.maxPlayers);
        json.addProperty("joinable", this.joinable);
        return json;
    }

    public static GameInfo fromJson(JsonObject json) {
        final GameInfo info = new GameInfo();

        info.id = json.get("id").getAsString();
        info.mode = json.get("mode").getAsString();
        info.map = json.get("map").getAsString();
        info.state = json.get("state").getAsString();
        info.players = json.get("players").getAsInt();
        info.maxPlayers = json.get("maxPlayers").getAsInt();
        info.joinable = json.get("joinable").getAsBoolean();
        return info;
    }

    public String getId() {
        return this.id;
    }

    public String getMode() {
        return this.mode;
    }

    public String getMap() {
        return this.map;
    }

    public String getState() {
        return this.state;
    }

    public int getPlayers() {
        return this.players;
    }

    public int getMaxPlayers() {
        return this.maxPlayers;
    }

    public boolean isJoinable() {
        return this.joinable;
    }

    /**
     * @return free slots right now
     */
    public int getFreeSlots() {
        return Math.max(0, this.maxPlayers - this.players);
    }

}
