package dev.ricodevvv.riven.player;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Finds players by name or uuid. The default one only looks at this server; the network module installs one
 * that sees every server, so parties, invites and spectating work across the whole network.
 */
public interface PlayerLookup {

    /**
     * Looks only at this server.
     */
    PlayerLookup LOCAL = new PlayerLookup() {
        @Override
        public UUID findOnline(String name) {
            final Player player = Bukkit.getPlayerExact(name);

            return player == null ? null : player.getUniqueId();
        }

        @Override
        public String getName(UUID uniqueId) {
            final Player player = Bukkit.getPlayer(uniqueId);

            if (player != null) {
                return player.getName();
            }

            final String name = Bukkit.getOfflinePlayer(uniqueId).getName();

            return name != null ? name : uniqueId.toString().substring(0, 8);
        }

        @Override
        public boolean isOnline(UUID uniqueId) {
            return Bukkit.getPlayer(uniqueId) != null;
        }

        @Override
        public String getServer(UUID uniqueId) {
            return Bukkit.getPlayer(uniqueId) != null ? "local" : null;
        }
    };

    /**
     * @param name exact name, any case
     * @return the uuid of the online player with that name, or null
     */
    UUID findOnline(String name);

    /**
     * @param uniqueId player
     * @return their name, or a short uuid if it's unknown
     */
    String getName(UUID uniqueId);

    /**
     * @param uniqueId player
     * @return true if they're online anywhere this lookup can see
     */
    boolean isOnline(UUID uniqueId);

    /**
     * @param uniqueId player
     * @return the server they're on, or null if offline
     */
    String getServer(UUID uniqueId);

}
