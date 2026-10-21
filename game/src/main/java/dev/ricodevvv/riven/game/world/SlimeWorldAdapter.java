package dev.ricodevvv.riven.game.world;

import com.grinderwolf.swm.api.SlimePlugin;
import com.grinderwolf.swm.api.loaders.SlimeLoader;
import com.grinderwolf.swm.api.world.SlimeWorld;
import com.grinderwolf.swm.api.world.properties.SlimeProperties;
import com.grinderwolf.swm.api.world.properties.SlimePropertyMap;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;

/**
 * Maps stored with SlimeWorldManager (like BedWars2023's SlimeAdapter). Every game uses an in-memory copy of the
 * map, so loading and dropping arenas is almost instant and never touches the disk. Maps still saved as zips by
 * {@link ZipWorldAdapter} get converted to slime the first time they're used.
 *
 * <p>Only build this when SlimeWorldManager is installed, it loads its classes.</p>
 */
public class SlimeWorldAdapter implements WorldAdapter {

    private final JavaPlugin plugin;
    private final SlimePlugin slime;
    private final SlimeLoader loader;
    private final ZipWorldAdapter fallback;
    private final File templates;

    /**
     * @param plugin plugin
     * @param loaderName SlimeWorldManager loader (file, mysql or mongodb)
     * @param fallback zip adapter used for imports and old templates
     */
    public SlimeWorldAdapter(JavaPlugin plugin, String loaderName, ZipWorldAdapter fallback) {
        this.plugin = plugin;
        this.slime = (SlimePlugin) Bukkit.getPluginManager().getPlugin("SlimeWorldManager");
        this.loader = this.slime.getLoader(loaderName);
        this.fallback = fallback;
        this.templates = new File(plugin.getDataFolder(), "templates");

        if (this.loader == null) {
            throw new IllegalArgumentException("SlimeWorldManager has no loader called " + loaderName);
        }
    }

    private SlimePropertyMap properties() {
        final SlimePropertyMap properties = new SlimePropertyMap();

        properties.setString(SlimeProperties.DIFFICULTY, "normal");
        properties.setBoolean(SlimeProperties.ALLOW_ANIMALS, false);
        properties.setBoolean(SlimeProperties.ALLOW_MONSTERS, false);
        properties.setBoolean(SlimeProperties.PVP, true);
        properties.setInt(SlimeProperties.SPAWN_Y, 100);
        return properties;
    }

    private boolean inLoader(String mapName) {
        try {
            return this.loader.worldExists(mapName);
        } catch (IOException e) {
            return false;
        }
    }

    private void migrate(String mapName) throws Exception {
        if (this.inLoader(mapName)) {
            return;
        }

        final File zip = new File(this.templates, mapName + ".zip");
        final File folder = new File(Bukkit.getWorldContainer(), mapName);
        final File temp = new File(this.plugin.getDataFolder(), "slime-import/" + mapName);

        WorldFiles.delete(temp);

        if (zip.exists()) {
            WorldFiles.unzip(zip, temp);
        } else if (new File(folder, "region").exists()) {
            WorldFiles.copy(folder, temp);
        } else {
            throw new IOException("There's no template for " + mapName);
        }

        try {
            this.slime.importWorld(temp, mapName, this.loader);
            this.plugin.getLogger().info("Map " + mapName + " converted to slime");
        } finally {
            WorldFiles.delete(temp);
        }
    }

