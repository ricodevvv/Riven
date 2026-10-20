package dev.ricodevvv.riven.network.redis;

import org.bukkit.configuration.ConfigurationSection;

/**
 * Redis settings:
 *
 * <pre>
 * host: localhost
 * port: 6379
 * password: ""
 * database: 0
 * pool-size: 16
 * namespace: "skywars"   # prefix for every key and channel, lets several networks share one Redis
 * </pre>
 */
public class RedisConfig {

    private String host = "localhost";
    private int port = 6379;
    private String password = "";
    private int database;
    private int poolSize = 16;
    private int timeout = 3000;
    private String namespace = "riven";

    public static RedisConfig from(ConfigurationSection section) {
        final RedisConfig config = new RedisConfig();

        if (section == null) {
            return config;
        }

        config.host = section.getString("host", config.host);
        config.port = section.getInt("port", config.port);
        config.password = section.getString("password", config.password);
        config.database = section.getInt("database", config.database);
        config.poolSize = section.getInt("pool-size", config.poolSize);
        config.timeout = section.getInt("timeout", config.timeout);
        config.namespace = section.getString("namespace", config.namespace);
        return config;
    }

    public RedisConfig host(String host, int port) {
        this.host = host;
        this.port = port;
        return this;
    }

    public RedisConfig password(String password) {
        this.password = password;
        return this;
    }

    public RedisConfig namespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public String getHost() {
        return this.host;
    }

    public int getPort() {
        return this.port;
    }

    public String getPassword() {
        return this.password == null || this.password.isEmpty() ? null : this.password;
    }

    public int getDatabase() {
        return this.database;
    }

    public int getPoolSize() {
        return this.poolSize;
    }

    public int getTimeout() {
        return this.timeout;
    }

    public String getNamespace() {
        return this.namespace;
    }

}
