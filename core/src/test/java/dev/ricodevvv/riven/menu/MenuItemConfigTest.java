package dev.ricodevvv.riven.menu;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MenuItemConfigTest {

    @Test
    void parsesRangesAndLists() {
        assertEquals(Arrays.asList(10, 11, 12), MenuItemConfig.parseSlots("10-12"));
        assertEquals(Arrays.asList(1, 5, 7, 8), MenuItemConfig.parseSlots("1, 5, 7-8"));
        assertEquals(Arrays.asList(3), MenuItemConfig.parseSlots("3,x"));
    }

}
