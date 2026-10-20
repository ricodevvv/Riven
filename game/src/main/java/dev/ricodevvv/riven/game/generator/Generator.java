package dev.ricodevvv.riven.game.generator;

import dev.ricodevvv.riven.game.GameFramework;
import dev.ricodevvv.riven.game.generator.event.GeneratorCreatedEvent;
import dev.ricodevvv.riven.game.generator.event.GeneratorDropEvent;
import dev.ricodevvv.riven.game.generator.event.GeneratorRemovedEvent;
import dev.ricodevvv.riven.game.generator.event.GeneratorUpgradedEvent;
import dev.ricodevvv.riven.item.ItemNBT;
import dev.ricodevvv.riven.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Resource generator. Port of HyriGenerator: drops an item every X ticks up to a limit,
 * can be upgraded and, if the tier allows it, splits pickups with nearby players.
 */
public class Generator {

    public static final String ITEM_TAG = "GeneratorItem";

    private final String id = UUID.randomUUID().toString().substring(0, 8);
    private final JavaPlugin plugin;
    private final Location location;
    private final GeneratorHeader header;
    private final GeneratorAnimation animation;
    private final Predicate<Player> pickupFilter;
    private final BiPredicate<Player, Player> splitFilter;
    private final long firstSpawnTime;

    private ItemStack item;
    private GeneratorTier tier;
    private BukkitTask spawnTask;
    private long nextSpawnAt;
    private boolean upgraded;
    private boolean created;

    protected Generator(Builder builder) {
        this.plugin = builder.plugin;
        this.location = builder.location;
        this.header = builder.header;
        this.animation = builder.animation;
        this.pickupFilter = builder.pickupFilter;
        this.splitFilter = builder.splitFilter;
        this.firstSpawnTime = builder.firstSpawnTime;
        this.item = builder.item.clone();
        this.tier = builder.tier;
    }

    /**
     * Starts the generator: header, animation and timer.
     */
    public void create() {
        if (this.created) {
            return;
        }

        final GeneratorCreatedEvent event = new GeneratorCreatedEvent(this);

        Bukkit.getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            return;
        }

        this.created = true;

        GameFramework.get().getGeneratorManager().register(this);

        if (this.header != null) {
            this.header.onCreate(this);
        }

        if (this.animation != null) {
            this.animation.start();
        }

