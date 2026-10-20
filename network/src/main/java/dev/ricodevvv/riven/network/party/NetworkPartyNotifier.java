package dev.ricodevvv.riven.network.party;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.ricodevvv.riven.network.NetworkMessenger;
import dev.ricodevvv.riven.party.PartyManager;
import dev.ricodevvv.riven.party.PartyNotifier;
import dev.ricodevvv.riven.player.PlayerLookup;
import dev.ricodevvv.riven.text.FancyMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Delivers party messages to members on any server. The text gets built on the receiving server, so every
 * player reads it in their own language.
 */
public class NetworkPartyNotifier implements PartyNotifier {

    private static final String NOTIFY = "party:notify";

    private final PartyManager parties;
    private final NetworkMessenger messenger;
    private final PlayerLookup lookup;

    public NetworkPartyNotifier(PartyManager parties, NetworkMessenger messenger, PlayerLookup lookup) {
        this.parties = parties;
        this.messenger = messenger;
        this.lookup = lookup;
    }

    public void start() {
        this.messenger.on(NOTIFY, message -> {
            final JsonObject payload = message.getPayload();
            final Player player = Bukkit.getPlayer(UUID.fromString(payload.get("member").getAsString()));

            if (player == null) {
                return;
            }

            final JsonArray raw = payload.getAsJsonArray("replacements");
            final Object[] replacements = new Object[raw.size()];

            for (int i = 0; i < raw.size(); i++) {
                replacements[i] = raw.get(i).getAsString();
            }

            FancyMessage.sendAuto(player, this.parties.render(player, payload.get("key").getAsString(), replacements));
        });
    }

    @Override
    public void send(UUID member, String key, Object... replacements) {
        final Player local = Bukkit.getPlayer(member);

        if (local != null) {
            FancyMessage.sendAuto(local, this.parties.render(local, key, replacements));
            return;
        }

        final String server = this.lookup.getServer(member);

        if (server == null) {
            return;
        }

        final JsonObject payload = new JsonObject();
        final JsonArray list = new JsonArray();

        for (Object replacement : replacements) {
            final JsonElement element = new JsonPrimitive(String.valueOf(replacement));

            list.add(element);
        }

        payload.addProperty("member", member.toString());
        payload.addProperty("key", key);
        payload.add("replacements", list);

        this.messenger.send(NOTIFY, payload, server);
    }

}
