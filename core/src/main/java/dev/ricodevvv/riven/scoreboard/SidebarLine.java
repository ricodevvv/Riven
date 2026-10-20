package dev.ricodevvv.riven.scoreboard;

import java.util.function.Consumer;

/**
 * A sidebar line. It can update by itself every X ticks.
 */
public class SidebarLine {

    private String value;
    private final Update update;

    public SidebarLine(String value) {
        this(value, null);
    }

    public SidebarLine(String value, Update update) {
        this.value = value;
        this.update = update;
    }

    public String getValue() {
        return this.value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Update getUpdate() {
        return this.update;
    }

    /**
     * Periodic update of a line.
     */
    public static class Update {

        private final Consumer<SidebarLine> action;
        private final int ticks;
        private int tickIndex;

        public Update(Consumer<SidebarLine> action, int ticks) {
            this.action = action;
            this.ticks = ticks;
        }

        /**
         * Moves one tick forward and runs the action when it's time.
         *
         * @param line line to update
         * @return true if it got updated
         */
        boolean onTick(SidebarLine line) {
            if (++this.tickIndex >= this.ticks) {
                this.tickIndex = 0;
                this.action.accept(line);
                return true;
            }
            return false;
        }

    }

}
