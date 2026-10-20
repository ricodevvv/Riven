package dev.ricodevvv.riven.network.redis;

import org.bukkit.plugin.java.JavaPlugin;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.logging.Level;

/**
 * Jedis pool plus a pub/sub listener thread that reconnects by itself with exponential backoff (1s up to 30s)
 * when Redis goes away. Publishing happens on a small thread pool so it never blocks the main thread.
 */
public class RedisConnector {

    private static final long MAX_BACKOFF = 30_000L;

    private final JavaPlugin plugin;
    private final RedisConfig config;
    private final ExecutorService publisher;
    private JedisPool pool;
    private Thread subscriberThread;
    private volatile JedisPubSub subscriber;
    private volatile boolean running;
    private volatile boolean connected;

    public RedisConnector(JavaPlugin plugin, RedisConfig config) {
        this.plugin = plugin;
        this.config = config;
        this.publisher = Executors.newFixedThreadPool(2, runnable -> {
            final Thread thread = new Thread(runnable, plugin.getName() + "-redis-publisher");

            thread.setDaemon(true);
            return thread;
        });
    }

    /**
     * Opens the pool and checks Redis answers. Throws if it doesn't.
     */
    public void connect() {
        final JedisPoolConfig poolConfig = new JedisPoolConfig();

        poolConfig.setMaxTotal(this.config.getPoolSize());
        poolConfig.setMaxIdle(this.config.getPoolSize());
        poolConfig.setMinIdle(1);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestWhileIdle(true);

        this.pool = new JedisPool(poolConfig, this.config.getHost(), this.config.getPort(), this.config.getTimeout(), this.config.getPassword(), this.config.getDatabase());

        try (Jedis jedis = this.pool.getResource()) {
            jedis.ping();
        }

        this.running = true;
        this.connected = true;
    }

    /**
     * Starts listening to every channel matching {@code <namespace>:*}. The handler gets (channel without
     * namespace, message) on the subscriber thread.
     *
     * @param handler message handler
     */
    public void subscribe(BiConsumer<String, String> handler) {
        final String prefix = this.config.getNamespace() + ":";

        this.subscriberThread = new Thread(() -> {
            long backoff = 1000L;

            while (this.running) {
                try (Jedis jedis = this.pool.getResource()) {
                    this.subscriber = new JedisPubSub() {
                        @Override
                        public void onPMessage(String pattern, String channel, String message) {
                            try {
                                handler.accept(channel.substring(prefix.length()), message);
                            } catch (Exception e) {
                                plugin.getLogger().log(Level.SEVERE, "Error handling Redis message on " + channel, e);
                            }
                        }
                    };

                    if (!this.connected) {
                        this.plugin.getLogger().info("Reconnected to Redis");
                    }

                    this.connected = true;
                    backoff = 1000L;
                    jedis.psubscribe(this.subscriber, prefix + "*");
                } catch (Exception e) {
                    if (!this.running) {
                        break;
                    }

                    if (this.connected) {
                        this.plugin.getLogger().log(Level.WARNING, "Lost the Redis connection, retrying in " + backoff / 1000 + "s", e);
                    }

                    this.connected = false;

                    try {
                        Thread.sleep(backoff);
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        break;
                    }

                    backoff = Math.min(MAX_BACKOFF, backoff * 2);
                }
            }
        }, this.plugin.getName() + "-redis-subscriber");

        this.subscriberThread.setDaemon(true);
        this.subscriberThread.start();
    }

    /**
     * Publishes a message without blocking.
     *
     * @param channel channel without namespace
     * @param message message
     */
    public void publish(String channel, String message) {
        if (this.publisher.isShutdown()) {
            return;
        }

        this.publisher.execute(() -> this.publishSync(channel, message));
    }

    /**
     * Publishes on the current thread.
     *
     * @param channel channel without namespace
     * @param message message
     */
    public void publishSync(String channel, String message) {
        try (Jedis jedis = this.pool.getResource()) {
            jedis.publish(this.config.getNamespace() + ":" + channel, message);
        } catch (Exception e) {
            this.plugin.getLogger().log(Level.WARNING, "Couldn't publish on " + channel, e);
        }
    }

    /**
     * Runs something with a pooled connection on the current thread.
     *
     * @param action what to do
     * @param <T> result
     * @return the result
     */
    public <T> T with(Function<Jedis, T> action) {
        try (Jedis jedis = this.pool.getResource()) {
            return action.apply(jedis);
        }
    }

    /**
     * Runs something with a pooled connection without blocking. Errors get logged.
     *
     * @param action what to do
     */
    public void async(java.util.function.Consumer<Jedis> action) {
        if (this.publisher.isShutdown()) {
            return;
        }

        this.publisher.execute(() -> {
            try (Jedis jedis = this.pool.getResource()) {
                action.accept(jedis);
            } catch (Exception e) {
                this.plugin.getLogger().log(Level.WARNING, "Redis task failed", e);
            }
        });
    }

    /**
     * @param key key without namespace
     * @return the full key
     */
    public String key(String key) {
        return this.config.getNamespace() + ":" + key;
    }

    public void close() {
        this.running = false;

        if (this.subscriber != null) {
            try {
                this.subscriber.punsubscribe();
            } catch (Exception ignored) {
            }
        }

        this.publisher.shutdown();

        try {
            this.publisher.awaitTermination(3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        if (this.pool != null) {
            this.pool.close();
        }
    }

    public boolean isConnected() {
        return this.connected;
    }

    public RedisConfig getConfig() {
        return this.config;
    }

}
