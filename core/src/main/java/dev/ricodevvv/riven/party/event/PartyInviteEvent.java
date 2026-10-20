package dev.ricodevvv.riven.party.event;

import dev.ricodevvv.riven.party.Party;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

import java.util.UUID;

/**
 * A leader is about to invite someone. Cancel it to block the invite (for example if the target ignores invites).
 */
public class PartyInviteEvent extends PartyEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID target;
    private boolean cancelled;

    public PartyInviteEvent(Party party, UUID target) {
        super(party);
        this.target = target;
    }

    public UUID getTarget() {
        return this.target;
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
