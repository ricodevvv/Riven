package dev.ricodevvv.riven.data.storage;

import com.google.gson.JsonObject;

import java.util.Map;

/**
 * Stores JSON documents by collection and id. That's the "efficient JSON storage": one row (or file) per
 * player with everything inside, instead of a column per stat. Every method blocks, so call them off the
 * main thread.
 */
public interface DocumentStore {

    /**
     * @param collection collection, like "players"
     * @param id document id
     * @return the document, or null if it doesn't exist
     */
    JsonObject load(String collection, String id);

    /**
     * @param collection collection
     * @param id document id
     * @param document document
     */
    void save(String collection, String id, JsonObject document);

    void delete(String collection, String id);

    /**
     * Reads a whole collection. Expensive on big collections, meant for rebuilds and migrations.
     *
     * @param collection collection
     * @return id to document
     */
    Map<String, JsonObject> loadAll(String collection);

    /**
     * @return name for logs
     */
    String getName();

}
