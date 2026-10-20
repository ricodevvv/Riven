package dev.ricodevvv.riven.util;

import org.bukkit.World;

/**
 * Saveable area: two corners without a world. Turns into an {@link Area} once you have the world.
 */
public class AreaWrapper {

    private LocationWrapper min;
    private LocationWrapper max;

    public AreaWrapper() {}

    public AreaWrapper(LocationWrapper min, LocationWrapper max) {
        this.min = min;
        this.max = max;
    }

    /**
     * Applies the area to a world.
     *
     * @param world target world
     * @return the area ready to use
     */
    public Area asArea(World world) {
        return new Area(this.min.asBukkit(world), this.max.asBukkit(world));
    }

    public LocationWrapper getMin() {
        return this.min;
    }

    public LocationWrapper getMax() {
        return this.max;
    }

    @Override
    public String toString() {
        return "[" + this.min + "] -> [" + this.max + "]";
    }

}
