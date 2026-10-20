package dev.ricodevvv.riven.network;

import com.google.gson.JsonObject;
import dev.ricodevvv.riven.network.server.GameInfo;
import dev.ricodevvv.riven.network.server.ServerInfo;
import dev.ricodevvv.riven.network.server.ServerMode;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NetworkMessageTest {

    @Test
    void messageRoundTrip() {
        final JsonObject payload = new JsonObject();

        payload.addProperty("map", "Skyblock");

        final NetworkMessage message = new NetworkMessage("games:list", "lobby-1", "game-2", null, payload);
        final NetworkMessage read = NetworkMessage.deserialize(message.serialize());

        assertEquals(message.getId(), read.getId());
        assertEquals("game-2", read.getTarget());
        assertNull(read.getReplyTo());
        assertEquals("Skyblock", read.getPayload().get("map").getAsString());
        assertNull(NetworkMessage.deserialize("not json"));
    }

    @Test
    void serverInfoRoundTrip() {
        final GameInfo game = new GameInfo("solo-1", "SOLO", "Skyblock", "WAITING", 3, 12, true);
        final ServerInfo info = new ServerInfo("game-1", "skywars", ServerMode.GAME, 3, 100, 4, false, System.currentTimeMillis(), Collections.singletonList(game));
        final ServerInfo read = ServerInfo.fromJson(info.toJson());

        assertEquals("game-1", read.getId());
        assertEquals(9, read.getGames().get(0).getFreeSlots());
        assertEquals(3, read.getPlayersIn("solo"));
        assertTrue(read.canCreateGame());
        assertTrue(read.isAlive(30_000));
        assertFalse(new ServerInfo("old", "skywars", ServerMode.GAME, 0, 1, 1, false, 0, null).isAlive(30_000));
    }

}
