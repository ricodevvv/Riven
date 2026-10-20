package dev.ricodevvv.riven.game.event;

import dev.ricodevvv.riven.game.Game;
import dev.ricodevvv.riven.game.GamePlayer;
import dev.ricodevvv.riven.game.protocol.LastHitterProtocol;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * A player died. Extra text can be added at the end of the death message (like "FINAL KILL").
 */
public class GameDeathEvent extends GameEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final GamePlayer gamePlayer;
    private final Reason reason;
    private final List<LastHitterProtocol.LastHitter> killers;
    private final List<Function<Player, String>> messagesToAdd = new ArrayList<>();

    public GameDeathEvent(Game<?> game, GamePlayer gamePlayer, Reason reason, List<LastHitterProtocol.LastHitter> killers) {
        super(game);
        this.gamePlayer = gamePlayer;
        this.reason = reason;
        this.killers = killers == null ? Collections.emptyList() : killers;
    }

    public GamePlayer getGamePlayer() {
        return this.gamePlayer;
    }

    public Reason getReason() {
        return this.reason;
    }

    /**
     * @return the last people who hit them, the first one did the most damage
     */
    public List<LastHitterProtocol.LastHitter> getKillers() {
        return this.killers;
    }

    /**
     * @return the main killer, or null if they died on their own
     */
    public GamePlayer getKiller() {
        return this.killers.isEmpty() ? null : this.killers.get(0).asGamePlayer();
    }

    /**
     * Adds text at the end of the death message.
     *
     * @param message text per player
     */
    public void addMessage(Function<Player, String> message) {
        this.messagesToAdd.add(message);
    }

    public List<Function<Player, String>> getMessagesToAdd() {
        return this.messagesToAdd;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    /**
     * Death causes.
     */
    public enum Reason {

        PLAYERS("player"),
        VOID("void"),
        FALL("fall"),
        BLOCK_EXPLOSION("explosion"),
        ENTITY_EXPLOSION("explosion"),
        LAVA("lava"),
        FIRE("fire"),
        LIGHTNING("lightning"),
        UNKNOWN("unknown");

        private final String key;

        Reason(String key) {
            this.key = key;
        }

        public String getKey() {
            return this.key;
        }

    }

}
