package dev.ricodevvv.riven.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class LocationWrapperTest {

    @Test
    void serializeRoundTrip() {
        final LocationWrapper wrapper = new LocationWrapper(1.5, 64, -3, 90, 10);
        final LocationWrapper read = LocationWrapper.deserialize(wrapper.serialize());

        assertEquals(1.5, read.getX());
        assertEquals(-3, read.getZ());
        assertEquals(90, read.getYaw());
    }

    @Test
    void brokenTextGivesNull() {
        assertNull(LocationWrapper.deserialize("1;2"));
        assertNull(LocationWrapper.deserialize("a;b;c"));
    }

}