        this.restartSpawnTask(this.firstSpawnTime > 0 ? this.firstSpawnTime : this.tier.getTimeBetweenSpawns());
    }

    private void restartSpawnTask(long initial) {
        if (this.spawnTask != null) {
            this.spawnTask.cancel();
        }

        this.nextSpawnAt = System.currentTimeMillis() + initial * 50L;
        this.spawnTask = Bukkit.getScheduler().runTaskTimer(this.plugin, this::spawn, initial, this.tier.getTimeBetweenSpawns());
    }

    private void spawn() {
        this.nextSpawnAt = System.currentTimeMillis() + this.tier.getTimeBetweenSpawns() * 50L;

        final GeneratorDropEvent event = new GeneratorDropEvent(this, this.item.clone());

        Bukkit.getPluginManager().callEvent(event);

        if (!event.isCancelled() && !this.isFull()) {
            ItemUtil.dropItem(this.location, new ItemNBT(event.getItem()).setString(ITEM_TAG, this.id).build());
        }

        if (this.header != null) {
            this.header.onDrop(this);
        }

        if (this.upgraded) {
            this.upgraded = false;
            this.restartSpawnTask(this.tier.getTimeBetweenSpawns());
        }
    }

    private boolean isFull() {
        if (this.tier.getSpawnLimit() < 0) {
            return false;
        }

        int count = 0;

        for (Entity entity : this.location.getWorld().getNearbyEntities(this.location, 3.0D, 3.0D, 3.0D)) {
            if (entity instanceof Item && this.id.equals(new ItemNBT(((Item) entity).getItemStack()).getString(ITEM_TAG))) {
                count += ((Item) entity).getItemStack().getAmount();
            }
        }
        return count >= this.tier.getSpawnLimit();
    }

    /**
     * Called by the {@link GeneratorManager} when someone picks up an item from this generator.
     *
     * @param event pickup event
     */
    void handlePickup(PlayerPickupItemEvent event) {
        final Player player = event.getPlayer();
        final Item droppedItem = event.getItem();

        if (!this.pickupFilter.test(player)) {
            event.setCancelled(true);
            return;
        }

        final ItemStack clean = new ItemNBT(droppedItem.getItemStack()).removeTag(ITEM_TAG).build();

        if (this.tier.isSplitting()) {
            final List<Player> others = new ArrayList<>();

            for (Entity entity : droppedItem.getNearbyEntities(2.0D, 2.0D, 2.0D)) {
                if (entity instanceof Player && entity != player && this.pickupFilter.test((Player) entity) && this.splitFilter.test(player, (Player) entity)) {
                    others.add((Player) entity);
                }
            }

            for (Player other : others) {
                other.getInventory().addItem(clean.clone());
                other.playSound(other.getLocation(), Sound.ITEM_PICKUP, 0.2F, 1.0F);
            }
        }

        droppedItem.setItemStack(clean);
    }

    /**
     * Changes the tier. The new pace kicks in from the next drop.
     *
     * @param tier new tier
     */
    public void upgrade(GeneratorTier tier) {
        final GeneratorUpgradedEvent event = new GeneratorUpgradedEvent(this, tier);

        Bukkit.getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            return;
        }

        this.tier = tier;
        this.upgraded = true;

        if (this.header != null) {
            this.header.onUpgrade(this);
        }
    }

    /**
     * Stops the generator and removes everything visual.
     */
    public void remove() {
        if (!this.created) {
            return;
        }

        Bukkit.getPluginManager().callEvent(new GeneratorRemovedEvent(this));

        this.created = false;

        GameFramework.get().getGeneratorManager().unregister(this);

        if (this.spawnTask != null) {
            this.spawnTask.cancel();
        }

        if (this.header != null) {
            this.header.onRemove(this);
        }

        if (this.animation != null) {
            this.animation.stop();
        }
    }

    /**
     * @return seconds left until the next drop
     */
    public long getSecondsBeforeSpawn() {
        return Math.max(0L, (this.nextSpawnAt - System.currentTimeMillis() + 999L) / 1000L);
    }

    public String getId() {
        return this.id;
    }

    public Location getLocation() {
        return this.location;
    }

    public ItemStack getItem() {
        return this.item.clone();
    }

    public void setItem(ItemStack item) {
        this.item = item.clone();
    }

    public GeneratorTier getTier() {
        return this.tier;
    }

    public GeneratorHeader getHeader() {
        return this.header;
    }

    public boolean isCreated() {
        return this.created;
    }

    /**
     * Generator builder.
     */
    public static class Builder {

        private final JavaPlugin plugin;
        private final Location location;
        private final GeneratorTier tier;

        private ItemStack item;
        private GeneratorHeader header;
        private GeneratorAnimation animation;
        private Predicate<Player> pickupFilter = player -> true;
        private BiPredicate<Player, Player> splitFilter = (picker, other) -> true;
        private long firstSpawnTime;

        public Builder(JavaPlugin plugin, Location location, GeneratorTier tier) {
            this.plugin = plugin;
            this.location = location;
            this.tier = tier;
        }

        public Builder withItem(ItemStack item) {
            this.item = item;
            return this;
        }

        public Builder withHeader(GeneratorHeader header) {
            this.header = header;
            return this;
        }

        /**
         * Sets Hyrame's header (hologram with name, tier and time) and the spinning block animation.
         *
         * @param block block spinning on top
         * @param name generator name per player
         * @param visibility who sees the hologram
         * @return this builder
         */
        public Builder withDefaultHeader(ItemStack block, Function<Player, String> name, Predicate<Player> visibility) {
            this.header = new DefaultGeneratorHeader(this.plugin, block, name, visibility);
            this.animation = new GeneratorAnimation.Default(this.plugin, this.location.clone().add(0.0D, 0.75D, 0.0D), block);
            return this;
        }

        public Builder withAnimation(GeneratorAnimation animation) {
            this.animation = animation;
            return this;
        }

        /**
         * Who can pick up what the generator drops.
         *
         * @param pickupFilter condition
         * @return this builder
         */
        public Builder withPickupFilter(Predicate<Player> pickupFilter) {
            this.pickupFilter = pickupFilter;
            return this;
        }

        /**
         * Who gets a share of the pickups when the tier splits them (picker, other player).
         *
         * @param splitFilter condition
         * @return this builder
         */
        public Builder withSplitFilter(BiPredicate<Player, Player> splitFilter) {
            this.splitFilter = splitFilter;
            return this;
        }

        public Builder withFirstSpawnTime(long firstSpawnTime) {
            this.firstSpawnTime = firstSpawnTime;
            return this;
        }

        public Generator build() {
            if (this.item == null) {
                throw new IllegalStateException("The generator needs an item");
            }
            return new Generator(this);
        }

    }

}
