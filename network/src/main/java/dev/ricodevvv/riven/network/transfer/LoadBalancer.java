package dev.ricodevvv.riven.network.transfer;

import dev.ricodevvv.riven.network.server.GameInfo;
import dev.ricodevvv.riven.network.server.ServerInfo;
import dev.ricodevvv.riven.network.server.ServerMode;
import dev.ricodevvv.riven.network.server.ServerRegistry;

import java.util.function.Predicate;

/**
 * Picks where a player (or a whole party) should play: first a game of the right mode that's still joinable
 * and has room for everyone, preferring the fullest so games start sooner; if there's none, the least loaded
 * server that can create a new game. Draining and dead servers are skipped.
 */
public class LoadBalancer {

    private final ServerRegistry registry;

    public LoadBalancer(ServerRegistry registry) {
        this.registry = registry;
    }

    /**
     * @param mode game mode, like SOLO
     * @param partySize how many players go together
     * @param filter extra game filter (a map, for example), can be null
     * @return where to go, or null if the network is full
     */
    public Target find(String mode, int partySize, Predicate<GameInfo> filter) {
        ServerInfo bestServer = null;
        GameInfo bestGame = null;

        for (ServerInfo server : this.registry.getServers(this.registry.getGroup(), ServerMode.GAME)) {
            if (server.isDraining()) {
                continue;
            }

            for (GameInfo game : server.getGames()) {
                if (!game.isJoinable() || !game.getMode().equalsIgnoreCase(mode) || game.getFreeSlots() < partySize || (filter != null && !filter.test(game))) {
                    continue;
                }

                if (bestGame == null || game.getPlayers() > bestGame.getPlayers()
                        || (game.getPlayers() == bestGame.getPlayers() && server.getPlayers() < bestServer.getPlayers())) {
                    bestGame = game;
                    bestServer = server;
                }
            }
        }

        if (bestGame != null) {
            return new Target(bestServer, bestGame);
        }

        ServerInfo leastLoaded = null;

        for (ServerInfo server : this.registry.getServers(this.registry.getGroup(), ServerMode.GAME)) {
            if (server.canCreateGame() && server.getPlayers() + partySize <= server.getMaxPlayers()
                    && (leastLoaded == null || server.getPlayers() < leastLoaded.getPlayers())) {
                leastLoaded = server;
            }
        }
        return leastLoaded == null ? null : new Target(leastLoaded, null);
    }

    /**
     * Where to send someone.
     */
    public static class Target {

        private final ServerInfo server;
        private final GameInfo game;

        Target(ServerInfo server, GameInfo game) {
            this.server = server;
            this.game = game;
        }

        public ServerInfo getServer() {
            return this.server;
        }

        /**
         * @return the game to join, or null if the server should create one
         */
        public GameInfo getGame() {
            return this.game;
        }

    }

}
