package dev.ricodevvv.riven.network;

import com.google.gson.JsonObject;
import dev.ricodevvv.riven.network.redis.RedisConnector;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.logging.Level;

/**
 * Typed messages between servers over one Redis channel. Handlers run on the main thread unless registered
 * with {@link #onAsync(String, Consumer)}. Requests get answered with {@link #reply(NetworkMessage, JsonObject)}
 * and time out on their own.
 *
 * <pre>
 * messenger.on("party:chat", message -&gt; ...);
 * messenger.send("party:chat", payload, null);
 * messenger.request("games:list", new JsonObject(), "game-1", 2000).thenAccept(answer -&gt; ...);
 * </pre>
 */
public class NetworkMessenger {

    private static final String CHANNEL = "messages";

    private final JavaPlugin plugin;
    private final RedisConnector redis;
    private final String serverId;
    private final Map<String, List<Consumer<NetworkMessage>>> handlers = new ConcurrentHashMap<>();
    private final Map<String, List<Consumer<NetworkMessage>>> asyncHandlers = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<JsonObject>> requests = new ConcurrentHashMap<>();
    private final ScheduledExecutorService timeouts = Executors.newSingleThreadScheduledExecutor(runnable -> {
        final Thread thread = new Thread(runnable, "riven-request-timeouts");

        thread.setDaemon(true);
        return thread;
    });

    public NetworkMessenger(JavaPlugin plugin, RedisConnector redis, String serverId) {
        this.plugin = plugin;
        this.redis = redis;
        this.serverId = serverId;
    }

    /**
     * Hooks into the connector. Call it once, after connecting.
     */
    public void start() {
        this.redis.subscribe((channel, text) -> {
            if (!channel.equals(CHANNEL)) {
                return;
            }

            final NetworkMessage message = NetworkMessage.deserialize(text);

            if (message == null || (message.getTarget() != null && !message.getTarget().equals(this.serverId))) {
                return;
            }

            if (message.getReplyTo() != null) {
                final CompletableFuture<JsonObject> future = this.requests.remove(message.getReplyTo());

                if (future != null) {
                    future.complete(message.getPayload());
                }
                return;
            }

            this.dispatch(message);
        });
    }

    public void stop() {
        this.timeouts.shutdownNow();

        for (CompletableFuture<JsonObject> future : this.requests.values()) {
            future.completeExceptionally(new IllegalStateException("Shutting down"));
        }

        this.requests.clear();
    }

    private void dispatch(NetworkMessage message) {
        for (Consumer<NetworkMessage> handler : this.asyncHandlers.getOrDefault(message.getType(), new CopyOnWriteArrayList<>())) {
            this.safe(handler, message);
        }

        final List<Consumer<NetworkMessage>> sync = this.handlers.get(message.getType());

        if (sync != null && !sync.isEmpty() && this.plugin.isEnabled()) {
            Bukkit.getScheduler().runTask(this.plugin, () -> {
                for (Consumer<NetworkMessage> handler : sync) {
                    this.safe(handler, message);
                }
            });
        }
    }

    private void safe(Consumer<NetworkMessage> handler, NetworkMessage message) {
        try {
            handler.accept(message);
        } catch (Exception e) {
            this.plugin.getLogger().log(Level.SEVERE, "Error handling network message " + message.getType(), e);
        }
    }

    /**
     * @param type message type
     * @param handler runs on the main thread
     */
    public void on(String type, Consumer<NetworkMessage> handler) {
        this.handlers.computeIfAbsent(type, key -> new CopyOnWriteArrayList<>()).add(handler);
    }

    /**
     * @param type message type
     * @param handler runs on the Redis thread, keep it short and thread safe
     */
    public void onAsync(String type, Consumer<NetworkMessage> handler) {
        this.asyncHandlers.computeIfAbsent(type, key -> new CopyOnWriteArrayList<>()).add(handler);
    }

    /**
     * Sends a message.
     *
     * @param type message type
     * @param payload payload
     * @param target server id, null for every server (this one included)
     */
    public void send(String type, JsonObject payload, String target) {
        this.redis.publish(CHANNEL, new NetworkMessage(type, this.serverId, target, null, payload).serialize());
    }

    /**
     * Sends a message and waits for one answer.
     *
     * @param type message type
     * @param payload payload
     * @param target server id that answers
     * @param timeoutMillis how long to wait
     * @return the answer payload, or a future failed with {@link TimeoutException}
     */
    public CompletableFuture<JsonObject> request(String type, JsonObject payload, String target, long timeoutMillis) {
        final NetworkMessage message = new NetworkMessage(type, this.serverId, target, null, payload);
        final CompletableFuture<JsonObject> future = new CompletableFuture<>();

        this.requests.put(message.getId(), future);
        this.timeouts.schedule(() -> {
            final CompletableFuture<JsonObject> pending = this.requests.remove(message.getId());

            if (pending != null) {
                pending.completeExceptionally(new TimeoutException(type + " to " + target + " timed out"));
            }
        }, timeoutMillis, TimeUnit.MILLISECONDS);

        this.redis.publish(CHANNEL, message.serialize());
        return future;
    }

    /**
     * Answers a request.
     *
     * @param request received request
     * @param payload answer
     */
    public void reply(NetworkMessage request, JsonObject payload) {
        this.redis.publish(CHANNEL, new NetworkMessage(request.getType(), this.serverId, request.getSource(), request.getId(), payload).serialize());
    }

    public String getServerId() {
        return this.serverId;
    }

}
