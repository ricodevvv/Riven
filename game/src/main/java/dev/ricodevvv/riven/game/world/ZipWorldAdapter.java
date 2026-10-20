package dev.ricodevvv.riven.game.world;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;

/**
 * Default world adapter, based on BedWars2023's InternalAdapter: every map is a zip in {@code templates/} and every
 * game unzips its own copy into a temporary world ({@code <prefix><map>_<n>}) that gets deleted afterwards.
 */
public class ZipWorldAdapter implements WorldAdapter {

    private final JavaPlugin plugin;
    private final File templates;
    private final String tempPrefix;
    private WorldSettings settings = new WorldSettings();

    /**
     * @param plugin owner plugin
     * @param tempPrefix prefix of the temporary game worlds, like "sw_"
     */
    public ZipWorldAdapter(JavaPlugin plugin, String tempPrefix) {
        this.plugin = plugin;
        this.tempPrefix = tempPrefix;
        this.templates = new File(plugin.getDataFolder(), "templates");

        if (!this.templates.exists() && !this.templates.mkdirs()) {
            plugin.getLogger().warning("Couldn't create the templates folder");
        }
    }

    public ZipWorldAdapter settings(WorldSettings settings) {
        this.settings = settings;
        return this;
    }

    public WorldSettings getSettings() {
        return this.settings;
    }

    public String getTempPrefix() {
        return this.tempPrefix;
    }

    /**
     * @param mapName map name
     * @return the template zip of a map
     */
    public File template(String mapName) {
        return new File(this.templates, mapName + ".zip");
    }

    private File worldFolder(String worldName) {
        return new File(Bukkit.getWorldContainer(), worldName);
    }

    @Override
    public void loadArenaWorld(String mapName, String worldName, Consumer<World> callback) {
        final World loadedTemplate = Bukkit.getWorld(mapName);

        if (loadedTemplate != null && !this.template(mapName).exists()) {
            loadedTemplate.save();
        }

        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
            try {
                if (!this.template(mapName).exists()) {
                    if (!new File(this.worldFolder(mapName), "region").exists()) {
                        throw new IOException("There's no template or folder for map " + mapName);
                    }

                    WorldFiles.zip(this.worldFolder(mapName), this.template(mapName));
                }

                final File target = this.worldFolder(worldName);

                WorldFiles.delete(target);
                WorldFiles.unzip(this.template(mapName), target);
                this.deleteTrash(worldName);
            } catch (IOException e) {
                this.plugin.getLogger().log(Level.SEVERE, "Couldn't prepare world " + worldName, e);
                Bukkit.getScheduler().runTask(this.plugin, () -> callback.accept(null));
                return;
            }

            Bukkit.getScheduler().runTask(this.plugin, () -> callback.accept(this.createWorld(worldName, false)));
        });
    }

    private World createWorld(String worldName, boolean autoSave) {
        final World world = Bukkit.createWorld(new WorldCreator(worldName).generator(new VoidChunkGenerator()).generateStructures(false));

        if (world != null) {
            this.settings.apply(world, autoSave);
        }
        return world;
    }

    @Override
    public void unloadArenaWorld(String worldName) {
        final World world = Bukkit.getWorld(worldName);

        if (world != null && !Bukkit.unloadWorld(world, false)) {
            this.plugin.getLogger().warning("Couldn't unload world " + worldName);
        }

        if (!this.plugin.isEnabled()) {
            this.deleteFolder(worldName);
            return;
        }

        Bukkit.getScheduler().runTaskLaterAsynchronously(this.plugin, () -> this.deleteFolder(worldName), 20L);
    }

    private void deleteFolder(String worldName) {
        try {
            WorldFiles.delete(this.worldFolder(worldName));
        } catch (IOException e) {
            this.plugin.getLogger().log(Level.WARNING, "Couldn't delete world " + worldName, e);
        }
    }

    @Override
    public void loadSetupWorld(String mapName, Consumer<World> callback) {
        final World loaded = Bukkit.getWorld(mapName);

        if (loaded != null) {
            callback.accept(loaded);
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
            try {
                if (this.template(mapName).exists()) {
                    WorldFiles.delete(this.worldFolder(mapName));
                    WorldFiles.unzip(this.template(mapName), this.worldFolder(mapName));
                }

                this.deleteTrash(mapName);
            } catch (IOException e) {
                this.plugin.getLogger().log(Level.SEVERE, "Couldn't open map " + mapName, e);
            }

            Bukkit.getScheduler().runTask(this.plugin, () -> callback.accept(this.createWorld(mapName, true)));
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

        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
            try {
                WorldFiles.zip(this.worldFolder(mapName), this.template(mapName));
            } catch (IOException e) {
                this.plugin.getLogger().log(Level.SEVERE, "Couldn't save the template of " + mapName, e);
            }

            Bukkit.getScheduler().runTask(this.plugin, callback);
        });
    }

    @Override
    public void importWorld(String worldName, String mapName, Runnable callback) {
        final World world = Bukkit.getWorld(worldName);

        if (world != null) {
            world.save();
        }

        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
            try {
                WorldFiles.zip(this.worldFolder(worldName), this.template(mapName));
            } catch (IOException e) {
                this.plugin.getLogger().log(Level.SEVERE, "Couldn't import world " + worldName, e);
            }

            Bukkit.getScheduler().runTask(this.plugin, callback);
        });
    }

    @Override
    public boolean exists(String mapName) {
        return this.template(mapName).exists() || new File(this.worldFolder(mapName), "region").exists();
    }

    @Override
    public void deleteMap(String mapName) {
        final World world = Bukkit.getWorld(mapName);

        if (world != null) {
            Bukkit.unloadWorld(world, false);
        }

        if (this.template(mapName).exists() && !this.template(mapName).delete()) {
            this.plugin.getLogger().warning("Couldn't delete the template of " + mapName);
        }

        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
            try {
                WorldFiles.delete(this.worldFolder(mapName));
            } catch (IOException e) {
                this.plugin.getLogger().log(Level.WARNING, "Couldn't delete the folder of " + mapName, e);
            }
        });
    }

    @Override
    public List<String> getImportableWorlds() {
        final List<String> worlds = new ArrayList<>();
        final File[] folders = Bukkit.getWorldContainer().listFiles(File::isDirectory);

        if (folders == null) {
            return worlds;
        }

        for (File folder : folders) {
            if (new File(folder, "level.dat").exists() && !folder.getName().startsWith(this.tempPrefix)) {
                worlds.add(folder.getName());
            }
        }
        return worlds;
    }

    @Override
    public void cleanTemporaryWorlds() {
        final File[] folders = Bukkit.getWorldContainer().listFiles(file -> file.isDirectory() && file.getName().startsWith(this.tempPrefix));

        if (folders == null) {
            return;
        }

        for (File folder : folders) {
            if (Bukkit.getWorld(folder.getName()) != null) {
                continue;
            }

            try {
                WorldFiles.delete(folder);
            } catch (IOException e) {
                this.plugin.getLogger().log(Level.WARNING, "Couldn't delete temporary world " + folder.getName(), e);
            }
        }
    }

    private void deleteTrash(String worldName) {
        for (String name : new String[] {"session.lock", "uid.dat"}) {
            final File file = new File(this.worldFolder(worldName), name);

            if (file.exists() && !file.delete()) {
                this.plugin.getLogger().warning("Couldn't delete " + file.getPath());
            }
        }
    }

    @Override
    public String getName() {
        return "Internal (zip)";
    }

}
