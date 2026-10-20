package dev.ricodevvv.riven.game.protocol;

import dev.ricodevvv.riven.game.Game;
import dev.ricodevvv.riven.game.GamePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.List;

/**
 * A "protocol" is a piece of logic you turn on and off during the game (death, waiting, spectator...).
 * Port of HyriGameProtocol. Since there are many games, each protocol belongs to one and its listeners filter by it.
 */
public abstract class GameProtocol implements Listener {

    private final List<Class<? extends GameProtocol>> requiredProtocols = new ArrayList<>();

    protected final Game<?> game;
    protected final String name;

    protected GameProtocol(Game<?> game, String name) {
        this.game = game;
        this.name = name;
    }

    /**
     * Called when the protocol turns on, before its listeners get registered.
     */
    protected abstract void enable();

    /**
     * Called when the protocol turns off, after its listeners get removed.
     */
    protected abstract void disable();

    /**
     * Marks another protocol as required for this one to work.
     *
     * @param protocol required protocol class
     */
    protected void require(Class<? extends GameProtocol> protocol) {
        this.requiredProtocols.add(protocol);
    }

    /**
     * @param player player
     * @return the player of this game, or null if they're not in it
     */
    protected GamePlayer getGamePlayer(Player player) {
        return this.game.getPlayer(player.getUniqueId());
    }

    /**
     * @param player player
     * @return true if the player plays or watches this game
     */
    protected boolean isInGame(Player player) {
        return this.game.contains(player);
    }

    List<Class<? extends GameProtocol>> getRequiredProtocols() {
        return this.requiredProtocols;
    }

    public String getName() {
        return this.name;
    }

    public Game<?> getGame() {
        return this.game;
    }

}
