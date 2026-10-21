package dev.ricodevvv.riven.game.world;

import org.bukkit.World;

import java.util.List;
import java.util.function.Consumer;

/**
 * Handles map worlds: hands every game a clean copy of the map and saves the maps being edited.
 * It's BedWars2023's RestoreAdapter. {@link ZipWorldAdapter} keeps every map as a zip, {@link SlimeWorldAdapter}
 * uses SlimeWorldManager, and you can write your own.
 */
public interface WorldAdapter {

    /**
     * Loads a clean copy of a map for a game. The callback runs on the main thread.
     *
     * @param mapName map (template) name
     * @param worldName name of the world to create
     * @param callback gets the loaded world, or null if it failed
     */
    void loadArenaWorld(String mapName, String worldName, Consumer<World> callback);

    /**
     * Unloads a game world without saving and deletes it from disk.
     *
     * @param worldName world to unload
     */
    void unloadArenaWorld(String worldName);

    /**
     * The world admins edit a map in. It's never the world the map was imported from, so editing or deleting a
     * map can't touch the original.
     *
     * @param mapName map name
     * @return world name of the edit copy
     */
    String getSetupWorldName(String mapName);

    /**
     * Opens the edit copy of a map (fresh from its template). Creates an empty one if there is no template.
     *
     * @param mapName map name
     * @param callback gets the world, on the main thread
     */
    void loadSetupWorld(String mapName, Consumer<World> callback);

    /**
     * Saves the edited map as the new template.
     *
     * @param mapName map name
     * @param callback runs when it's done, on the main thread
     */
    void saveSetupWorld(String mapName, Runnable callback);

    /**
     * Takes a world already on the server and saves it as a map template. A loaded world gets unloaded for a
     * moment so the copy isn't broken, players inside wait in the main world and come back.
     *
     * @param worldName source world
     * @param mapName map name
     * @param callback runs when it's done, on the main thread
     */
    void importWorld(String worldName, String mapName, Runnable callback);

    /**
     * @param mapName map name
     * @return true if there's a template or a folder for that map
     */
    boolean exists(String mapName);

    /**
     * Deletes a map's template and its edit copy. The world it was imported from stays.
     *
     * @param mapName map name
     */
    void deleteMap(String mapName);

    /**
     * @return worlds in the server folder that could be imported as maps
     */
    List<String> getImportableWorlds();

    /**
     * Cleans up temporary worlds left behind by a hard shutdown.
     */
    void cleanTemporaryWorlds();

    /**
     * @return name used in logs
     */
    String getName();

}
