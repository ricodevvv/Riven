package dev.ricodevvv.riven.game.setup.handler;

import dev.ricodevvv.riven.game.setup.ConfigProcess;
import dev.ricodevvv.riven.game.setup.SetupManager;
import dev.ricodevvv.riven.util.AreaWrapper;
import dev.ricodevvv.riven.util.LocationWrapper;
import dev.ricodevvv.riven.util.Title;
import org.bukkit.ChatColor;
import org.bukkit.Location;

/**
 * An area: both corners get marked with the star.
 */
public class AreaHandler extends ConfigOptionHandler<AreaWrapper> implements LocationConsumer {

    private LocationWrapper first;

    public AreaHandler(ConfigProcess<?> process) {
        super(process);
    }

    @Override
    public void handle() {
        this.process.getManager().giveItem(this.player, SetupManager.LOCATION_ITEM, 0);
        this.process.sendMessage(this.process.text("setup.area.first"));
    }

    @Override
    public void provideLocation(Location location) {
        final LocationWrapper corner = LocationHandler.wrap(location, false, true);

        Title.sendTitle(this.player, ChatColor.AQUA + this.process.text(this.first == null ? "setup.area.first-title" : "setup.area.second-title"),
                "X: " + corner.getX() + "; Y: " + corner.getY() + "; Z: " + corner.getZ(), 2, 25, 2);

        if (this.first == null) {
            this.first = corner;
            this.process.sendMessage(this.process.text("setup.area.second"));
            return;
        }

        this.player.getInventory().clear();

        this.complete(new AreaWrapper(this.first, corner));
    }

}
