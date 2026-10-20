package dev.ricodevvv.riven.npc;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import dev.ricodevvv.riven.hologram.Hologram;
import dev.ricodevvv.riven.packet.PacketHandler;
import dev.ricodevvv.riven.packet.PacketInterceptor;
import dev.ricodevvv.riven.util.Reflection;
import net.minecraft.server.v1_8_R3.PacketPlayInUseEntity;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.NameTagVisibility;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Creates NPCs, decides who sees them and hands them the clicks. Port of Hyrame's NPCManager and NPCHandler.
 */
public class NPCManager implements Listener {

    private static final String NPC_TEAM_NAME = "zNPC";
    private static final String NPC_NAME_PREFIX = "[NPC] ";
    private static final double VIEW_RANGE = 48.0D;
    private static final double INTERACT_RANGE = 5.0D;
    private static final double TRACK_RANGE = 6.0D;

    private final JavaPlugin plugin;
    private final PacketInterceptor packetInterceptor;
    private final List<NPC> npcs = new CopyOnWriteArrayList<>();

    private PacketHandler packetHandler;
    private BukkitTask task;

    public NPCManager(JavaPlugin plugin, PacketInterceptor packetInterceptor) {
        this.plugin = plugin;
        this.packetInterceptor = packetInterceptor;
    }

    public void start() {
        Bukkit.getPluginManager().registerEvents(this, this.plugin);

        this.packetHandler = new PacketHandler() {
            @Override
            public boolean onReceive(Player player, Object packet) {
                final int entityId = Reflection.getField("a", packet);
                final PacketPlayInUseEntity.EnumEntityUseAction action = ((PacketPlayInUseEntity) packet).a();

                for (NPC npc : npcs) {
                    if (npc.getId() != entityId || npc.getInteractCallback() == null || !npc.isViewer(player)) {
                        continue;
                    }

                    Bukkit.getScheduler().runTask(plugin, () -> {
                        if (player.getWorld() == npc.getLocation().getWorld() && player.getLocation().distance(npc.getLocation()) <= INTERACT_RANGE) {
                            npc.getInteractCallback().call(action != PacketPlayInUseEntity.EnumEntityUseAction.ATTACK, player);
                        }
                    });
                    return true;
                }
                return false;
            }
        };

        this.packetInterceptor.addHandler(PacketPlayInUseEntity.class, this.packetHandler);

        this.task = Bukkit.getScheduler().runTaskTimer(this.plugin, this::tick, 5L, 5L);
    }

    public void stop() {
        HandlerList.unregisterAll(this);

        if (this.packetHandler != null) {
            this.packetInterceptor.removeHandler(this.packetHandler);
        }

        if (this.task != null) {
            this.task.cancel();
        }

        for (NPC npc : new ArrayList<>(this.npcs)) {
            this.remove(npc);
        }
    }

    /**
     * Creates an NPC with a skin and a hologram on top.
     *
     * @param location where it goes
     * @param skin skin, can be null
     * @param lines hologram lines (computed per player)
     * @param visibility who can see it
     * @return the new NPC
     */
    public NPC create(Location location, Skin skin, List<Function<Player, String>> lines, Predicate<Player> visibility) {
        final GameProfile profile = new GameProfile(UUID.randomUUID(), ChatColor.DARK_GRAY + NPC_NAME_PREFIX + UUID.randomUUID().toString().substring(0, 6));

        if (skin != null) {
            profile.getProperties().put("textures", new Property("textures", skin.getTextureData(), skin.getTextureSignature()));
        }

        final NPC npc = new NPC(this.plugin, location, profile);

        npc.setVisibility(visibility);

        this.hideName(npc.getName());

        if (lines != null && !lines.isEmpty()) {
            final Hologram.Builder builder = new Hologram.Builder(this.plugin, location.clone().add(0.0D, 1.5D, 0.0D)).withVisibility(visibility);

            for (Function<Player, String> line : lines) {
                builder.withLine(new Hologram.Line(line));
            }

            npc.setHologram(builder.build());
        }

        this.npcs.add(npc);

        return npc;
    }

    private void hideName(String name) {
        final Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        Team team = scoreboard.getTeam(NPC_TEAM_NAME);

        if (team == null) {
            team = scoreboard.registerNewTeam(NPC_TEAM_NAME);
            team.setNameTagVisibility(NameTagVisibility.NEVER);
        }

        team.addEntry(name);
    }

    /**
     * Removes an NPC for everyone.
     *
     * @param npc NPC to remove
     */
    public void remove(NPC npc) {
        this.npcs.remove(npc);

        for (UUID viewer : new ArrayList<>(npc.getViewers())) {
            final Player player = Bukkit.getPlayer(viewer);

            if (player != null) {
                npc.despawnFor(player);
            }
        }

        if (npc.getHologram() != null) {
            npc.getHologram().destroy();
        }

        final Team team = Bukkit.getScoreboardManager().getMainScoreboard().getTeam(NPC_TEAM_NAME);

        if (team != null) {
            team.removeEntry(npc.getName());
        }
    }

    public List<NPC> getNPCs() {
        return this.npcs;
    }

    private void tick() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            for (NPC npc : this.npcs) {
                final Location location = npc.getLocation();
                final boolean sameWorld = player.getWorld() == location.getWorld();
                final double distance = sameWorld ? player.getLocation().distanceSquared(location) : Double.MAX_VALUE;
                final boolean shouldSee = sameWorld && distance <= VIEW_RANGE * VIEW_RANGE && npc.getVisibility().test(player);

                if (shouldSee && !npc.isViewer(player)) {
                    npc.spawnFor(player);
                } else if (!shouldSee && npc.isViewer(player)) {
                    npc.despawnFor(player);
                }

                if (shouldSee && npc.isTrackingPlayer() && distance <= TRACK_RANGE * TRACK_RANGE) {
                    final Location direction = location.clone();
                    final Vector vector = player.getEyeLocation().toVector().subtract(location.clone().add(0.0D, 1.62D, 0.0D).toVector());

                    direction.setDirection(vector);

                    npc.look(player, direction.getYaw(), direction.getPitch());
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        for (NPC npc : this.npcs) {
            npc.getViewers().remove(event.getPlayer().getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        for (NPC npc : this.npcs) {
            npc.getViewers().remove(event.getPlayer().getUniqueId());
        }
    }

}
