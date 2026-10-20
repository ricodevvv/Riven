package dev.ricodevvv.riven.game.setup.handler;

import dev.ricodevvv.riven.game.setup.ConfigProcess;
import dev.ricodevvv.riven.game.setup.SetupManager;
import dev.ricodevvv.riven.util.AreaWrapper;
import dev.ricodevvv.riven.util.LocationWrapper;
import dev.ricodevvv.riven.util.Title;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;

/**
 * Several areas: a sign asks how many, then the corners of each one get marked.
 */
public class AreasHandler extends ConfigOptionHandler<List<AreaWrapper>> implements LocationConsumer {

    private final List<AreaWrapper> result = new ArrayList<>();
    private LocationWrapper first;
    private int size = -1;

    public AreasHandler(ConfigProcess<?> process) {
        super(process);
    }

    @Override
    public void handle() {
        Bukkit.getScheduler().runTaskLater(this.process.getManager().getPlugin(), () -> CountInput.ask(this.process, "setup.sign.areas", 0, value -> {
            this.size = value;

            if (value == 0) {
                this.complete(new ArrayList<>());
                return;
            }

            this.process.getManager().giveItem(this.player, SetupManager.LOCATION_ITEM, 0);
            this.sendProgress("setup.area.first");
        }), 5L);
    }

    private void sendProgress(String key) {
        this.process.sendMessage(this.process.text(key) + ChatColor.DARK_GRAY + " (" + (this.result.size() + 1) + "/" + this.size + ")");
    }

    @Override
    public void provideLocation(Location location) {
        if (this.size <= 0) {
            return;
        }

        final LocationWrapper corner = LocationHandler.wrap(location, false, true);

        Title.sendTitle(this.player, ChatColor.AQUA + this.process.text(this.first == null ? "setup.area.first-title" : "setup.area.second-title"),
                "X: " + corner.getX() + "; Y: " + corner.getY() + "; Z: " + corner.getZ(), 2, 25, 2);

        if (this.first == null) {
            this.first = corner;
            this.sendProgress("setup.area.second");
            return;
        }

        this.result.add(new AreaWrapper(this.first, corner));
        this.first = null;

        if (this.result.size() >= this.size) {
            this.player.getInventory().clear();

            this.complete(this.result);
            return;
        }

        this.sendProgress("setup.area.first");
    }

}
