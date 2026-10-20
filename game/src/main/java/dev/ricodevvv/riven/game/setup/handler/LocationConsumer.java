package dev.ricodevvv.riven.game.setup.handler;

import org.bukkit.Location;

/**
 * A step that takes locations from the nether star.
 */
public interface LocationConsumer {

    /**
     * @param location where the builder stood when clicking
     */
    void provideLocation(Location location);

}
