package dev.ricodevvv.riven.party;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory parties, for a single server.
 */
public class LocalPartyBackend implements PartyBackend {

    private final Map<UUID, Party> byMember = new ConcurrentHashMap<>();

    @Override
    public Party getByMember(UUID member) {
        return this.byMember.get(member);
    }

    @Override
    public void save(Party party) {
        for (UUID member : party.getMembers()) {
            this.byMember.put(member, party);
        }
    }

    @Override
    public void delete(Party party) {
        this.byMember.values().removeIf(value -> value.getId().equals(party.getId()));
    }

    @Override
    public void unlink(UUID member) {
        this.byMember.remove(member);
    }

    @Override
    public Collection<Party> getParties() {
        return new HashSet<>(this.byMember.values());
    }

}
