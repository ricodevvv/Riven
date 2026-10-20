package dev.ricodevvv.riven.scoreboard;

import dev.ricodevvv.riven.util.PacketUtil;
import dev.ricodevvv.riven.util.Reflection;
import net.minecraft.server.v1_8_R3.Packet;
import net.minecraft.server.v1_8_R3.PacketPlayOutScoreboardTeam;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Tab teams sent only to a group of players (the "receivers").
 * Every game has its own, so the colors of one arena never leak into another.
 */
public class NameTagTeamHandler {

    private static final int CREATE = 0;
    private static final int REMOVE = 1;
    private static final int UPDATE = 2;
    private static final int ADD_PLAYERS = 3;
    private static final int REMOVE_PLAYERS = 4;

    private final Map<String, NameTagTeam> teams = new LinkedHashMap<>();
    private final List<UUID> receivers = new CopyOnWriteArrayList<>();

    /**
     * Adds a receiver and sends them every current team.
     *
     * @param player player
     */
    public void addReceiver(Player player) {
        if (this.receivers.contains(player.getUniqueId())) {
            return;
        }

        this.receivers.add(player.getUniqueId());

        for (NameTagTeam team : this.teams.values()) {
            PacketUtil.sendPacket(player, this.packet(team, team.getPlayers(), CREATE));
        }
    }

    /**
     * Removes a receiver and wipes the teams from their client.
     *
     * @param player player
     */
    public void removeReceiver(Player player) {
        if (!this.receivers.remove(player.getUniqueId())) {
            return;
        }

        for (NameTagTeam team : this.teams.values()) {
            PacketUtil.sendPacket(player, this.packet(team, Collections.emptySet(), REMOVE));
        }
    }

    /**
     * Registers a team and sends it to every receiver.
     *
     * @param team equipo
     */
    public void addTeam(NameTagTeam team) {
        if (this.teams.containsKey(team.getName())) {
            return;
        }

        this.teams.put(team.getName(), team);
        this.broadcast(this.packet(team, team.getPlayers(), CREATE));
    }

    public void removeTeam(String name) {
        final NameTagTeam team = this.teams.remove(name);

        if (team != null) {
            this.broadcast(this.packet(team, Collections.emptySet(), REMOVE));
        }
    }

    /**
     * Re-sends a team's prefix, suffix and visibility.
     *
     * @param name team name
     */
    public void updateTeam(String name) {
        final NameTagTeam team = this.teams.get(name);

        if (team != null) {
            this.broadcast(this.packet(team, Collections.emptySet(), UPDATE));
        }
    }

    /**
     * Puts a player (by name) in a team and takes them out of any other.
     *
     * @param player player name
     * @param teamName target team
     */
    public void setPlayerTeam(String player, String teamName) {
        final NameTagTeam team = this.teams.get(teamName);

        if (team == null || team.contains(player)) {
            return;
        }

        this.removePlayer(player);

        team.addPlayer(player);

        this.broadcast(this.packet(team, Collections.singleton(player), ADD_PLAYERS));
    }

    /**
     * Takes a player out of their team.
     *
     * @param player player name
     */
    public void removePlayer(String player) {
        for (NameTagTeam team : this.teams.values()) {
            if (team.contains(player)) {
                team.removePlayer(player);

                this.broadcast(this.packet(team, Collections.singleton(player), REMOVE_PLAYERS));
            }
        }
    }

    /**
     * Deletes every team from every receiver.
     */
    public void destroy() {
        for (NameTagTeam team : new ArrayList<>(this.teams.values())) {
            this.removeTeam(team.getName());
        }

        this.receivers.clear();
    }

    public NameTagTeam getTeam(String name) {
        return this.teams.get(name);
    }

    public Collection<NameTagTeam> getTeams() {
        return this.teams.values();
    }

    private void broadcast(Packet<?> packet) {
        for (UUID receiver : this.receivers) {
            final Player player = Bukkit.getPlayer(receiver);

            if (player != null) {
                PacketUtil.sendPacket(player, packet);
            }
        }
    }

    private Packet<?> packet(NameTagTeam team, Set<String> players, int mode) {
        final PacketPlayOutScoreboardTeam packet = new PacketPlayOutScoreboardTeam();

        Reflection.setField("a", packet, team.getName());

        if (mode == CREATE || mode == UPDATE) {
            Reflection.setField("b", packet, team.getName());
            Reflection.setField("c", packet, team.getPrefix());
            Reflection.setField("d", packet, team.getSuffix());
            Reflection.setField("e", packet, team.getNameTagVisibility().getValue());
            Reflection.setField("f", packet, -1);
            Reflection.setField("i", packet, 1);
        }

        Reflection.setField("g", packet, new HashSet<>(players));
        Reflection.setField("h", packet, mode);

        return packet;
    }

}
