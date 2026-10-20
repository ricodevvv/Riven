package dev.ricodevvv.riven.data.storage;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.ricodevvv.riven.data.sql.Database;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Documents in SQL: one table per collection with {@code id}, {@code data} (the JSON) and {@code updated}.
 * Tables get created the first time a collection is used.
 */
public class SqlDocumentStore implements DocumentStore {

    private final Database database;
    private final Set<String> created = ConcurrentHashMap.newKeySet();

    public SqlDocumentStore(Database database) {
        this.database = database;
    }

    private String table(String collection) {
        final String table = this.database.table(collection.replaceAll("[^a-zA-Z0-9_]", "_"));

        if (this.created.add(table)) {
            try {
                this.database.update("CREATE TABLE IF NOT EXISTS " + table + " (id VARCHAR(64) NOT NULL PRIMARY KEY, data "
                        + this.database.longTextType() + " NOT NULL, updated BIGINT NOT NULL)");
            } catch (SQLException e) {
                this.created.remove(table);
                throw new IllegalStateException("Couldn't create table " + table, e);
            }
        }
        return table;
    }

    @Override
    public JsonObject load(String collection, String id) {
        try {
            return this.database.query("SELECT data FROM " + this.table(collection) + " WHERE id = ?",
                    result -> result.next() ? new JsonParser().parse(result.getString(1)).getAsJsonObject() : null, id);
        } catch (SQLException e) {
            throw new IllegalStateException("Couldn't load " + collection + "/" + id, e);
        }
    }

    @Override
    public void save(String collection, String id, JsonObject document) {
        final String table = this.table(collection);
        final String sql = this.database.isSqlite()
                ? "INSERT OR REPLACE INTO " + table + " (id, data, updated) VALUES (?, ?, ?)"
                : "INSERT INTO " + table + " (id, data, updated) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE data = VALUES(data), updated = VALUES(updated)";

        try {
            this.database.update(sql, id, document.toString(), System.currentTimeMillis());
        } catch (SQLException e) {
            throw new IllegalStateException("Couldn't save " + collection + "/" + id, e);
        }
    }

    @Override
    public void delete(String collection, String id) {
        try {
            this.database.update("DELETE FROM " + this.table(collection) + " WHERE id = ?", id);
        } catch (SQLException e) {
            throw new IllegalStateException("Couldn't delete " + collection + "/" + id, e);
        }
    }

    @Override
    public Map<String, JsonObject> loadAll(String collection) {
        try {
            return this.database.query("SELECT id, data FROM " + this.table(collection), result -> {
                final Map<String, JsonObject> documents = new LinkedHashMap<>();
                final JsonParser parser = new JsonParser();

                while (result.next()) {
                    documents.put(result.getString(1), parser.parse(result.getString(2)).getAsJsonObject());
                }
                return documents;
            });
        } catch (SQLException e) {
            throw new IllegalStateException("Couldn't load " + collection, e);
        }
    }

    @Override
    public String getName() {
        return this.database.isSqlite() ? "SQLite" : "MySQL";
    }

    public Database getDatabase() {
        return this.database;
    }

}
