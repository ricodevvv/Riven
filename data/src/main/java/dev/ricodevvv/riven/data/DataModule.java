package dev.ricodevvv.riven.data;

import dev.ricodevvv.riven.Riven;
import dev.ricodevvv.riven.data.leaderboard.LeaderboardStore;
import dev.ricodevvv.riven.data.leaderboard.MemoryLeaderboardStore;
import dev.ricodevvv.riven.data.leaderboard.SqlLeaderboardStore;
import dev.ricodevvv.riven.data.sql.Database;
import dev.ricodevvv.riven.data.sql.DatabaseConfig;
import dev.ricodevvv.riven.data.storage.DocumentStore;
import dev.ricodevvv.riven.data.storage.FileDocumentStore;
import dev.ricodevvv.riven.data.storage.SqlDocumentStore;
import dev.ricodevvv.riven.module.RivenModule;

import java.io.File;
import java.util.logging.Level;

/**
 * The data layer as a Riven module. It connects to the database from the config and hands out a
 * {@link DocumentStore} and a {@link LeaderboardStore}. If the database can't be reached and
 * {@code fallbackToFiles} is on, it falls back to JSON files so the server still starts.
 *
 * <pre>
 * DataModule data = riven.install(new DataModule(DatabaseConfig.from(config.getConfigurationSection("database")), true));
 * ProfileManager&lt;MyProfile&gt; profiles = new ProfileManager&lt;&gt;(plugin, data.getStore(), "players", MyProfile.class, MyProfile::new);
 * </pre>
 */
public class DataModule implements RivenModule {

    private final DatabaseConfig config;
    private final boolean fallbackToFiles;

    private Database database;
    private DocumentStore store;
    private LeaderboardStore leaderboards;

    /**
     * @param config database settings
     * @param fallbackToFiles true to use JSON files if the database fails
     */
    public DataModule(DatabaseConfig config, boolean fallbackToFiles) {
        this.config = config;
        this.fallbackToFiles = fallbackToFiles;
    }

    @Override
    public void enable(Riven riven) {
        final File dataFolder = riven.getPlugin().getDataFolder();

        try {
            this.database = new Database(riven.getPlugin(), this.config);
            this.database.connect();
            this.store = new SqlDocumentStore(this.database);
            this.leaderboards = new SqlLeaderboardStore(this.database);

            riven.getPlugin().getLogger().info("Connected to " + this.config.getType() + " for player data");
        } catch (Exception e) {
            if (this.database != null) {
                this.database.close();
                this.database = null;
            }

            if (!this.fallbackToFiles) {
                throw new IllegalStateException("Couldn't connect to the database", e);
            }

            riven.getPlugin().getLogger().log(Level.SEVERE, "Couldn't connect to the database, using JSON files instead", e);

            this.store = new FileDocumentStore(new File(dataFolder, "data"));
            this.leaderboards = new MemoryLeaderboardStore(new File(dataFolder, "data/leaderboards.json"));
        }
    }

    @Override
    public void disable() {
        if (this.leaderboards instanceof MemoryLeaderboardStore) {
            ((MemoryLeaderboardStore) this.leaderboards).flush();
        }

        if (this.database != null) {
            this.database.close();
        }
    }

    /**
     * @return the SQL database, or null when running on files
     */
    public Database getDatabase() {
        return this.database;
    }

    public DocumentStore getStore() {
        return this.store;
    }

    public LeaderboardStore getLeaderboards() {
        return this.leaderboards;
    }

    public boolean isSql() {
        return this.database != null;
    }

}
