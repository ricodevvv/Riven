package dev.ricodevvv.riven.update;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpdateCheckerTest {

    @Test
    void comparesVersions() {
        assertTrue(UpdateChecker.isNewer("1.2.0", "1.1.9"));
        assertTrue(UpdateChecker.isNewer("1.0.1-build.4", "1.0.0"));
        assertFalse(UpdateChecker.isNewer("1.0.0-build.9", "1.0.0"));
        assertFalse(UpdateChecker.isNewer("0.9", "1.0"));
    }

}
