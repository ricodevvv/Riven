package dev.ricodevvv.riven.game.timeline;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * List of timed events for a game. Feed it the game second (for example from {@code GameTimer.addListener})
 * and it fires whatever is due. Scoreboards ask it for the next event and the time left.
 *
 * <pre>
 * GameTimeline timeline = new GameTimeline()
 *         .add(new TimelineEvent("refill", "event.refill", 120, this::refill))
 *         .add(new TimelineEvent("final", "event.final", 600, this::finalEvent));
 * game.getTimer().addListener(timeline::tick);
 * </pre>
 */
public class GameTimeline {

    private final List<TimelineEvent> events = new ArrayList<>();
    private long current;

    public GameTimeline add(TimelineEvent event) {
        this.events.add(event);
        this.events.sort(Comparator.comparingLong(TimelineEvent::getTime));
        return this;
    }

    /**
     * Adds the same event every {@code interval} seconds from {@code first} up to {@code until}.
     *
     * @param id base id, the index is appended
     * @param nameKey language key
     * @param first first second
     * @param interval seconds between events
     * @param until last possible second
     * @param action what it does
     * @return this timeline
     */
    public GameTimeline repeat(String id, String nameKey, long first, long interval, long until, Runnable action) {
        int index = 0;

        for (long time = first; time <= until && interval > 0; time += interval) {
            this.add(new TimelineEvent(id + "-" + index++, nameKey, time, action));
        }
        return this;
    }

    /**
     * Moves the timeline to the given second, firing every event that's due.
     *
     * @param second current game second
     */
    public void tick(long second) {
        this.current = second;

        for (TimelineEvent event : new ArrayList<>(this.events)) {
            if (!event.isDone() && event.getTime() <= second) {
                event.fire();
            }
        }
    }

    /**
     * Removes every pending event with that id or id prefix (for example to cancel the remaining refills).
     *
     * @param idPrefix id or prefix
     */
    public void cancel(String idPrefix) {
        this.events.removeIf(event -> !event.isDone() && event.getId().startsWith(idPrefix));
    }

    /**
     * @return the next pending event, or null
     */
    public TimelineEvent getNext() {
        for (TimelineEvent event : this.events) {
            if (!event.isDone()) {
                return event;
            }
        }
        return null;
    }

    /**
     * @return seconds until the next event, or -1 if there's none
     */
    public long getTimeToNext() {
        final TimelineEvent next = this.getNext();

        return next == null ? -1 : Math.max(0, next.getTime() - this.current);
    }

    public long getCurrent() {
        return this.current;
    }

    public List<TimelineEvent> getEvents() {
        return Collections.unmodifiableList(this.events);
    }

}
