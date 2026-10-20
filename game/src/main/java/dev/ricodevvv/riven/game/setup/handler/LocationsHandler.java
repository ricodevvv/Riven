package dev.ricodevvv.riven.game.setup.handler;

import dev.ricodevvv.riven.game.setup.ConfigProcess;
import dev.ricodevvv.riven.game.setup.SetupManager;
import dev.ricodevvv.riven.util.LocationWrapper;
import dev.ricodevvv.riven.util.Title;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;

/**
 * Several locations: first a sign asks how many, then they get marked one by one.
 */
public class LocationsHandler extends ConfigOptionHandler<List<LocationWrapper>> implements LocationConsumer {

    private final List<LocationWrapper> result = new ArrayList<>();
    private final boolean direction;
    private final boolean rounded;
    private int size = -1;

    public LocationsHandler(ConfigProcess<?> process, boolean direction, boolean rounded) {
        super(process);
        this.direction = direction;
        this.rounded = rounded;
    }

    @Override
    public void handle() {
        Bukkit.getScheduler().runTaskLater(this.process.getManager().getPlugin(), () -> CountInput.ask(this.process, "setup.sign.locations", 0, value -> {
            this.size = value;

            if (value == 0) {
                this.complete(new ArrayList<>());
                return;
            }

            this.process.getManager().giveItem(this.player, SetupManager.LOCATION_ITEM, 0);
        }), 5L);
    }

    @Override
    public void provideLocation(Location location) {
        if (this.size <= 0) {
            return;
        }

        final LocationWrapper wrapper = LocationHandler.wrap(location, this.direction, this.rounded);

        this.result.add(wrapper);

        Title.sendTitle(this.player, ChatColor.AQUA.toString() + this.result.size() + "/" + this.size,
                "X: " + wrapper.getX() + "; Y: " + wrapper.getY() + "; Z: " + wrapper.getZ(), 2, 25, 2);

        if (this.result.size() >= this.size) {
            this.player.getInventory().clear();

            this.complete(this.result);
        }
    }

}
