package dev.ricodevvv.riven.lobby;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.EnumSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Keeps the lobby clean: no breaking, placing, dropping, hunger, damage, mobs or weather, and falling into the
 * void sends you back to spawn. Each rule can be turned off, and players with the bypass permission in creative
 * are left alone. It can also give flight in the lobby to anyone with the auto-fly permission.
 */
public class LobbyProtection implements Listener {

    private final JavaPlugin plugin;
    private final Set<Rule> rules = EnumSet.allOf(Rule.class);

    private Predicate<World> lobbyWorld = world -> false;
    private Predicate<Player> exempt = player -> false;
    private Supplier<Location> spawn = () -> null;
    private String bypassPermission = "riven.lobby.bypass";
    private String autoFlyPermission;
    private int voidY = 0;

    public LobbyProtection(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public LobbyProtection start() {
        Bukkit.getPluginManager().registerEvents(this, this.plugin);
        return this;
    }

    public void stop() {
        HandlerList.unregisterAll(this);
    }

    /**
     * @param lobbyWorld which worlds count as lobby
     * @return this protection
     */
    public LobbyProtection lobbyWorld(Predicate<World> lobbyWorld) {
        this.lobbyWorld = lobbyWorld;
        return this;
    }

    /**
     * @param exempt players the rules don't apply to even in the lobby (people in setup, for example)
     * @return this protection
     */
    public LobbyProtection exempt(Predicate<Player> exempt) {
        this.exempt = exempt;
        return this;
    }

    /**
     * @param spawn where void falls send you, null disables the void rule
     * @return this protection
     */
    public LobbyProtection spawn(Supplier<Location> spawn) {
        this.spawn = spawn;
        return this;
    }

    public LobbyProtection bypassPermission(String bypassPermission) {
        this.bypassPermission = bypassPermission;
        return this;
    }

    /**
     * @param autoFlyPermission players with it can fly in the lobby, null turns it off
     * @return this protection
     */
    public LobbyProtection autoFlyPermission(String autoFlyPermission) {
        this.autoFlyPermission = autoFlyPermission;
        return this;
    }

    public LobbyProtection voidY(int voidY) {
        this.voidY = voidY;
        return this;
    }

    public LobbyProtection set(Rule rule, boolean enabled) {
        if (enabled) {
            this.rules.add(rule);
        } else {
            this.rules.remove(rule);
        }
        return this;
    }

    public boolean isEnabled(Rule rule) {
        return this.rules.contains(rule);
    }

    private boolean protects(Player player, Rule rule) {
        return this.rules.contains(rule)
                && this.lobbyWorld.test(player.getWorld())
                && !this.exempt.test(player)
                && !(player.getGameMode() == GameMode.CREATIVE && player.hasPermission(this.bypassPermission));
    }

    private boolean protects(World world, Rule rule) {
        return this.rules.contains(rule) && this.lobbyWorld.test(world);
    }

    /**
     * Gives or removes the lobby flight according to the permission.
     *
     * @param player player
     */
    public void applyFlight(Player player) {
        if (this.autoFlyPermission == null || !this.lobbyWorld.test(player.getWorld()) || this.exempt.test(player) || player.getGameMode() == GameMode.CREATIVE) {
            return;
        }

        final boolean fly = player.hasPermission(this.autoFlyPermission);

        player.setAllowFlight(fly);

        if (fly) {
            player.setFlying(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (this.protects(event.getPlayer(), Rule.BLOCK_BREAK)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (this.protects(event.getPlayer(), Rule.BLOCK_PLACE)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (this.protects(event.getPlayer(), Rule.ITEM_DROP)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPickup(PlayerPickupItemEvent event) {
        if (this.protects(event.getPlayer(), Rule.ITEM_PICKUP)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onInventory(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player && event.getClickedInventory() == event.getWhoClicked().getInventory()
                && this.protects((Player) event.getWhoClicked(), Rule.INVENTORY_MOVE)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() != null && this.protects(event.getPlayer(), Rule.BLOCK_INTERACT)) {
            final String type = event.getClickedBlock().getType().name();

            if (type.contains("DOOR") || type.contains("CHEST") || type.contains("FURNACE") || type.contains("PLATE")
                    || type.contains("LEVER") || type.contains("BUTTON") || type.contains("SOIL") || type.contains("HOPPER")
                    || type.contains("DISPENSER") || type.contains("DROPPER") || type.contains("ANVIL") || type.contains("WORKBENCH")
                    || type.contains("ENCHANTMENT") || type.contains("BREWING") || type.contains("BEACON") || type.contains("FENCE_GATE")) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player && this.protects((Player) event.getEntity(), Rule.DAMAGE)) {
            event.setCancelled(true);

            if (event.getCause() == EntityDamageEvent.DamageCause.VOID) {
                this.toSpawn((Player) event.getEntity());
            }
        } else if (!(event.getEntity() instanceof Player) && this.protects(event.getEntity().getWorld(), Rule.ENTITY_DAMAGE)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onHunger(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player && this.protects((Player) event.getEntity(), Rule.HUNGER)) {
            event.setCancelled(true);
            ((Player) event.getEntity()).setFoodLevel(20);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onSpawn(CreatureSpawnEvent event) {
        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.CUSTOM && this.protects(event.getLocation().getWorld(), Rule.MOB_SPAWNING)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onWeather(WeatherChangeEvent event) {
        if (event.toWeatherState() && this.protects(event.getWorld(), Rule.WEATHER)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onExplode(EntityExplodeEvent event) {
        if (this.protects(event.getLocation().getWorld(), Rule.EXPLOSIONS)) {
            event.blockList().clear();
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onHanging(HangingBreakEvent event) {
        if (this.protects(event.getEntity().getWorld(), Rule.BLOCK_BREAK)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBurn(BlockBurnEvent event) {
        if (this.protects(event.getBlock().getWorld(), Rule.WORLD_CHANGES)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onSpread(BlockSpreadEvent event) {
        if (this.protects(event.getBlock().getWorld(), Rule.WORLD_CHANGES)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onFade(BlockFadeEvent event) {
        if (this.protects(event.getBlock().getWorld(), Rule.WORLD_CHANGES)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onDecay(LeavesDecayEvent event) {
        if (this.protects(event.getBlock().getWorld(), Rule.WORLD_CHANGES)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo().getBlockY() < this.voidY && event.getFrom().getBlockY() >= this.voidY && this.protects(event.getPlayer(), Rule.VOID)) {
            this.toSpawn(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
            if (event.getPlayer().isOnline()) {
                this.applyFlight(event.getPlayer());
            }
        }, 5L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        this.applyFlight(event.getPlayer());
    }

    private void toSpawn(Entity entity) {
        final Location location = this.spawn.get();

        if (location != null && this.rules.contains(Rule.VOID)) {
            entity.setFallDistance(0.0F);
            entity.teleport(location);
        }
    }

    /**
     * Things the protection can block.
     */
    public enum Rule {
        BLOCK_BREAK,
        BLOCK_PLACE,
        BLOCK_INTERACT,
        ITEM_DROP,
        ITEM_PICKUP,
        INVENTORY_MOVE,
        DAMAGE,
        ENTITY_DAMAGE,
        HUNGER,
        MOB_SPAWNING,
        WEATHER,
        EXPLOSIONS,
        WORLD_CHANGES,
        VOID
    }

}
