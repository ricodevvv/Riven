package dev.ricodevvv.riven.data.leaderboard;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Leaderboards kept in memory and dumped to a JSON file, for servers without a database.
 */
public class MemoryLeaderboardStore implements LeaderboardStore {

    private final File file;
    private final Map<String, Map<UUID, Row>> boards = new ConcurrentHashMap<>();

    public MemoryLeaderboardStore(File file) {
        this.file = file;
        this.load();
    }

    private void load() {
        if (!this.file.exists()) {
            return;
        }

        try (Reader reader = Files.newBufferedReader(this.file.toPath(), StandardCharsets.UTF_8)) {
            final JsonObject root = new JsonParser().parse(reader).getAsJsonObject();

            for (Map.Entry<String, JsonElement> board : root.entrySet()) {
                final Map<UUID, Row> rows = new ConcurrentHashMap<>();

                for (Map.Entry<String, JsonElement> row : board.getValue().getAsJsonObject().entrySet()) {
                    final JsonObject value = row.getValue().getAsJsonObject();

                    rows.put(UUID.fromString(row.getKey()), new Row(value.get("name").getAsString(), value.get("value").getAsLong()));
                }

                this.boards.put(board.getKey(), rows);
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * Writes everything to the file. Call it every now and then and on disable.
     */
    public synchronized void flush() {
        final JsonObject root = new JsonObject();

        for (Map.Entry<String, Map<UUID, Row>> board : this.boards.entrySet()) {
            final JsonObject rows = new JsonObject();

            for (Map.Entry<UUID, Row> row : board.getValue().entrySet()) {
                final JsonObject value = new JsonObject();

                value.addProperty("name", row.getValue().name);
                value.addProperty("value", row.getValue().value);
                rows.add(row.getKey().toString(), value);
            }

            root.add(board.getKey(), rows);
        }

        if (this.file.getParentFile() != null && !this.file.getParentFile().exists() && !this.file.getParentFile().mkdirs()) {
            return;
        }

        try (Writer writer = Files.newBufferedWriter(this.file.toPath(), StandardCharsets.UTF_8)) {
            writer.write(root.toString());
        } catch (IOException e) {
            throw new IllegalStateException("Couldn't write " + this.file, e);
        }
    }

    @Override
    public void submit(String board, UUID uniqueId, String name, long value) {
        this.boards.computeIfAbsent(board, key -> new ConcurrentHashMap<>()).put(uniqueId, new Row(name, value));
    }

    @Override
    public List<LeaderboardEntry> top(String board, int limit) {
        final List<Map.Entry<UUID, Row>> rows = new ArrayList<>(this.boards.getOrDefault(board, new ConcurrentHashMap<>()).entrySet());

        rows.sort((first, second) -> Long.compare(second.getValue().value, first.getValue().value));

        final List<LeaderboardEntry> entries = new ArrayList<>();

        for (int i = 0; i < rows.size() && i < limit; i++) {
            entries.add(new LeaderboardEntry(i + 1, rows.get(i).getKey(), rows.get(i).getValue().name, rows.get(i).getValue().value));
        }
        return entries;
    }

    @Override
    public int position(String board, UUID uniqueId) {
        final Map<UUID, Row> rows = this.boards.get(board);
        final Row own = rows == null ? null : rows.get(uniqueId);

        if (own == null) {
            return -1;
        }

        int position = 1;

        for (Row row : rows.values()) {
            if (row.value > own.value) {
                position++;
            }
        }
        return position;
    }

    @Override
    public void clear(String board) {
        this.boards.remove(board);
    }

    private static final class Row {

        private final String name;
        private final long value;

        Row(String name, long value) {
            this.name = name;
            this.value = value;
        }

    }

}
