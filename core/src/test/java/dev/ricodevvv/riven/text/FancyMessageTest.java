package dev.ricodevvv.riven.text;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FancyMessageTest {

    @Test
    void findsBlocks() {
        assertTrue(FancyMessage.isFancy("&aDone {{&b[GO]|run:/sws setup map}}"));
        assertFalse(FancyMessage.isFancy("&aDone, no buttons here"));
    }

    @Test
    void blocksCanHaveMultiLineHovers() {
        assertTrue(FancyMessage.isFancy("{{&b/sws create|suggest:/sws create |hover:&bFirst line\n&7Second line}}"));
    }

}
