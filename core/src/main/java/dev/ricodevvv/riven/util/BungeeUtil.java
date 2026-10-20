package dev.ricodevvv.riven.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Sends players to other servers through the BungeeCord channel.
 */
public final class BungeeUtil {

    public static final String CHANNEL = "BungeeCord";

    private BungeeUtil() {}

    /**
     * Registers the outgoing channel. Safe to call more than once.
     *
     * @param plugin plugin sending the messages
     */
    public static void register(JavaPlugin plugin) {
        if (!Bukkit.getMessenger().isOutgoingChannelRegistered(plugin, CHANNEL)) {
            Bukkit.getMessenger().registerOutgoingPluginChannel(plugin, CHANNEL);
        }
    }

    /**
     * Connects the player to another server. If they're still here after three seconds (no proxy), they get
     * kicked with a note.
     *
     * @param plugin plugin sending the message
     * @param player player
     * @param server server name on the proxy
     */
    public static void connect(JavaPlugin plugin, Player player, String server) {
        connect(plugin, player, server, true);
    }

    /**
     * Connects the player to another server.
     *
     * @param plugin plugin sending the message
     * @param player player
     * @param server server name on the proxy
     * @param kickFallback true to kick them if the transfer didn't happen
     */
    public static void connect(JavaPlugin plugin, Player player, String server, boolean kickFallback) {
        register(plugin);

        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream(); DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeUTF("Connect");
            out.writeUTF(server);

            player.sendPluginMessage(plugin, CHANNEL, bytes.toByteArray());
        } catch (IOException e) {
            plugin.getLogger().warning("Couldn't send " + player.getName() + " to " + server);
        }

        if (kickFallback) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    player.kickPlayer("Lobby: " + server);
                }
            }, 60L);
        }
    }

}
