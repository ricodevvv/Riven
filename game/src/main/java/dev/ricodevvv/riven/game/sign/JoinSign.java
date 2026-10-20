package dev.ricodevvv.riven.game.sign;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

/**
 * A sign placed in the lobby that joins a group of games (SOLO, TEAM...). It's not tied to one game:
 * the manager rotates it to whatever game of its group is free.
 */
public class JoinSign {

    private final String world;
    private final int x;
    private final int y;
    private final int z;
    private final String group;
    private transient String targetId;

    public JoinSign(Location location, String group) {
        this(location.getWorld().getName(), location.getBlockX(), location.getBlockY(), location.getBlockZ(), group);
    }

    public JoinSign(String world, int x, int y, int z, String group) {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.group = group.toUpperCase();
    }

    /**
     * @return the sign location, or null if the world isn't loaded
     */
    public Location getLocation() {
        final World bukkit = Bukkit.getWorld(this.world);

        return bukkit == null ? null : new Location(bukkit, this.x, this.y, this.z);
    }

    public boolean isAt(Location location) {
        return location.getWorld().getName().equals(this.world) && location.getBlockX() == this.x && location.getBlockY() == this.y && location.getBlockZ() == this.z;
    }

    /**
     * @return key used in the signs file
     */
    public String getKey() {
        return this.world + ";" + this.x + ";" + this.y + ";" + this.z;
    }

    public String getWorld() {
        return this.world;
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    public int getZ() {
        return this.z;
    }

    public String getGroup() {
        return this.group;
    }

    public String getTargetId() {
        return this.targetId;
    }

    void setTargetId(String targetId) {
        this.targetId = targetId;
    }

}
