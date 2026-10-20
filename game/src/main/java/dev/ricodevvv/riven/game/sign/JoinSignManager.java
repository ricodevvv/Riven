package dev.ricodevvv.riven.game.sign;

import dev.ricodevvv.riven.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.logging.Level;

/**
 * Join signs with rotation. Each sign belongs to a group (SOLO, TEAM...) and shows one game of that group.
 * While its game is joinable it stays on it; once the game starts the sign keeps showing it "in game" until
 * there's a free game no other sign is showing, then it moves on. With 30 games and 20 signs, signs keep
 * flowing as games start.
 *
 * <p>Signs live in a yml file, formats come from config as {@code state -> 4 lines}; the {@code none} state
 * is used when the group has no game at all.</p>
 */
public class JoinSignManager implements Listener {

    private final JavaPlugin plugin;
    private final File file;
    private final Map<String, JoinSign> signs = new LinkedHashMap<>();
    private final Map<String, List<String>> formats = new ConcurrentHashMap<>();
    private final Map<UUID, String> placing = new ConcurrentHashMap<>();

    private Function<String, List<? extends SignTarget>> targets = group -> Collections.emptyList();
    private BiConsumer<Player, SignTarget> joinHandler = (player, target) -> {};
    private String permission = "riven.sign";
    private BukkitTask task;

