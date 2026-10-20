package dev.ricodevvv.riven.game.timeline;

/**
 * Something that happens at a fixed second of the game: a chest refill, the final event, the end.
 */
public class TimelineEvent {

    private final String id;
    private final String nameKey;
    private final long time;
    private final Runnable action;
    private boolean done;

    /**
     * @param id event id
     * @param nameKey language key (or text) shown on scoreboards, like "Refill"
     * @param time second of the game it fires at
     * @param action what it does
     */
    public TimelineEvent(String id, String nameKey, long time, Runnable action) {
        this.id = id;
        this.nameKey = nameKey;
        this.time = time;
        this.action = action;
    }

    public String getId() {
        return this.id;
    }

    public String getNameKey() {
        return this.nameKey;
    }

    public long getTime() {
        return this.time;
    }

    public boolean isDone() {
        return this.done;
    }

    void fire() {
        this.done = true;
        this.action.run();
    }

}
