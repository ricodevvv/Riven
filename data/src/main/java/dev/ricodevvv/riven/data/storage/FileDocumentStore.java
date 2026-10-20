package dev.ricodevvv.riven.data.storage;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Documents as JSON files: {@code <folder>/<collection>/<id>.json}. No setup needed, fine for single servers.
 */
public class FileDocumentStore implements DocumentStore {

    private final File folder;

    public FileDocumentStore(File folder) {
        this.folder = folder;
    }

    private File file(String collection, String id) {
        return new File(new File(this.folder, collection), id + ".json");
    }

    @Override
    public JsonObject load(String collection, String id) {
        final File file = this.file(collection, id);

        if (!file.exists()) {
            return null;
        }

        try (Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            return new JsonParser().parse(reader).getAsJsonObject();
        } catch (Exception e) {
            throw new IllegalStateException("Couldn't read " + file, e);
        }
    }

    @Override
    public synchronized void save(String collection, String id, JsonObject document) {
        final File file = this.file(collection, id);
        final File temp = new File(file.getPath() + ".tmp");

        if (!file.getParentFile().exists() && !file.getParentFile().mkdirs()) {
            throw new IllegalStateException("Couldn't create " + file.getParentFile());
        }

        try {
            try (Writer writer = Files.newBufferedWriter(temp.toPath(), StandardCharsets.UTF_8)) {
                writer.write(document.toString());
            }

            Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("Couldn't write " + file, e);
        }
    }

    @Override
    public void delete(String collection, String id) {
        final File file = this.file(collection, id);

        if (file.exists() && !file.delete()) {
            throw new IllegalStateException("Couldn't delete " + file);
        }
    }

    @Override
    public Map<String, JsonObject> loadAll(String collection) {
        final Map<String, JsonObject> documents = new LinkedHashMap<>();
        final File[] files = new File(this.folder, collection).listFiles((dir, name) -> name.endsWith(".json"));

        if (files == null) {
            return documents;
        }

        for (File file : files) {
            final String id = file.getName().substring(0, file.getName().length() - 5);
            final JsonObject document = this.load(collection, id);

            if (document != null) {
                documents.put(id, document);
            }
        }
        return documents;
    }

    @Override
    public String getName() {
        return "Files";
    }

}