    @Override
    public void loadArenaWorld(String mapName, String worldName, Consumer<World> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
            final SlimeWorld copy;

            try {
                this.migrate(mapName);
                copy = this.slime.loadWorld(this.loader, mapName, true, this.properties()).clone(worldName);
            } catch (Exception e) {
                this.plugin.getLogger().log(Level.SEVERE, "Couldn't load map " + mapName + " from SlimeWorldManager", e);
                Bukkit.getScheduler().runTask(this.plugin, () -> callback.accept(null));
                return;
            }

            Bukkit.getScheduler().runTask(this.plugin, () -> {
                this.slime.generateWorld(copy);

                final World world = Bukkit.getWorld(worldName);

                if (world != null) {
                    this.fallback.getSettings().apply(world, false);
                }

                callback.accept(world);
            });
        });
    }

    @Override
    public void unloadArenaWorld(String worldName) {
        final World world = Bukkit.getWorld(worldName);

        if (world != null && !Bukkit.unloadWorld(world, false)) {
            this.plugin.getLogger().warning("Couldn't unload world " + worldName);
        }
    }

    @Override
    public String getSetupWorldName(String mapName) {
        return mapName;
    }

    @Override
    public void loadSetupWorld(String mapName, Consumer<World> callback) {
        final World loaded = Bukkit.getWorld(mapName);

        if (loaded != null) {
            callback.accept(loaded);
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
            SlimeWorld world;

            try {
                if (this.fallback.exists(mapName)) {
                    this.migrate(mapName);
                }

                world = this.inLoader(mapName)
                        ? this.slime.loadWorld(this.loader, mapName, false, this.properties())
                        : this.slime.createEmptyWorld(this.loader, mapName, false, this.properties());
            } catch (Exception e) {
                this.plugin.getLogger().log(Level.SEVERE, "Couldn't open map " + mapName, e);
                world = null;
            }

            final SlimeWorld result = world;

            Bukkit.getScheduler().runTask(this.plugin, () -> {
                if (result == null) {
                    callback.accept(null);
                    return;
                }

                this.slime.generateWorld(result);

                final World bukkit = Bukkit.getWorld(mapName);

                if (bukkit != null) {
                    this.fallback.getSettings().apply(bukkit, true);
                }

                callback.accept(bukkit);
            });
        });
    }

    @Override
    public void saveSetupWorld(String mapName, Runnable callback) {
        final World world = Bukkit.getWorld(mapName);

        if (world != null) {
            world.save();

            if (!Bukkit.unloadWorld(world, true)) {
                this.plugin.getLogger().warning("Couldn't unload map " + mapName + " before saving it");
            }
        }

        Bukkit.getScheduler().runTaskLater(this.plugin, callback, 40L);
    }

    @Override
    public void importWorld(String worldName, String mapName, Runnable callback) {
        final Runnable restore = LiveWorlds.release(this.plugin, worldName);

        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
            final File temp = new File(this.plugin.getDataFolder(), "slime-import/" + mapName);

            try {
                if (this.inLoader(mapName)) {
                    this.loader.deleteWorld(mapName);
                }

                WorldFiles.delete(temp);
                WorldFiles.copy(new File(Bukkit.getWorldContainer(), worldName), temp);

                this.slime.importWorld(temp, mapName, this.loader);
            } catch (Exception e) {
                this.plugin.getLogger().log(Level.SEVERE, "Couldn't import world " + worldName, e);
            } finally {
                try {
                    WorldFiles.delete(temp);
                } catch (IOException ignored) {
                }
            }

            Bukkit.getScheduler().runTask(this.plugin, () -> {
                restore.run();
                callback.run();
            });
        });
    }

    @Override
    public boolean exists(String mapName) {
        return this.inLoader(mapName) || this.fallback.exists(mapName);
    }

    @Override
    public void deleteMap(String mapName) {
        final World world = Bukkit.getWorld(mapName);

        if (world != null) {
            Bukkit.unloadWorld(world, false);
        }

        try {
            if (this.inLoader(mapName)) {
                this.loader.deleteWorld(mapName);
            }
        } catch (Exception e) {
            this.plugin.getLogger().log(Level.WARNING, "Couldn't delete " + mapName + " from SlimeWorldManager", e);
        }

        this.fallback.deleteMap(mapName);
    }

    @Override
    public List<String> getImportableWorlds() {
        return this.fallback.getImportableWorlds();
    }

    @Override
    public void cleanTemporaryWorlds() {
        this.fallback.cleanTemporaryWorlds();
    }

    @Override
    public String getName() {
        return "SlimeWorldManager";
    }

}
