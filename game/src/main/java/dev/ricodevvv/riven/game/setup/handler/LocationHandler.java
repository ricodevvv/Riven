package dev.ricodevvv.riven.game.setup.handler;

import dev.ricodevvv.riven.game.setup.ConfigProcess;
import dev.ricodevvv.riven.game.setup.SetupManager;
import dev.ricodevvv.riven.util.LocationWrapper;
import org.bukkit.Location;

/**
 * A single location. It can keep the facing direction or snap to the block.
 */
public class LocationHandler extends ConfigOptionHandler<LocationWrapper> implements LocationConsumer {

    private final boolean direction;
    private final boolean rounded;

    public LocationHandler(ConfigProcess<?> process, boolean direction, boolean rounded) {
        super(process);
        this.direction = direction;
        this.rounded = rounded;
    }

    @Override
    public void handle() {
        this.process.getManager().giveItem(this.player, SetupManager.LOCATION_ITEM, 0);
    }

    @Override
    public void provideLocation(Location location) {
        this.player.getInventory().clear();

        this.complete(wrap(location, this.direction, this.rounded));
    }

    /**
     * @param location bukkit location
     * @param direction true to keep yaw and pitch
     * @param rounded true to snap to the block
     * @return the saveable location
     */
    static LocationWrapper wrap(Location location, boolean direction, boolean rounded) {
        final LocationWrapper result = rounded
                ? new LocationWrapper(location.getBlockX(), location.getBlockY(), location.getBlockZ())
                : new LocationWrapper(location.getX(), location.getY(), location.getZ());

        if (direction) {
            result.setYaw(location.getYaw());
            result.setPitch(location.getPitch());
        }
        return result;
    }

}
