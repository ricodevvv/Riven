package dev.ricodevvv.riven.party;

import java.util.UUID;

/**
 * Delivers party messages to a member, wherever they are. The default one only reaches players on this
 * server; the network module replaces it to reach other servers too.
 */
@FunctionalInterface
public interface PartyNotifier {

    /**
     * @param member receiver
     * @param key language key
     * @param replacements placeholder and value pairs
     */
    void send(UUID member, String key, Object... replacements);

}
