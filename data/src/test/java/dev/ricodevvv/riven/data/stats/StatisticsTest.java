package dev.ricodevvv.riven.data.stats;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatisticsTest {

    @Test
    void addTouchesEveryScope() {
        final Statistics stats = new Statistics();
        final String[] scopes = StatScope.of("solo", "archer", "2026");

        stats.add("kills", 2, scopes);

        assertEquals(2, stats.get(StatScope.TOTAL, "KILLS"));
        assertEquals(2, stats.get(StatScope.mode("SOLO"), "kills"));
        assertEquals(2, stats.get(StatScope.kit("ARCHER"), "kills"));
        assertEquals(2, stats.get(StatScope.season("2026", "solo"), "kills"));
        assertTrue(Arrays.asList(scopes).contains(StatScope.currentYear()));
    }

    @Test
    void recordsKeepBestValues() {
        final Statistics stats = new Statistics();

        stats.max("most_kills", 5, StatScope.TOTAL);
        stats.max("most_kills", 3, StatScope.TOTAL);
        stats.min("fastest", 120, StatScope.TOTAL);
        stats.min("fastest", 90, StatScope.TOTAL);
        stats.min("fastest", 0, StatScope.TOTAL);

        assertEquals(5, stats.get(StatScope.TOTAL, "most_kills"));
        assertEquals(90, stats.get(StatScope.TOTAL, "fastest"));
    }

    @Test
    void ratioHandlesZero() {
        final Statistics stats = new Statistics();

        stats.set(StatScope.TOTAL, "kills", 10);
        assertEquals(10, stats.ratio(StatScope.TOTAL, "kills", "deaths"));

        stats.set(StatScope.TOTAL, "deaths", 3);
        assertEquals(3.33, stats.ratio(StatScope.TOTAL, "kills", "deaths"));
    }

}
