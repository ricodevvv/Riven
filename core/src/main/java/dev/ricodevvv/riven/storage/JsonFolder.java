package dev.ricodevvv.riven.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.logging.Level;

/**
 * A folder of JSON files, one object per file ({@code <folder>/<id>.json}). Good for maps, kits or anything
 * edited from the game that doesn't need a database. Writes go to a temp file first, so a crash never leaves
 * half a file behind.
 *
 * @param <T> stored type, it needs a no-args constructor (private is fine)
 */
public class JsonFolder<T> {

    private static final Gson DEFAULT_GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private final JavaPlugin plugin;
    private final File folder;
    private final Class<T> type;
    private final Function<T, String> idFunction;
    private final Gson gson;
    private final Map<String, T> values = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    public JsonFolder(JavaPlugin plugin, String folder, Class<T> type, Function<T, String> idFunction) {
        this(plugin, folder, type, idFunction, DEFAULT_GSON);
    }

    /**
     * @param plugin owner plugin
     * @param folder folder inside the plugin data folder
     * @param type stored class
     * @param idFunction gets the id (file name) of an object
     * @param gson gson to use, for custom adapters
     */
    public JsonFolder(JavaPlugin plugin, String folder, Class<T> type, Function<T, String> idFunction, Gson gson) {
        this.plugin = plugin;
        this.folder = new File(plugin.getDataFolder(), folder);
        this.type = type;
        this.idFunction = idFunction;
        this.gson = gson;
    }

    /**
     * Reads every file from disk, dropping whatever was loaded before.
     */
    public void load() {
        this.values.clear();

        if (!this.folder.exists() && !this.folder.mkdirs()) {
            this.plugin.getLogger().warning("Couldn't create " + this.folder);
            return;
        }

        final File[] files = this.folder.listFiles((dir, name) -> name.endsWith(".json"));

        if (files == null) {
            return;
        }

        for (File file : files) {
            try (Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
                final T value = this.gson.fromJson(reader, this.type);

                if (value != null && this.idFunction.apply(value) != null) {
                    this.values.put(this.idFunction.apply(value), value);
                }
            } catch (Exception e) {
                this.plugin.getLogger().log(Level.SEVERE, "Couldn't read " + file.getName(), e);
            }
        }
    }

    /**
     * Saves an object to disk and keeps it in memory.
     *
     * @param value object
     */
    public void save(T value) {
        final String id = this.idFunction.apply(value);

        this.values.put(id, value);

        if (!this.folder.exists() && !this.folder.mkdirs()) {
            return;
        }

        final File file = new File(this.folder, id + ".json");
        final File temp = new File(this.folder, id + ".json.tmp");

        try (Writer writer = Files.newBufferedWriter(temp.toPath(), StandardCharsets.UTF_8)) {
            this.gson.toJson(value, writer);
        } catch (IOException e) {
            this.plugin.getLogger().log(Level.SEVERE, "Couldn't save " + file.getName(), e);
            return;
        }

        try {
            Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            this.plugin.getLogger().log(Level.SEVERE, "Couldn't save " + file.getName(), e);
        }
    }

    /**
     * Deletes an object from memory and disk.
     *
     * @param id object id
     */
    public void delete(String id) {
        this.values.remove(id);

        final File file = new File(this.folder, id + ".json");

        if (file.exists() && !file.delete()) {
            this.plugin.getLogger().warning("Couldn't delete " + file.getName());
        }
    }

    public T get(String id) {
        return this.values.get(id);
    }

    public boolean contains(String id) {
        return this.values.containsKey(id);
    }

    public Collection<T> values() {
        return new ArrayList<>(this.values.values());
    }

    public File getFolder() {
        return this.folder;
    }

}
