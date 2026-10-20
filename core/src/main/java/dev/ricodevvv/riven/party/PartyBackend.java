package dev.ricodevvv.riven.party;

import java.util.Collection;
import java.util.UUID;

/**
 * Where parties live. The default one keeps them in memory; the network module swaps it for one backed by
 * Redis so parties span every server.
 */
public interface PartyBackend {

    /**
     * @param member any member
     * @return the party, or null
     */
    Party getByMember(UUID member);

    /**
     * Stores (or updates) a party after a change.
     *
     * @param party party
     */
    void save(Party party);

    /**
     * Forgets a party.
     *
     * @param party party
     */
    void delete(Party party);

    /**
     * Drops the link between a player and a party, without touching the party itself.
     *
     * @param member player
     */
    void unlink(UUID member);

    /**
     * @return parties this server knows about
     */
    Collection<Party> getParties();

    /**
     * Whether this server runs the timers of a party (invite expiry). With many servers only one of them
     * should, or players would get every message once per server.
     *
     * @param party party
     * @return true if this server is in charge of it
     */
    default boolean isAuthority(Party party) {
        return true;
    }

}
