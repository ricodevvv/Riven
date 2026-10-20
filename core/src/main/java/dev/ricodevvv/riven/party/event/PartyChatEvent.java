package dev.ricodevvv.riven.party.event;

import dev.ricodevvv.riven.party.Party;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

/**
 * A member is talking in party chat. The message can be changed or blocked.
 */
public class PartyChatEvent extends PartyEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player sender;
    private String message;
    private boolean cancelled;

    public PartyChatEvent(Party party, Player sender, String message) {
        super(party);
        this.sender = sender;
        this.message = message;
    }

    public Player getSender() {
        return this.sender;
    }

    public String getMessage() {
        return this.message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

}
