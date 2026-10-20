package dev.ricodevvv.riven.input;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/**
 * Grabs the next chat message of a player instead of sending it to chat. Typing "cancel" aborts it.
 * Good for setup wizards where a sign or an anvil is too small.
 */
public class ChatInputManager implements Listener {

    private static final String CANCEL_WORD = "cancel";

    private final JavaPlugin plugin;
    private final Map<UUID, Request> requests = new ConcurrentHashMap<>();

    public ChatInputManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        Bukkit.getPluginManager().registerEvents(this, this.plugin);
    }

    public void stop() {
        HandlerList.unregisterAll(this);
        this.requests.clear();
    }

    /**
     * Waits for the player's next chat message.
     *
     * @param player player
     * @param callback gets the text on the main thread
     * @param onCancel runs if they type "cancel" or leave, can be null
     */
    public void await(Player player, BiConsumer<Player, String> callback, Runnable onCancel) {
        this.requests.put(player.getUniqueId(), new Request(callback, onCancel));
    }

    public boolean isWaiting(Player player) {
        return this.requests.containsKey(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        final Request request = this.requests.remove(event.getPlayer().getUniqueId());

        if (request == null) {
            return;
        }

        event.setCancelled(true);

        final String message = event.getMessage().trim();

        Bukkit.getScheduler().runTask(this.plugin, () -> {
            if (message.equalsIgnoreCase(CANCEL_WORD)) {
                if (request.onCancel != null) {
                    request.onCancel.run();
                }
                return;
            }
            request.callback.accept(event.getPlayer(), message);
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        final Request request = this.requests.remove(event.getPlayer().getUniqueId());

        if (request != null && request.onCancel != null) {
            request.onCancel.run();
        }
    }

    private static class Request {

        private final BiConsumer<Player, String> callback;
        private final Runnable onCancel;

        Request(BiConsumer<Player, String> callback, Runnable onCancel) {
            this.callback = callback;
            this.onCancel = onCancel;
        }

    }

}
