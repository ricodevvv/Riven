package dev.ricodevvv.riven.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextUtilTest {

    @Test
    void romanNumbersInRange() {
        assertEquals("IV", TextUtil.toRoman(4));
        assertEquals("11", TextUtil.toRoman(11));
    }

    @Test
    void prettyEnumName() {
        assertEquals("Final Kill", TextUtil.prettyEnum("FINAL_KILL"));
    }

    @Test
    void progressBarSplitsColors() {
        assertEquals("aXXbXX", TextUtil.progressBar(5, 10, 4, "X", "a", "b"));
        assertEquals("aXXXX", TextUtil.progressBar(10, 10, 4, "X", "a", "b"));
    }

    @Test
    void numbersGetSeparators() {
        assertEquals("12,345", TextUtil.formatNumber(12345));
    }

    @Test
    void centerAddsPadding() {
        assertTrue(TextUtil.center("hi").startsWith(" "));
    }

}
