package dev.ricodevvv.riven.hologram;

import dev.ricodevvv.riven.Riven;
import dev.ricodevvv.riven.util.PacketUtil;
import net.minecraft.server.v1_8_R3.EntityArmorStand;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntityDestroy;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntityMetadata;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntityTeleport;
import net.minecraft.server.v1_8_R3.PacketPlayOutSpawnEntityLiving;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Hologram made of fake armor stands (packets only). Every player sees their own copy,
 * so everyone reads it in their language. Port of Hyrame's Hologram, with a visibility condition
 * so in multi-arena only the people in that world see it.
 */
public class Hologram {

    private static final double RANGE_VIEW = 48.0D;

    private final JavaPlugin plugin;
    private final double linesDistance;
    private final Map<Integer, Line> lines;
    private final Predicate<Player> visibility;
    private final Map<UUID, Map<Integer, EntityArmorStand>> viewers = new ConcurrentHashMap<>();
    private final List<BukkitTask> updateTasks = new ArrayList<>();

    private Location location;
    private boolean destroyed;

    protected Hologram(JavaPlugin plugin, Location location, double linesDistance, Map<Integer, Line> lines, Predicate<Player> visibility) {
        this.plugin = plugin;
        this.location = location;
        this.linesDistance = linesDistance;
        this.lines = lines;
        this.visibility = visibility;

        for (Map.Entry<Integer, Line> entry : this.lines.entrySet()) {
            this.startUpdate(entry.getKey(), entry.getValue());
        }

        Riven.get().getHologramManager().register(this);
    }

    private void startUpdate(int index, Line line) {
        final Line.Update update = line.getUpdate();

        if (update == null) {
            return;
        }

        this.updateTasks.add(Bukkit.getScheduler().runTaskTimer(this.plugin, () -> {
            update.getAction().accept(line);
            this.updateLine(index);
        }, update.getTicks(), update.getTicks()));
    }

    /**
     * Called by the manager every half second to spawn or remove the hologram based on distance.
     *
     * @param players online players
     */
    void tick(Collection<? extends Player> players) {
        if (this.destroyed) {
            return;
        }

        for (Player player : players) {
            final boolean shouldSee = this.canSee(player);
            final boolean sees = this.viewers.containsKey(player.getUniqueId());

            if (shouldSee && !sees) {
                this.spawn(player);
            } else if (!shouldSee && sees) {
                this.despawn(player);
            }
        }
    }

    private boolean canSee(Player player) {
        return player.getWorld() == this.location.getWorld()
                && player.getLocation().distanceSquared(this.location) <= RANGE_VIEW * RANGE_VIEW
                && this.visibility.test(player);
    }

    private void spawn(Player player) {
        final Map<Integer, EntityArmorStand> entities = new HashMap<>();
        final Location current = this.location.clone().add(0.0D, this.lines.size() * (this.linesDistance + 0.05D), 0.0D);

        for (Map.Entry<Integer, Line> entry : this.lines.entrySet()) {
            final String value = entry.getValue().getValue(player);

            if (value != null && !value.isEmpty()) {
                final EntityArmorStand armorStand = this.createEntity(current, value);

                entities.put(entry.getKey(), armorStand);

                PacketUtil.sendPacket(player, new PacketPlayOutSpawnEntityLiving(armorStand));
            }

            current.subtract(0.0D, this.linesDistance + 0.05D, 0.0D);
        }

        this.viewers.put(player.getUniqueId(), entities);
    }

    private void despawn(Player player) {
        final Map<Integer, EntityArmorStand> entities = this.viewers.remove(player.getUniqueId());

        if (entities == null || entities.isEmpty()) {
            return;
        }

        final int[] ids = entities.values().stream().mapToInt(EntityArmorStand::getId).toArray();

        PacketUtil.sendPacket(player, new PacketPlayOutEntityDestroy(ids));
    }

    /**
     * Forgets a player without sending packets (the client already drops them when changing worlds).
     *
     * @param playerId player uuid
     */
    void forget(UUID playerId) {
        this.viewers.remove(playerId);
    }

    private EntityArmorStand createEntity(Location location, String text) {
        final EntityArmorStand armorStand = new EntityArmorStand(((CraftWorld) location.getWorld()).getHandle());

        armorStand.setLocation(location.getX(), location.getY(), location.getZ(), 0.0F, 0.0F);
        armorStand.setInvisible(true);
        armorStand.setGravity(false);
        armorStand.setBasePlate(false);
        armorStand.n(true);
        armorStand.setCustomName(text);
        armorStand.setCustomNameVisible(true);

        return armorStand;
    }

    /**
     * Refreshes a line's text for everyone looking at it.
     *
     * @param index line number
     */
    public void updateLine(int index) {
        final Line line = this.lines.get(index);

        if (line == null || this.destroyed) {
            return;
        }

        for (Map.Entry<UUID, Map<Integer, EntityArmorStand>> entry : this.viewers.entrySet()) {
            final Player player = Bukkit.getPlayer(entry.getKey());

            if (player == null) {
                continue;
            }

            final EntityArmorStand armorStand = entry.getValue().get(index);
            final String value = line.getValue(player);

            if (armorStand == null || value == null || value.isEmpty()) {
                this.despawn(player);
                this.spawn(player);
                continue;
            }

            armorStand.setCustomName(value);

            PacketUtil.sendPacket(player, new PacketPlayOutEntityMetadata(armorStand.getId(), armorStand.getDataWatcher(), true));
        }
    }

