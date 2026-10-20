package dev.ricodevvv.riven.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TimeUtilTest {

    @Test
    void formatsMinutesAndHours() {
        assertEquals("1:05", TimeUtil.formatTime(65));
        assertEquals("1:00:00", TimeUtil.formatTime(3600));
    }

    @Test
    void parsesUnits() {
        assertEquals(90, TimeUtil.parseSeconds("90"));
        assertEquals(300, TimeUtil.parseSeconds("5m"));
        assertEquals(7200, TimeUtil.parseSeconds("2h"));
        assertEquals(-1, TimeUtil.parseSeconds("abc"));
    }

}
