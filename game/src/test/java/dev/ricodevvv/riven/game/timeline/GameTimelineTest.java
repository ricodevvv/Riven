package dev.ricodevvv.riven.game.timeline;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class GameTimelineTest {

    @Test
    void firesDueEventsOnce() {
        final AtomicInteger refills = new AtomicInteger();
        final GameTimeline timeline = new GameTimeline().repeat("refill", "Refill", 60, 60, 180, refills::incrementAndGet);

        timeline.tick(59);
        assertEquals(0, refills.get());
        assertEquals(1, timeline.getTimeToNext());

        timeline.tick(120);
        assertEquals(2, refills.get());

        timeline.tick(121);
        assertEquals(2, refills.get());
        assertEquals(59, timeline.getTimeToNext());
    }

    @Test
    void cancelDropsPendingEvents() {
        final GameTimeline timeline = new GameTimeline().repeat("refill", "Refill", 60, 60, 180, () -> {});

        timeline.tick(60);
        timeline.cancel("refill");

        assertNull(timeline.getNext());
        assertEquals(-1, timeline.getTimeToNext());
    }

}
