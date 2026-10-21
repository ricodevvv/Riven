package dev.ricodevvv.riven.game.world;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Copying the folder of a world while the server has it loaded gives you broken region files (chunks end up in
 * the wrong spot, "Chunk file at x,z is in the wrong location"), because chunks keep being written in the
 * background. This gets a world's files to a safe state first: everything saved, every pending write done and,
 * when possible, the world unloaded.
 */
public final class LiveWorlds {

    private LiveWorlds() {}

    /**
     * Gets a world ready to be copied. Players inside wait in the main world and come back with {@code restore}.
     * The main world can't be unloaded, so it only gets saved and flushed.
     *
     * @param plugin plugin, for logs
     * @param worldName world about to be copied
     * @return what puts the world (and its players) back, run it on the main thread once the copy is done
     */
    public static Runnable release(JavaPlugin plugin, String worldName) {
        final World world = Bukkit.getWorld(worldName);

        if (world == null) {
            return () -> {};
        }

        world.save();
        flush();

        final World main = Bukkit.getWorlds().get(0);

        if (world.equals(main)) {
            plugin.getLogger().warning("Copying " + worldName + ", the main world, while the server runs. If the map comes out broken, put it in its own world folder.");
            return () -> {};
        }

        final WorldCreator creator = new WorldCreator(worldName).copy(world);
        final Map<UUID, Location> players = new HashMap<>();

        for (Player player : world.getPlayers()) {
            players.put(player.getUniqueId(), player.getLocation());
            player.teleport(main.getSpawnLocation());
        }

        if (!Bukkit.unloadWorld(world, true)) {
            plugin.getLogger().warning("Couldn't unload " + worldName + " before copying it, the copy could come out broken");
            flush();
            return () -> {};
        }

        flush();

        return () -> {
            final World reloaded = Bukkit.createWorld(creator);

            for (Map.Entry<UUID, Location> entry : players.entrySet()) {
                final Player player = Bukkit.getPlayer(entry.getKey());
                final Location location = entry.getValue();

                if (player != null && reloaded != null) {
                    player.teleport(new Location(reloaded, location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch()));
                }
            }
        };
    }

    /**
     * Waits until the server wrote every chunk it had queued to disk. Does nothing on servers where the internals
     * aren't there.
     */
    public static void flush() {
        try {
            final String version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
            final Class<?> thread = Class.forName("net.minecraft.server." + version + ".FileIOThread");

            thread.getMethod("b").invoke(thread.getMethod("a").invoke(null));
        } catch (Throwable ignored) {
        }
    }

}
