package dev.ricodevvv.riven.data.sql;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

/**
 * Connection pool (HikariCP) for MySQL, MariaDB or SQLite, plus a small thread pool so queries never run on
 * the main thread. SQLite uses a single connection because it can't write from many at once.
 *
 * <pre>
 * Database database = new Database(plugin, DatabaseConfig.from(config.getConfigurationSection("database")));
 * database.connect();
 * database.updateAsync("DELETE FROM " + database.table("games") + " WHERE ended &lt; ?", cutoff);
 * </pre>
 */
public class Database {

    private final JavaPlugin plugin;
    private final DatabaseConfig config;
    private final ExecutorService executor;
    private HikariDataSource dataSource;

    public Database(JavaPlugin plugin, DatabaseConfig config) {
        this.plugin = plugin;
        this.config = config;

        final AtomicInteger counter = new AtomicInteger();

        this.executor = Executors.newFixedThreadPool(config.getType() == DatabaseConfig.Type.SQLITE ? 1 : Math.max(2, config.getPoolSize() / 2), runnable -> {
            final Thread thread = new Thread(runnable, plugin.getName() + "-db-" + counter.incrementAndGet());

            thread.setDaemon(true);
            return thread;
        });
    }

    /**
     * Opens the pool. Throws if the database can't be reached, so the plugin can fall back or stop.
     */
    public void connect() {
        final HikariConfig hikari = new HikariConfig();

        hikari.setPoolName(this.plugin.getName() + "-pool");

        if (this.config.getType() == DatabaseConfig.Type.SQLITE) {
            final File file = new File(this.plugin.getDataFolder(), this.config.getFile());

            if (!file.getParentFile().exists() && !file.getParentFile().mkdirs()) {
                this.plugin.getLogger().warning("Couldn't create " + file.getParentFile());
            }

            hikari.setDriverClassName("org.sqlite.JDBC");
            hikari.setJdbcUrl("jdbc:sqlite:" + file.getAbsolutePath());
            hikari.setMaximumPoolSize(1);
            hikari.setConnectionTestQuery("SELECT 1");
        } else {
            hikari.setDriverClassName("org.mariadb.jdbc.Driver");
            hikari.setJdbcUrl("jdbc:mariadb://" + this.config.getHost() + ":" + this.config.getPort() + "/" + this.config.getDatabase()
                    + "?useUnicode=true&characterEncoding=utf8&autoReconnect=true");
            hikari.setUsername(this.config.getUsername());
            hikari.setPassword(this.config.getPassword());
            hikari.setMaximumPoolSize(this.config.getPoolSize());
            hikari.setMinimumIdle(Math.min(2, this.config.getPoolSize()));
            hikari.setMaxLifetime(TimeUnit.MINUTES.toMillis(25));
            hikari.setKeepaliveTime(TimeUnit.MINUTES.toMillis(5));
            hikari.addDataSourceProperty("cachePrepStmts", "true");
            hikari.addDataSourceProperty("prepStmtCacheSize", "250");
            hikari.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        }

        hikari.setConnectionTimeout(TimeUnit.SECONDS.toMillis(10));

        this.dataSource = new HikariDataSource(hikari);
    }

    /**
     * Waits for pending queries (up to 10 seconds) and closes everything.
     */
    public void close() {
        this.executor.shutdown();

        try {
            if (!this.executor.awaitTermination(10, TimeUnit.SECONDS)) {
                this.plugin.getLogger().warning("Some database tasks didn't finish in time");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        if (this.dataSource != null) {
            this.dataSource.close();
        }
    }

    public Connection getConnection() throws SQLException {
        if (this.dataSource == null) {
            throw new SQLException("The database isn't connected");
        }
        return this.dataSource.getConnection();
    }

    /**
     * @param name table name without prefix
     * @return the name with the configured prefix
     */
    public String table(String name) {
        return this.config.getTablePrefix() + name;
    }

    public boolean isSqlite() {
        return this.config.getType() == DatabaseConfig.Type.SQLITE;
    }

    /**
     * @return {@code TEXT} on SQLite, {@code LONGTEXT} on MySQL
     */
    public String longTextType() {
        return this.isSqlite() ? "TEXT" : "LONGTEXT";
    }

    /**
     * Runs an insert, update or delete on the current thread.
     *
     * @param sql statement with ? placeholders
     * @param params values
     * @return affected rows
     * @throws SQLException if it fails
     */
    public int update(String sql, Object... params) throws SQLException {
        try (Connection connection = this.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, params);
            return statement.executeUpdate();
        }
    }

    /**
     * Runs a query on the current thread.
     *
     * @param sql query with ? placeholders
     * @param mapper reads the result set
     * @param params values
     * @param <T> result type
     * @return the mapped result
     * @throws SQLException if it fails
     */
    public <T> T query(String sql, SqlFunction<ResultSet, T> mapper, Object... params) throws SQLException {
        try (Connection connection = this.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, params);

            try (ResultSet result = statement.executeQuery()) {
                return mapper.apply(result);
            }
        }
    }

    /**
     * Same as {@link #update(String, Object...)} on the database thread. Errors get logged.
     *
     * @param sql statement
     * @param params values
     * @return future with the affected rows, -1 on error
     */
    public CompletableFuture<Integer> updateAsync(String sql, Object... params) {
        return this.supply(() -> {
            try {
                return this.update(sql, params);
            } catch (SQLException e) {
                this.plugin.getLogger().log(Level.SEVERE, "Query failed: " + sql, e);
                return -1;
            }
        });
    }

    /**
     * Same as {@link #query(String, SqlFunction, Object...)} on the database thread. Errors complete the
     * future exceptionally.
     *
     * @param sql query
     * @param mapper reads the result set
     * @param params values
     * @param <T> result type
     * @return the future
     */
    public <T> CompletableFuture<T> queryAsync(String sql, SqlFunction<ResultSet, T> mapper, Object... params) {
        final CompletableFuture<T> future = new CompletableFuture<>();

        this.executor.execute(() -> {
            try {
                future.complete(this.query(sql, mapper, params));
            } catch (Throwable throwable) {
                future.completeExceptionally(throwable);
            }
        });
        return future;
    }

    /**
     * Runs any work on the database thread.
     *
     * @param supplier work
     * @param <T> result type
     * @return the future
     */
    public <T> CompletableFuture<T> supply(java.util.function.Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(supplier, this.executor);
    }

    public void execute(Runnable runnable) {
        this.executor.execute(() -> {
            try {
                runnable.run();
            } catch (Throwable throwable) {
                this.plugin.getLogger().log(Level.SEVERE, "Database task failed", throwable);
            }
        });
    }

    private static void bind(PreparedStatement statement, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            statement.setObject(i + 1, params[i]);
        }
    }

    public DatabaseConfig getConfig() {
        return this.config;
    }

    public JavaPlugin getPlugin() {
        return this.plugin;
    }

}
