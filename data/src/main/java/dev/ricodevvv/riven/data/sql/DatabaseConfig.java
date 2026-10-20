package dev.ricodevvv.riven.data.sql;

import org.bukkit.configuration.ConfigurationSection;

/**
 * Connection settings, usually read from a config section:
 *
 * <pre>
 * type: SQLITE        # SQLITE, MYSQL or MARIADB
 * host: localhost
 * port: 3306
 * database: minigames
 * username: root
 * password: ""
 * pool-size: 10
 * table-prefix: "sw_"
 * </pre>
 */
public class DatabaseConfig {

    private Type type = Type.SQLITE;
    private String host = "localhost";
    private int port = 3306;
    private String database = "minigames";
    private String username = "root";
    private String password = "";
    private int poolSize = 10;
    private String tablePrefix = "";
    private String file = "database.db";

    /**
     * @param section config section, null gives the defaults (SQLite)
     * @return the settings
     */
    public static DatabaseConfig from(ConfigurationSection section) {
        final DatabaseConfig config = new DatabaseConfig();

        if (section == null) {
            return config;
        }

        try {
            config.type = Type.valueOf(section.getString("type", "SQLITE").toUpperCase());
        } catch (IllegalArgumentException e) {
            config.type = Type.SQLITE;
        }

        config.host = section.getString("host", config.host);
        config.port = section.getInt("port", config.port);
        config.database = section.getString("database", config.database);
        config.username = section.getString("username", config.username);
        config.password = section.getString("password", config.password);
        config.poolSize = section.getInt("pool-size", config.poolSize);
        config.tablePrefix = section.getString("table-prefix", config.tablePrefix);
        config.file = section.getString("file", config.file);
        return config;
    }

    public DatabaseConfig type(Type type) {
        this.type = type;
        return this;
    }

    public DatabaseConfig host(String host) {
        this.host = host;
        return this;
    }

    public DatabaseConfig port(int port) {
        this.port = port;
        return this;
    }

    public DatabaseConfig database(String database) {
        this.database = database;
        return this;
    }

    public DatabaseConfig credentials(String username, String password) {
        this.username = username;
        this.password = password;
        return this;
    }

    public DatabaseConfig poolSize(int poolSize) {
        this.poolSize = poolSize;
        return this;
    }

    public DatabaseConfig tablePrefix(String tablePrefix) {
        this.tablePrefix = tablePrefix;
        return this;
    }

    public DatabaseConfig file(String file) {
        this.file = file;
        return this;
    }

    public Type getType() {
        return this.type;
    }

    public String getHost() {
        return this.host;
    }

    public int getPort() {
        return this.port;
    }

    public String getDatabase() {
        return this.database;
    }

    public String getUsername() {
        return this.username;
    }

    public String getPassword() {
        return this.password;
    }

    public int getPoolSize() {
        return this.poolSize;
    }

    public String getTablePrefix() {
        return this.tablePrefix;
    }

    public String getFile() {
        return this.file;
    }

    /**
     * Supported databases.
     */
    public enum Type {
        SQLITE,
        MYSQL,
        MARIADB
    }

}
