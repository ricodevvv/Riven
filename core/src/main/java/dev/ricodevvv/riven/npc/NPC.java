package dev.ricodevvv.riven.npc;

import com.mojang.authlib.GameProfile;
import dev.ricodevvv.riven.hologram.Hologram;
import dev.ricodevvv.riven.util.PacketUtil;
import net.minecraft.server.v1_8_R3.EntityPlayer;
import net.minecraft.server.v1_8_R3.PacketPlayOutAnimation;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntity;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntityDestroy;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntityEquipment;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntityHeadRotation;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntityMetadata;
import net.minecraft.server.v1_8_R3.PacketPlayOutNamedEntitySpawn;
import net.minecraft.server.v1_8_R3.PacketPlayOutPlayerInfo;
import net.minecraft.server.v1_8_R3.PlayerInteractManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_8_R3.CraftServer;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * Player shaped NPC that only exists in packets. Port of Hyrame's NPC.
 */
public class NPC extends EntityPlayer {

    protected final JavaPlugin plugin;
    protected final Location location;
    protected final Map<Integer, ItemStack> equipment = new HashMap<>();
    protected final Set<UUID> viewers = ConcurrentHashMap.newKeySet();

    protected Predicate<Player> visibility = player -> true;
    protected NPCInteractCallback interactCallback;
    protected Hologram hologram;
    protected boolean trackingPlayer;

    public NPC(JavaPlugin plugin, Location location, GameProfile profile) {
        super(((CraftServer) Bukkit.getServer()).getServer(), ((CraftWorld) location.getWorld()).getHandle(), profile, new PlayerInteractManager(((CraftWorld) location.getWorld()).getHandle()));
        this.plugin = plugin;
        this.location = location.clone();

        super.setLocation(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());

        this.getDataWatcher().watch(10, (byte) 127);
    }

    /**
     * Sends the NPC to a player. Two seconds later it's removed from the tab so it doesn't show in the list.
     *
     * @param player player
     */
    void spawnFor(Player player) {
        this.viewers.add(player.getUniqueId());

        PacketUtil.sendPacket(player, new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER, this));
        PacketUtil.sendPacket(player, new PacketPlayOutNamedEntitySpawn(this));
        PacketUtil.sendPacket(player, new PacketPlayOutEntityHeadRotation(this, (byte) (this.yaw * 256.0F / 360.0F)));
        PacketUtil.sendPacket(player, new PacketPlayOutEntityMetadata(this.getId(), this.getDataWatcher(), true));
        PacketUtil.sendPacket(player, new PacketPlayOutAnimation(this, 0));

        for (Map.Entry<Integer, ItemStack> entry : this.equipment.entrySet()) {
            PacketUtil.sendPacket(player, new PacketPlayOutEntityEquipment(this.getId(), entry.getKey(), CraftItemStack.asNMSCopy(entry.getValue())));
        }

        Bukkit.getScheduler().runTaskLater(this.plugin, () -> PacketUtil.sendPacket(player, new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.REMOVE_PLAYER, this)), 40L);
    }

    /**
     * Removes the NPC from a player's client.
     *
     * @param player player
     */
    void despawnFor(Player player) {
        this.viewers.remove(player.getUniqueId());

        PacketUtil.sendPacket(player, new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.REMOVE_PLAYER, this));
        PacketUtil.sendPacket(player, new PacketPlayOutEntityDestroy(this.getId()));
    }

    /**
     * Turns the NPC's head to a direction, only for one player.
     *
     * @param player player who sees the turn
     * @param yaw horizontal rotation
     * @param pitch vertical rotation
     */
    void look(Player player, float yaw, float pitch) {
        PacketUtil.sendPacket(player, new PacketPlayOutEntity.PacketPlayOutEntityLook(this.getId(), (byte) (yaw * 256.0F / 360.0F), (byte) (pitch * 256.0F / 360.0F), false));
        PacketUtil.sendPacket(player, new PacketPlayOutEntityHeadRotation(this, (byte) (yaw * 256.0F / 360.0F)));
    }

    /**
     * Puts an item on the NPC (0 hand, 1 boots, 2 leggings, 3 chestplate, 4 helmet).
     *
     * @param slot equipment slot
     * @param itemStack item
     * @return this NPC
     */
    public NPC setEquipment(int slot, ItemStack itemStack) {
        this.equipment.put(slot, itemStack);

        for (UUID viewer : this.viewers) {
            PacketUtil.sendPacket(Bukkit.getPlayer(viewer), new PacketPlayOutEntityEquipment(this.getId(), slot, CraftItemStack.asNMSCopy(itemStack)));
        }
        return this;
    }

    public Location getLocation() {
        return this.location.clone();
    }

    public NPC setInteractCallback(NPCInteractCallback interactCallback) {
        this.interactCallback = interactCallback;
        return this;
    }

    public NPCInteractCallback getInteractCallback() {
        return this.interactCallback;
    }

    public NPC setTrackingPlayer(boolean trackingPlayer) {
        this.trackingPlayer = trackingPlayer;
        return this;
    }

    public boolean isTrackingPlayer() {
        return this.trackingPlayer;
    }

    /**
     * Condition for a player to see the NPC (on top of being close and in the same world).
     *
     * @param visibility condition
     * @return this NPC
     */
    public NPC setVisibility(Predicate<Player> visibility) {
        this.visibility = visibility;
        return this;
    }

    Predicate<Player> getVisibility() {
        return this.visibility;
    }

    public Hologram getHologram() {
        return this.hologram;
    }

    void setHologram(Hologram hologram) {
        this.hologram = hologram;
    }

    boolean isViewer(Player player) {
        return this.viewers.contains(player.getUniqueId());
    }

    Set<UUID> getViewers() {
        return this.viewers;
    }

}
