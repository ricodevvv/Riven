package dev.ricodevvv.riven.game.protocol;

import dev.ricodevvv.riven.game.Game;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Turns a game's protocols on and off. Port of HyriGameProtocolManager.
 */
public class ProtocolManager {

    private final JavaPlugin plugin;
    private final Game<?> game;
    private final Map<String, GameProtocol> protocols = new ConcurrentHashMap<>();

    public ProtocolManager(JavaPlugin plugin, Game<?> game) {
        this.plugin = plugin;
        this.game = game;
    }

    /**
     * Turns a protocol on if it wasn't already.
     *
     * @param protocol protocol
     * @return true if it turned on
     */
    public boolean enableProtocol(GameProtocol protocol) {
        if (this.protocols.containsKey(protocol.getName())) {
            return false;
        }

        for (Class<? extends GameProtocol> required : protocol.getRequiredProtocols()) {
            if (this.getProtocol(required) == null) {
                throw new IllegalStateException("Protocol '" + protocol.getName() + "' needs " + required.getSimpleName() + " enabled");
            }
        }

        protocol.enable();

        this.plugin.getServer().getPluginManager().registerEvents(protocol, this.plugin);
        this.protocols.put(protocol.getName(), protocol);
        return true;
    }

    /**
     * Turns a protocol off by its class.
     *
     * @param protocolClass class
     * @return true if it turned off
     */
    public boolean disableProtocol(Class<? extends GameProtocol> protocolClass) {
        final GameProtocol protocol = this.getProtocol(protocolClass);

        if (protocol == null) {
            return false;
        }

        this.protocols.remove(protocol.getName());

        HandlerList.unregisterAll(protocol);

        protocol.disable();
        return true;
    }

    /**
     * Turns every protocol off.
     */
    public void disable() {
        for (GameProtocol protocol : new ArrayList<>(this.protocols.values())) {
            this.disableProtocol(protocol.getClass());
        }
    }

    /**
     * @param protocolClass protocol class
     * @param <T> type
     * @return the enabled protocol or null
     */
    public <T extends GameProtocol> T getProtocol(Class<T> protocolClass) {
        for (GameProtocol protocol : this.protocols.values()) {
            if (protocol.getClass() == protocolClass) {
                return protocolClass.cast(protocol);
            }
        }
        return null;
    }

    public boolean isProtocolActive(Class<? extends GameProtocol> protocolClass) {
        return this.getProtocol(protocolClass) != null;
    }

    public Game<?> getGame() {
        return this.game;
    }

}
