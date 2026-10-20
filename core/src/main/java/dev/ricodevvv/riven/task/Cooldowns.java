package dev.ricodevvv.riven.task;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-player cooldowns by key. Nothing runs in the background, it just compares timestamps.
 */
public class Cooldowns {

    private final Map<String, Long> expirations = new ConcurrentHashMap<>();

    /**
     * Starts a cooldown.
     *
     * @param playerId player
     * @param key cooldown name
     * @param millis duration in milliseconds
     */
    public void put(UUID playerId, String key, long millis) {
        this.expirations.put(playerId + ":" + key, System.currentTimeMillis() + millis);
    }

    /**
     * @param playerId player
     * @param key cooldown name
     * @return true if the cooldown is still running
     */
    public boolean has(UUID playerId, String key) {
        return this.getRemaining(playerId, key) > 0;
    }

    /**
     * Checks and starts in one go: returns false if it's still cooling down, otherwise starts it and returns true.
     *
     * @param playerId player
     * @param key cooldown name
     * @param millis duration in milliseconds
     * @return true if the action can go ahead
     */
    public boolean tryUse(UUID playerId, String key, long millis) {
        if (this.has(playerId, key)) {
            return false;
        }

        this.put(playerId, key, millis);
        return true;
    }

    /**
     * @param playerId player
     * @param key cooldown name
     * @return milliseconds left, 0 if it's over
     */
    public long getRemaining(UUID playerId, String key) {
        final Long expiration = this.expirations.get(playerId + ":" + key);

        if (expiration == null) {
            return 0;
        }

        final long remaining = expiration - System.currentTimeMillis();

        if (remaining <= 0) {
            this.expirations.remove(playerId + ":" + key);
            return 0;
        }
        return remaining;
    }

    public void clear(UUID playerId) {
        this.expirations.keySet().removeIf(key -> key.startsWith(playerId + ":"));
    }

}
