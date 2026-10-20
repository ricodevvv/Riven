package dev.ricodevvv.riven.util;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Block-aligned cuboid. Used for bases, protected zones, waiting rooms, islands and so on.
 */
public class Area implements Iterable<Block> {

    private final World world;
    private final int minX;
    private final int minY;
    private final int minZ;
    private final int maxX;
    private final int maxY;
    private final int maxZ;

    public Area(Location first, Location second) {
        this.world = first.getWorld();
        this.minX = Math.min(first.getBlockX(), second.getBlockX());
        this.minY = Math.min(first.getBlockY(), second.getBlockY());
        this.minZ = Math.min(first.getBlockZ(), second.getBlockZ());
        this.maxX = Math.max(first.getBlockX(), second.getBlockX());
        this.maxY = Math.max(first.getBlockY(), second.getBlockY());
        this.maxZ = Math.max(first.getBlockZ(), second.getBlockZ());
    }

    /**
     * Checks whether a location falls inside the area, borders included.
     *
     * @param location location to check
     * @return true if it's inside
     */
    public boolean isInArea(Location location) {
        if (location == null || location.getWorld() != this.world) {
            return false;
        }

        final int x = location.getBlockX();
        final int y = location.getBlockY();
        final int z = location.getBlockZ();

        return x >= this.minX && x <= this.maxX && y >= this.minY && y <= this.maxY && z >= this.minZ && z <= this.maxZ;
    }

    /**
     * Same check as {@link #isInArea(Location)} but ignoring the Y axis, handy for columns like islands.
     *
     * @param location location to check
     * @return true if x and z are inside
     */
    public boolean isInColumn(Location location) {
        if (location == null || location.getWorld() != this.world) {
            return false;
        }

        final int x = location.getBlockX();
        final int z = location.getBlockZ();

        return x >= this.minX && x <= this.maxX && z >= this.minZ && z <= this.maxZ;
    }

    /**
     * Returns a copy grown by the given amount of blocks on every side.
     *
     * @param amount blocks to grow, negative shrinks
     * @return the new area
     */
    public Area expand(int amount) {
        return new Area(new Location(this.world, this.minX - amount, this.minY - amount, this.minZ - amount),
                new Location(this.world, this.maxX + amount, this.maxY + amount, this.maxZ + amount));
    }

    /**
     * Every block inside the area. Careful with huge areas.
     *
     * @return block list
     */
    public List<Block> getBlocks() {
        final List<Block> blocks = new ArrayList<>();

        for (Block block : this) {
            blocks.add(block);
        }
        return blocks;
    }

    /**
     * @return how many blocks the area holds
     */
    public long getVolume() {
        return (long) (this.maxX - this.minX + 1) * (this.maxY - this.minY + 1) * (this.maxZ - this.minZ + 1);
    }

    public World getWorld() {
        return this.world;
    }

    public Location getMin() {
        return new Location(this.world, this.minX, this.minY, this.minZ);
    }

    public Location getMax() {
        return new Location(this.world, this.maxX, this.maxY, this.maxZ);
    }

    public Location getCenter() {
        return new Location(this.world, (this.minX + this.maxX + 1) / 2.0D, (this.minY + this.maxY + 1) / 2.0D, (this.minZ + this.maxZ + 1) / 2.0D);
    }

    @Override
    public Iterator<Block> iterator() {
        return new Iterator<Block>() {

            private int x = minX;
            private int y = minY;
            private int z = minZ;

            @Override
            public boolean hasNext() {
                return this.y <= maxY;
            }

            @Override
            public Block next() {
                if (!this.hasNext()) {
                    throw new NoSuchElementException();
                }

                final Block block = world.getBlockAt(this.x, this.y, this.z);

                if (++this.x > maxX) {
                    this.x = minX;

                    if (++this.z > maxZ) {
                        this.z = minZ;
                        this.y++;
                    }
                }
                return block;
            }
        };
    }

}
