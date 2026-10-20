package dev.ricodevvv.riven.party.event;

import dev.ricodevvv.riven.party.Party;
import org.bukkit.event.HandlerList;

import java.util.UUID;

/**
 * Someone stopped being part of a party.
 */
public class PartyLeaveEvent extends PartyEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID member;
    private final Reason reason;

    public PartyLeaveEvent(Party party, UUID member, Reason reason) {
        super(party);
        this.member = member;
        this.reason = reason;
    }

    public UUID getMember() {
        return this.member;
    }

    public Reason getReason() {
        return this.reason;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    public enum Reason {
        LEFT,
        KICKED,
        OFFLINE,
        DISBANDED
    }

}
