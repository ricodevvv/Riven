package dev.ricodevvv.riven.party;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * A group of players that joins games together. It's a plain object (no Bukkit references inside) so it
 * can be saved to Redis or JSON as is.
 */
public class Party {

    private UUID id;
    private UUID leader;
    private Set<UUID> members = new LinkedHashSet<>();
    private Map<UUID, String> names = new LinkedHashMap<>();
    private Map<UUID, Long> invites = new LinkedHashMap<>();
    private Set<UUID> hidden = new LinkedHashSet<>();
    private boolean open;
    private long createdAt;

    Party() {}

    public Party(UUID leader, String leaderName) {
        this.id = UUID.randomUUID();
        this.leader = leader;
        this.createdAt = System.currentTimeMillis();
        this.members.add(leader);
        this.names.put(leader, leaderName);
    }

    public UUID getId() {
        return this.id;
    }

    public UUID getLeader() {
        return this.leader;
    }

    /**
     * Gives the lead to another member and moves them first in the list.
     *
     * @param leader new leader
     */
    void setLeader(UUID leader) {
        this.leader = leader;

        final List<UUID> others = new ArrayList<>(this.members);

        others.remove(leader);

        this.members.clear();
        this.members.add(leader);
        this.members.addAll(others);
    }

    /**
     * @return every member, leader first
     */
    public List<UUID> getMembers() {
        return Collections.unmodifiableList(new ArrayList<>(this.members));
    }

    /**
     * @return members connected to this server
     */
    public List<Player> getOnlineMembers() {
        final List<Player> online = new ArrayList<>();

        for (UUID member : this.members) {
            final Player player = Bukkit.getPlayer(member);

            if (player != null) {
                online.add(player);
            }
        }
        return online;
    }

    public boolean isMember(UUID uniqueId) {
        return this.members.contains(uniqueId);
    }

    public boolean isLeader(UUID uniqueId) {
        return this.leader.equals(uniqueId);
    }

    public int size() {
        return this.members.size();
    }

    /**
     * @param uniqueId member
     * @return their last known name
     */
    public String getName(UUID uniqueId) {
        final String name = this.names.get(uniqueId);

        return name != null ? name : uniqueId.toString().substring(0, 8);
    }

    /**
     * @param name member name, any case
     * @return their uuid, or null if no member has that name
     */
    public UUID findMember(String name) {
        for (Map.Entry<UUID, String> entry : this.names.entrySet()) {
            if (entry.getValue().equalsIgnoreCase(name) && this.members.contains(entry.getKey())) {
                return entry.getKey();
            }
        }
        return null;
    }

    void addMember(UUID uniqueId, String name) {
        this.members.add(uniqueId);
        this.names.put(uniqueId, name);
        this.invites.remove(uniqueId);
    }

    void removeMember(UUID uniqueId) {
        this.members.remove(uniqueId);
        this.hidden.remove(uniqueId);
    }

    void invite(UUID uniqueId, String name, long expiration) {
        this.invites.put(uniqueId, expiration);
        this.names.put(uniqueId, name);
    }

    /**
     * @param uniqueId player
     * @return true if they have an invite that hasn't expired
     */
    public boolean isInvited(UUID uniqueId) {
        final Long expiration = this.invites.get(uniqueId);

        return expiration != null && expiration > System.currentTimeMillis();
    }

    public boolean hasPendingInvites() {
        for (Long expiration : this.invites.values()) {
            if (expiration > System.currentTimeMillis()) {
                return true;
            }
        }
        return false;
    }

    void removeInvite(UUID uniqueId) {
        this.invites.remove(uniqueId);
    }

    /**
     * Drops expired invites.
     *
     * @return players whose invite just expired
     */
    List<UUID> clearExpiredInvites() {
        final List<UUID> expired = new ArrayList<>();
        final Iterator<Map.Entry<UUID, Long>> iterator = this.invites.entrySet().iterator();

        while (iterator.hasNext()) {
            final Map.Entry<UUID, Long> entry = iterator.next();

            if (entry.getValue() <= System.currentTimeMillis()) {
                expired.add(entry.getKey());
                iterator.remove();
            }
        }
        return expired;
    }

    /**
     * @return true if anyone can join without an invite
     */
    public boolean isOpen() {
        return this.open;
    }

    void setOpen(boolean open) {
        this.open = open;
    }

    /**
     * @param uniqueId member
     * @return true if they joined silently (staff) and shouldn't show up in announcements
     */
    public boolean isHidden(UUID uniqueId) {
        return this.hidden.contains(uniqueId);
    }

    void setHidden(UUID uniqueId) {
        this.hidden.add(uniqueId);
    }

    public long getCreatedAt() {
        return this.createdAt;
    }

}
