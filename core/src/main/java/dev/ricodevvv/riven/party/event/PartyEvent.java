package dev.ricodevvv.riven.party.event;

import dev.ricodevvv.riven.party.Party;
import org.bukkit.event.Event;

/**
 * Base of every party event. They fire on the server where the action happened.
 */
public abstract class PartyEvent extends Event {

    private final Party party;

    protected PartyEvent(Party party) {
        this.party = party;
    }

    public Party getParty() {
        return this.party;
    }

}
