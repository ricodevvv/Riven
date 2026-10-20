package dev.ricodevvv.riven.party.event;

import dev.ricodevvv.riven.party.Party;
import org.bukkit.event.HandlerList;

/**
 * A party got disbanded.
 */
public class PartyDisbandEvent extends PartyEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    public PartyDisbandEvent(Party party) {
        super(party);
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

}
