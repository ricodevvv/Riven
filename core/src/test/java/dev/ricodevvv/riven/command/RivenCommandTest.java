package dev.ricodevvv.riven.command;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RivenCommandTest {

    @Test
    void clickableTextCantBreakTheBlock() {
        assertEquals("<SOLO/TEAM> {a} }", RivenCommand.clickable("<SOLO|TEAM> {a}}"));
    }

}
