package dev.ricodevvv.riven.data.leaderboard;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MemoryLeaderboardStoreTest {

    @TempDir
    File folder;

    @Test
    void topAndPositionSurviveFlush() {
        final File file = new File(this.folder, "boards.json");
        final MemoryLeaderboardStore store = new MemoryLeaderboardStore(file);
        final UUID first = UUID.randomUUID();
        final UUID second = UUID.randomUUID();

        store.submit("wins", first, "Alice", 10);
        store.submit("wins", second, "Bob", 25);
        store.flush();

        final MemoryLeaderboardStore reloaded = new MemoryLeaderboardStore(file);
        final List<LeaderboardEntry> top = reloaded.top("wins", 10);

        assertEquals(2, top.size());
        assertEquals("Bob", top.get(0).getName());
        assertEquals(2, reloaded.position("wins", first));
        assertEquals(-1, reloaded.position("wins", UUID.randomUUID()));
    }

}
