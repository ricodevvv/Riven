package dev.ricodevvv.riven.data.leaderboard;

import java.util.UUID;

/**
 * One row of a leaderboard.
 */
public class LeaderboardEntry {

    private final int position;
    private final UUID uniqueId;
    private final String name;
    private final long value;

    public LeaderboardEntry(int position, UUID uniqueId, String name, long value) {
        this.position = position;
        this.uniqueId = uniqueId;
        this.name = name;
        this.value = value;
    }

    public int getPosition() {
        return this.position;
    }

    public UUID getUniqueId() {
        return this.uniqueId;
    }

    public String getName() {
        return this.name;
    }

    public long getValue() {
        return this.value;
    }

}