    public void updateLines() {
        for (int index : this.lines.keySet()) {
            this.updateLine(index);
        }
    }

    /**
     * Changes a line (or adds a new one) and sends it.
     *
     * @param index line number
     * @param line new line
     */
    public void setLine(int index, Line line) {
        final boolean isNew = !this.lines.containsKey(index);

        this.lines.put(index, line);
        this.startUpdate(index, line);

        if (isNew) {
            this.respawnAll();
        } else {
            this.updateLine(index);
        }
    }

    private void respawnAll() {
        for (UUID viewer : new ArrayList<>(this.viewers.keySet())) {
            final Player player = Bukkit.getPlayer(viewer);

            if (player != null) {
                this.despawn(player);
                this.spawn(player);
            }
        }
    }

    /**
     * Moves the hologram.
     *
     * @param location new location
     */
    public void setLocation(Location location) {
        final double offsetY = location.getY() - this.location.getY();

        this.location = location;

        for (Map.Entry<UUID, Map<Integer, EntityArmorStand>> entry : this.viewers.entrySet()) {
            final Player player = Bukkit.getPlayer(entry.getKey());

            for (EntityArmorStand armorStand : entry.getValue().values()) {
                armorStand.setLocation(location.getX(), armorStand.locY + offsetY, location.getZ(), 0.0F, 0.0F);

                PacketUtil.sendPacket(player, new PacketPlayOutEntityTeleport(armorStand));
            }
        }
    }

    /**
     * Removes the hologram for everyone and stops its updates.
     */
    public void destroy() {
        if (this.destroyed) {
            return;
        }

        this.destroyed = true;

        for (BukkitTask task : this.updateTasks) {
            task.cancel();
        }

        for (UUID viewer : new ArrayList<>(this.viewers.keySet())) {
            final Player player = Bukkit.getPlayer(viewer);

            if (player != null) {
                this.despawn(player);
            }
        }

        this.viewers.clear();

        Riven.get().getHologramManager().unregister(this);
    }

    public Location getLocation() {
        return this.location;
    }

    public Map<Integer, Line> getLines() {
        return this.lines;
    }

    public double getLinesDistance() {
        return this.linesDistance;
    }

    public boolean isDestroyed() {
        return this.destroyed;
    }

    /**
     * Hologram line. The text is computed per player.
     */
    public static class Line {

        private Function<Player, String> value;
        private Update update;

        public Line(Function<Player, String> value) {
            this.value = value;
        }

        public Line(Supplier<String> value) {
            this(player -> value.get());
        }

        /**
         * Line with a fixed text.
         *
         * @param text text
         * @return the line
         */
        public static Line of(String text) {
            return new Line(player -> text);
        }

        String getValue(Player player) {
            return this.value.apply(player);
        }

        public Line withValue(Function<Player, String> value) {
            this.value = value;
            return this;
        }

        public Line withUpdate(int ticks, Consumer<Line> action) {
            this.update = new Update(ticks, action);
            return this;
        }

        Update getUpdate() {
            return this.update;
        }

        static class Update {

            private final int ticks;
            private final Consumer<Line> action;

            Update(int ticks, Consumer<Line> action) {
                this.ticks = ticks;
                this.action = action;
            }

            int getTicks() {
                return this.ticks;
            }

            Consumer<Line> getAction() {
                return this.action;
            }

        }

    }

    /**
     * Hologram builder.
     */
    public static class Builder {

        private final JavaPlugin plugin;
        private final Location location;
        private final Map<Integer, Line> lines = new TreeMap<>();
        private double linesDistance = 0.30D;
        private Predicate<Player> visibility = player -> true;

        public Builder(JavaPlugin plugin, Location location) {
            this.plugin = plugin;
            this.location = location;
        }

        public Builder withLine(int index, Line line) {
            this.lines.put(index, line);
            return this;
        }

        public Builder withLine(Line line) {
            return this.withLine(this.lines.size(), line);
        }

        public Builder withLine(String line) {
            return this.withLine(Line.of(line));
        }

        public Builder withLines(List<String> lines) {
            for (String line : lines) {
                this.withLine(line);
            }
            return this;
        }

        public Builder withLinesDistance(double linesDistance) {
            this.linesDistance = linesDistance;
            return this;
        }

        /**
         * Extra condition for a player to see the hologram (on top of being close and in the same world).
         *
         * @param visibility condition
         * @return this builder
         */
        public Builder withVisibility(Predicate<Player> visibility) {
            this.visibility = visibility;
            return this;
        }

        public Hologram build() {
            return new Hologram(this.plugin, this.location, this.linesDistance, this.lines, this.visibility);
        }

    }

}
