package dev.ricodevvv.riven.game.generator;

import dev.ricodevvv.riven.item.ItemNBT;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry of running generators. A single listener for every pickup, no matter how many arenas.
 */
public class GeneratorManager implements Listener {

    private final JavaPlugin plugin;
    private final Map<String, Generator> generators = new ConcurrentHashMap<>();

    public GeneratorManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        Bukkit.getPluginManager().registerEvents(this, this.plugin);
    }

    public void stop() {
        HandlerList.unregisterAll(this);

        for (Generator generator : new ArrayList<>(this.generators.values())) {
            generator.remove();
        }
    }

    void register(Generator generator) {
        this.generators.put(generator.getId(), generator);
    }

    void unregister(Generator generator) {
        this.generators.remove(generator.getId());
    }

    public Collection<Generator> getGenerators() {
        return this.generators.values();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPickup(PlayerPickupItemEvent event) {
        final ItemNBT nbt = new ItemNBT(event.getItem().getItemStack());

        if (!nbt.hasTag(Generator.ITEM_TAG)) {
            return;
        }

        final Generator generator = this.generators.get(nbt.getString(Generator.ITEM_TAG));

        if (generator == null) {
            event.getItem().setItemStack(nbt.removeTag(Generator.ITEM_TAG).build());
            return;
        }

        generator.handlePickup(event);
    }

}
