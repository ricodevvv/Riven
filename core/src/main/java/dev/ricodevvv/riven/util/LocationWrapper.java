package dev.ricodevvv.riven.util;

import org.bukkit.Location;
import org.bukkit.World;

/**
 * Location without a world, made to be saved in files and later applied to any copy of a map.
 */
public class LocationWrapper {

    private double x;
    private double y;
    private double z;
    private float yaw;
    private float pitch;

    public LocationWrapper() {}

    public LocationWrapper(double x, double y, double z) {
        this(x, y, z, 0.0F, 0.0F);
    }

    public LocationWrapper(double x, double y, double z, float yaw, float pitch) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    /**
     * @param location bukkit location
     * @return a wrapper with the same coordinates and direction
     */
    public static LocationWrapper from(Location location) {
        return new LocationWrapper(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
    }

    /**
     * @param location bukkit location
     * @return a wrapper snapped to the block (no decimals, no direction)
     */
    public static LocationWrapper fromBlock(Location location) {
        return new LocationWrapper(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    /**
     * Reads the format written by {@link #serialize()}: {@code x;y;z;yaw;pitch} (yaw and pitch are optional).
     *
     * @param text serialized location
     * @return the wrapper, or null if the text is broken
     */
    public static LocationWrapper deserialize(String text) {
        if (text == null) {
            return null;
        }

        final String[] parts = text.split(";");

        if (parts.length < 3) {
            return null;
        }

        try {
            return new LocationWrapper(Double.parseDouble(parts[0]), Double.parseDouble(parts[1]), Double.parseDouble(parts[2]),
                    parts.length > 3 ? Float.parseFloat(parts[3]) : 0.0F,
                    parts.length > 4 ? Float.parseFloat(parts[4]) : 0.0F);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * @return the location as {@code x;y;z;yaw;pitch}
     */
    public String serialize() {
        return this.x + ";" + this.y + ";" + this.z + ";" + this.yaw + ";" + this.pitch;
    }

    /**
     * @param world world to apply it to
     * @return the bukkit location
     */
    public Location asBukkit(World world) {
        return new Location(world, this.x, this.y, this.z, this.yaw, this.pitch);
    }

    /**
     * Same as {@link #asBukkit(World)} but centered on the block (x + 0.5, z + 0.5).
     *
     * @param world world to apply it to
     * @return the centered location
     */
    public Location asCenteredBukkit(World world) {
        return new Location(world, Math.floor(this.x) + 0.5D, this.y, Math.floor(this.z) + 0.5D, this.yaw, this.pitch);
    }

    public double getX() {
        return this.x;
    }

    public double getY() {
        return this.y;
    }

    public double getZ() {
        return this.z;
    }

    public float getYaw() {
        return this.yaw;
    }

    public float getPitch() {
        return this.pitch;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    @Override
    public String toString() {
        return String.format("%.1f, %.1f, %.1f", this.x, this.y, this.z);
    }

}
