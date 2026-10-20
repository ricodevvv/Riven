package dev.ricodevvv.riven.data.level;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LevelCurveTest {

    @Test
    void formulaCurve() {
        final LevelCurve curve = new LevelCurve(500, 250, 15000, 0, null);

        assertEquals(1, curve.getLevel(0));
        assertEquals(1, curve.getLevel(499));
        assertEquals(2, curve.getLevel(500));
        assertEquals(3, curve.getLevel(1250));
        assertEquals(100, curve.getProgress(600));
        assertEquals(750, curve.getRequired(600));
    }

    @Test
    void capAndMaxLevel() {
        final LevelCurve curve = new LevelCurve(500, 10000, 1000, 3, null);

        assertEquals(1000, curve.xpForLevel(5));
        assertEquals(3, curve.getLevel(1_000_000));
    }

    @Test
    void fixedList() {
        final LevelCurve curve = new LevelCurve(1, 0, 0, 0, Arrays.asList(100L, 200L));

        assertEquals(2, curve.getLevel(100));
        assertEquals(3, curve.getLevel(300));
        assertEquals(200, curve.xpForLevel(10));
    }

}
