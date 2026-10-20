package dev.ricodevvv.riven.party.event;

import dev.ricodevvv.riven.party.Party;
import org.bukkit.event.HandlerList;

import java.util.UUID;

/**
 * A party joined (by invite, open party or silently).
 */
public class PartyJoinEvent extends PartyEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID member;

    public PartyJoinEvent(Party party, UUID member) {
        super(party);
        this.member = member;
    }

    public UUID getMember() {
        return this.member;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

}
