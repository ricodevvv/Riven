package dev.ricodevvv.riven.network.party;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.ricodevvv.riven.network.NetworkMessenger;
import dev.ricodevvv.riven.network.redis.RedisConnector;
import dev.ricodevvv.riven.party.Party;
import dev.ricodevvv.riven.party.PartyBackend;
import org.bukkit.Bukkit;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Parties stored in Redis (two hours since the last change by default) and cached on every server. Every
 * change is published, so all servers see the same party right away. Only the server where the leader is
 * runs the party timers.
 */
public class RedisPartyBackend implements PartyBackend {

    private static final String UPDATE = "party:update";
    private static final String DELETE = "party:delete";
    private static final String UNLINK = "party:unlink";
    private static final long MISS_CACHE_MILLIS = 5000L;

    private final RedisConnector redis;
    private final NetworkMessenger messenger;
    private final int ttlSeconds;
    private final Gson gson = new Gson();
    private final Map<UUID, Party> parties = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> members = new ConcurrentHashMap<>();
    private final Map<UUID, Long> misses = new ConcurrentHashMap<>();

    /**
     * @param redis connector
     * @param messenger messenger
     * @param ttlSeconds how long a party lives in Redis without changes (7200 = two hours)
     */
    public RedisPartyBackend(RedisConnector redis, NetworkMessenger messenger, int ttlSeconds) {
        this.redis = redis;
        this.messenger = messenger;
        this.ttlSeconds = ttlSeconds;
    }

    public void start() {
        this.messenger.onAsync(UPDATE, message -> {
            if (!message.getSource().equals(this.messenger.getServerId())) {
                this.cache(this.gson.fromJson(message.getPayload().get("party").getAsString(), Party.class));
            }
        });

        this.messenger.onAsync(DELETE, message -> {
            final UUID id = UUID.fromString(message.getPayload().get("id").getAsString());
            final Party removed = this.parties.remove(id);

            if (removed != null) {
                for (UUID member : removed.getMembers()) {
                    this.members.remove(member, id);
                }
            }
        });

        this.messenger.onAsync(UNLINK, message -> {
            final UUID member = UUID.fromString(message.getPayload().get("member").getAsString());

            this.members.remove(member);
            this.misses.remove(member);
        });
    }

    private void cache(Party party) {
        final Party old = this.parties.put(party.getId(), party);

        if (old != null) {
            for (UUID member : old.getMembers()) {
                if (!party.isMember(member)) {
                    this.members.remove(member, party.getId());
                }
            }
        }

        for (UUID member : party.getMembers()) {
            this.members.put(member, party.getId());
            this.misses.remove(member);
        }
    }

    @Override
    public Party getByMember(UUID member) {
        final UUID partyId = this.members.get(member);

        if (partyId != null) {
            final Party cached = this.parties.get(partyId);

            if (cached != null) {
                return cached;
            }
        }

        final Long miss = this.misses.get(member);

        if (miss != null && System.currentTimeMillis() - miss < MISS_CACHE_MILLIS) {
            return null;
        }

        try {
            final Party party = this.redis.with(jedis -> {
                final String id = jedis.get(this.redis.key("party-member:" + member));
                final String json = id == null ? null : jedis.get(this.redis.key("party:" + id));

                return json == null ? null : this.gson.fromJson(json, Party.class);
            });

            if (party == null || !party.isMember(member)) {
                this.misses.put(member, System.currentTimeMillis());
                return null;
            }

            this.cache(party);
            return party;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void save(Party party) {
        this.cache(party);

        final String json = this.gson.toJson(party);
        final String id = party.getId().toString();
        final JsonObject payload = new JsonObject();

        payload.addProperty("party", json);

        this.redis.async(jedis -> {
            jedis.setex(this.redis.key("party:" + id), this.ttlSeconds, json);

            for (UUID member : party.getMembers()) {
                jedis.setex(this.redis.key("party-member:" + member), this.ttlSeconds, id);
            }
        });

        this.messenger.send(UPDATE, payload, null);
    }

    @Override
    public void delete(Party party) {
        this.parties.remove(party.getId());

        final JsonArray list = new JsonArray();

        for (UUID member : party.getMembers()) {
            this.members.remove(member, party.getId());
            list.add(this.gson.toJsonTree(member.toString()));
        }

        final String id = party.getId().toString();
        final JsonObject payload = new JsonObject();

        payload.addProperty("id", id);
        payload.add("members", list);

        this.redis.async(jedis -> {
            jedis.del(this.redis.key("party:" + id));

            for (JsonElement member : list) {
                final String key = this.redis.key("party-member:" + member.getAsString());

                if (id.equals(jedis.get(key))) {
                    jedis.del(key);
                }
            }
        });

        this.messenger.send(DELETE, payload, null);
    }

    @Override
    public void unlink(UUID member) {
        this.members.remove(member);

        final JsonObject payload = new JsonObject();

        payload.addProperty("member", member.toString());

        this.redis.async(jedis -> jedis.del(this.redis.key("party-member:" + member)));
        this.messenger.send(UNLINK, payload, null);
    }

    @Override
    public Collection<Party> getParties() {
        return new HashSet<>(this.parties.values());
    }

    @Override
    public boolean isAuthority(Party party) {
        return Bukkit.getPlayer(party.getLeader()) != null;
    }

}