    /**
     * @param plugin owner plugin
     * @param fileName file inside the plugin folder, like "signs.yml"
     */
    public JoinSignManager(JavaPlugin plugin, String fileName) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), fileName);

        this.formats.put("none", lines("&8[&bJoin&8]", "&7%group%", "&cNo games", ""));
        this.formats.put("waiting", lines("&8[&bJoin&8]", "&a%map%", "&7%players%/%max%", "&aWaiting"));
        this.formats.put("starting", lines("&8[&bJoin&8]", "&a%map%", "&7%players%/%max%", "&eStarting"));
        this.formats.put("full", lines("&8[&bJoin&8]", "&a%map%", "&7%players%/%max%", "&cFull"));
        this.formats.put("playing", lines("&8[&bJoin&8]", "&a%map%", "&7%players%/%max%", "&cIn game"));
        this.formats.put("restarting", lines("&8[&bJoin&8]", "&a%map%", "", "&8Restarting"));
    }

    private static List<String> lines(String... lines) {
        final List<String> result = new ArrayList<>();

        for (String line : lines) {
            result.add(line);
        }
        return result;
    }

    /**
     * @param targets games of a group, in order of preference
     * @return this manager
     */
    public JoinSignManager targets(Function<String, List<? extends SignTarget>> targets) {
        this.targets = targets;
        return this;
    }

    /**
     * @param joinHandler what happens when someone clicks a sign with a target
     * @return this manager
     */
    public JoinSignManager onJoin(BiConsumer<Player, SignTarget> joinHandler) {
        this.joinHandler = joinHandler;
        return this;
    }

    /**
     * @param permission permission to place and break signs
     * @return this manager
     */
    public JoinSignManager permission(String permission) {
        this.permission = permission;
        return this;
    }

    /**
     * Reads the line formats from a section with one list of 4 lines per state.
     *
     * @param section formats section, can be null
     * @return this manager
     */
    public JoinSignManager formats(ConfigurationSection section) {
        if (section == null) {
            return this;
        }

        for (String state : section.getKeys(false)) {
            this.formats.put(state.toLowerCase(), new ArrayList<>(section.getStringList(state)));
        }
        return this;
    }

    /**
     * Loads the signs and starts updating them every {@code period} ticks.
     *
     * @param period update period in ticks
     */
    public void start(long period) {
        this.load();

        Bukkit.getPluginManager().registerEvents(this, this.plugin);

        this.task = Bukkit.getScheduler().runTaskTimer(this.plugin, this::update, 20L, period);
    }

    public void stop() {
        HandlerList.unregisterAll(this);

        if (this.task != null) {
            this.task.cancel();
        }
    }

    /**
     * Reloads signs from the file.
     */
    public void load() {
        this.signs.clear();

        final YamlConfiguration config = YamlConfiguration.loadConfiguration(this.file);

        for (String raw : config.getStringList("signs")) {
            final String[] parts = raw.split(";");

            if (parts.length < 5) {
                continue;
            }

            try {
                final JoinSign sign = new JoinSign(parts[0], Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]), parts[4]);

                this.signs.put(sign.getKey(), sign);
            } catch (NumberFormatException ignored) {
            }
        }
    }

    public void save() {
        final YamlConfiguration config = new YamlConfiguration();
        final List<String> list = new ArrayList<>();

        for (JoinSign sign : this.signs.values()) {
            list.add(sign.getKey() + ";" + sign.getGroup());
        }

        config.set("signs", list);

        try {
            config.save(this.file);
        } catch (IOException e) {
            this.plugin.getLogger().log(Level.SEVERE, "Couldn't save " + this.file.getName(), e);
        }
    }

    /**
     * Puts a player in placing mode: the next sign they right click becomes a join sign of that group.
     *
     * @param player player
     * @param group group, like SOLO
     */
    public void startPlacing(Player player, String group) {
        this.placing.put(player.getUniqueId(), group.toUpperCase());
    }

    public boolean cancelPlacing(Player player) {
        return this.placing.remove(player.getUniqueId()) != null;
    }

    public boolean isPlacing(Player player) {
        return this.placing.containsKey(player.getUniqueId());
    }

    /**
     * Registers a sign right away.
     *
     * @param location sign block
     * @param group group
     * @return the new sign
     */
    public JoinSign add(Location location, String group) {
        final JoinSign sign = new JoinSign(location, group);

        this.signs.put(sign.getKey(), sign);
        this.save();
        this.update();
        return sign;
    }

    public boolean remove(Location location) {
        final JoinSign sign = this.getSign(location);

        if (sign == null) {
            return false;
        }

        this.signs.remove(sign.getKey());
        this.save();
        return true;
    }

    public JoinSign getSign(Location location) {
        for (JoinSign sign : this.signs.values()) {
            if (sign.isAt(location)) {
                return sign;
            }
        }
        return null;
    }

    public List<JoinSign> getSigns() {
        return new ArrayList<>(this.signs.values());
    }

    /**
     * Rotates and redraws every sign. Runs on its own every period; call it to force an update.
     */
    public void update() {
        final Map<String, List<JoinSign>> byGroup = new LinkedHashMap<>();

        for (JoinSign sign : this.signs.values()) {
            byGroup.computeIfAbsent(sign.getGroup(), group -> new ArrayList<>()).add(sign);
        }

        for (Map.Entry<String, List<JoinSign>> entry : byGroup.entrySet()) {
            this.updateGroup(entry.getKey(), entry.getValue());
        }
    }

    private void updateGroup(String group, List<JoinSign> groupSigns) {
        final List<? extends SignTarget> available = this.targets.apply(group);
        final Map<String, SignTarget> byId = new LinkedHashMap<>();

        for (SignTarget target : available) {
            byId.put(target.getId(), target);
        }

        final Set<String> shown = new HashSet<>();

        for (JoinSign sign : groupSigns) {
            final SignTarget current = sign.getTargetId() == null ? null : byId.get(sign.getTargetId());

            if (current == null || shown.contains(current.getId())) {
                sign.setTargetId(null);
            } else if (current.isJoinable()) {
                shown.add(current.getId());
            }
        }

        for (JoinSign sign : groupSigns) {
            final SignTarget current = sign.getTargetId() == null ? null : byId.get(sign.getTargetId());

            if (current != null && current.isJoinable()) {
                continue;
            }

            SignTarget free = null;

            for (SignTarget target : available) {
                if (target.isJoinable() && !shown.contains(target.getId())) {
                    free = target;
                    break;
                }
            }

            if (free != null) {
                sign.setTargetId(free.getId());
                shown.add(free.getId());
            }
        }

        for (JoinSign sign : groupSigns) {
            this.draw(sign, sign.getTargetId() == null ? null : byId.get(sign.getTargetId()));
        }
    }

    private void draw(JoinSign sign, SignTarget target) {
        final Location location = sign.getLocation();

        if (location == null || !location.getChunk().isLoaded()) {
            return;
        }

        final BlockState state = location.getBlock().getState();

        if (!(state instanceof Sign)) {
            return;
        }

        final List<String> format = this.formats.getOrDefault(target == null ? "none" : target.getState().toLowerCase(), this.formats.get("none"));
        final Sign bukkitSign = (Sign) state;
        boolean changed = false;

        for (int i = 0; i < 4; i++) {
            String line = i < format.size() ? format.get(i) : "";

            line = line.replace("%group%", sign.getGroup());

            if (target != null) {
                line = target.format(line);
            }

            line = TextUtil.colorize(line);

            if (line.length() > 15) {
                line = line.substring(0, 15);
            }

            if (!line.equals(bukkitSign.getLine(i))) {
                bukkitSign.setLine(i, line);
                changed = true;
            }
        }

        if (changed) {
            bukkitSign.update(true, false);
        }
    }

    /**
     * @param sign sign
     * @return the game the sign shows right now, or null
     */
    public SignTarget getTarget(JoinSign sign) {
        if (sign.getTargetId() == null) {
            return null;
        }

        for (SignTarget target : this.targets.apply(sign.getGroup())) {
            if (target.getId().equals(sign.getTargetId())) {
                return target;
            }
        }
        return null;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        final Block block = event.getClickedBlock();

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || block == null || !(block.getState() instanceof Sign)) {
            return;
        }

        final Player player = event.getPlayer();
        final String group = this.placing.remove(player.getUniqueId());

        if (group != null) {
            event.setCancelled(true);
            this.add(block.getLocation(), group);
            player.sendMessage(TextUtil.colorize("&aJoin sign for &e" + group + " &aadded."));
            return;
        }

        final JoinSign sign = this.getSign(block.getLocation());

        if (sign == null) {
            return;
        }

        event.setCancelled(true);

        final SignTarget target = this.getTarget(sign);

        if (target != null && target.isJoinable()) {
            this.joinHandler.accept(player, target);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        final JoinSign sign = this.getSign(event.getBlock().getLocation());

        if (sign == null) {
            return;
        }

        if (!event.getPlayer().hasPermission(this.permission) || !event.getPlayer().isSneaking()) {
            event.setCancelled(true);
            return;
        }

        this.remove(event.getBlock().getLocation());
        event.getPlayer().sendMessage(TextUtil.colorize("&cJoin sign removed."));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        this.placing.remove(event.getPlayer().getUniqueId());
    }

}
