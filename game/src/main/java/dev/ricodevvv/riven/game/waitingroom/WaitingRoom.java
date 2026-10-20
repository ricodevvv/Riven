package dev.ricodevvv.riven.game.waitingroom;

import com.mojang.authlib.properties.Property;
import dev.ricodevvv.riven.game.GameFramework;
import dev.ricodevvv.riven.game.Game;
import dev.ricodevvv.riven.game.GamePlayer;
import dev.ricodevvv.riven.game.event.GameJoinEvent;
import dev.ricodevvv.riven.game.event.GameLeaveEvent;
import dev.ricodevvv.riven.inventory.GameInventory;
import dev.ricodevvv.riven.item.ItemBuilder;
import dev.ricodevvv.riven.npc.NPC;
import dev.ricodevvv.riven.npc.Skin;
import dev.ricodevvv.riven.util.Area;
import dev.ricodevvv.riven.util.LocationWrapper;
import dev.ricodevvv.riven.util.Symbols;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Waiting room of a game. Port of HyriWaitingRoom: teleports people in, brings them back if they fall,
 * shows an NPC with your own skin that opens your stats, and removes the room when the game starts.
 */
public class WaitingRoom {

    private static final int MIN_SLOT = 9;
    private static final int MAX_SLOT = 35;

    protected final Game<?> game;
    protected final Config config;
    protected final ItemStack icon;
    protected boolean clearBlocks = true;

    private final Map<Integer, NPCCategory> categories = new TreeMap<>();
    private final Map<UUID, NPC> npcs = new ConcurrentHashMap<>();
    private final Handler handler = new Handler();
    private boolean setup;

    public WaitingRoom(Game<?> game, ItemStack icon, Config config) {
        this.game = game;
        this.icon = icon;
        this.config = config;
    }

    /**
     * Sets the room up: listeners and NPCs for whoever is already in.
     */
    public void setup() {
        if (this.setup) {
            return;
        }

        this.setup = true;

        Bukkit.getPluginManager().registerEvents(this.handler, this.game.getPlugin());

        for (GamePlayer gamePlayer : this.game.getOnlinePlayers()) {
            this.createNPC(gamePlayer.getPlayer());
        }
    }

    /**
     * Tears the room down: removes the NPCs and, if enabled, the blocks of the area.
     */
    public void remove() {
        if (!this.setup) {
            return;
        }

        this.setup = false;

        HandlerList.unregisterAll(this.handler);

        for (NPC npc : this.npcs.values()) {
            GameFramework.get().getNPCManager().remove(npc);
        }

        this.npcs.clear();

        if (this.clearBlocks && this.config.getFirstPos() != null && this.config.getSecondPos() != null) {
            final Area area = new Area(this.config.getFirstPos().asBukkit(this.game.getWorld()), this.config.getSecondPos().asBukkit(this.game.getWorld()));

            for (Block block : area) {
                if (block.getType() != Material.AIR) {
                    block.setType(Material.AIR, false);
                }
            }
        }
    }

    /**
     * Teleports every player of the game to the room spawn.
     */
    public void teleportPlayers() {
        for (GamePlayer gamePlayer : this.game.getOnlinePlayers()) {
            gamePlayer.getPlayer().teleport(this.getSpawn());
        }
    }

    /**
     * @return the room spawn in the game's world
     */
    public Location getSpawn() {
        return this.config.getSpawn().asBukkit(this.game.getWorld());
    }

    private void createNPC(Player player) {
        if (this.config.getNpcLocation() == null || this.categories.isEmpty() || this.npcs.containsKey(player.getUniqueId())) {
            return;
        }

        Skin skin = null;

        for (Property property : ((CraftPlayer) player).getProfile().getProperties().get("textures")) {
            skin = new Skin(property.getValue(), property.getSignature());
        }

        final UUID playerId = player.getUniqueId();
        final List<Function<Player, String>> lines = new ArrayList<>();

        for (String ignored : GameFramework.get().getLanguage().getList(player, "waiting-room.npc.display")) {
            final int index = lines.size();

            lines.add(target -> {
                final List<String> display = GameFramework.get().getLanguage().getList(target, "waiting-room.npc.display");

                return index < display.size() ? display.get(index).replace("%game%", this.game.getDisplayName()) : "";
            });
        }

        final NPC npc = GameFramework.get().getNPCManager().create(this.config.getNpcLocation().asBukkit(this.game.getWorld()), skin, lines, target -> target.getUniqueId().equals(playerId))
                .setTrackingPlayer(true)
                .setInteractCallback((rightClick, clicker) -> {
                    if (this.game.getPlayer(clicker) != null) {
                        new StatisticsGUI(clicker).open();
                    }
                });

        this.npcs.put(playerId, npc);
    }

    private void removeNPC(UUID playerId) {
        final NPC npc = this.npcs.remove(playerId);

        if (npc != null) {
            GameFramework.get().getNPCManager().remove(npc);
        }
    }

    /**
     * Adds a category to the NPC stats menu.
     *
     * @param slot menu slot (between 9 and 35)
     * @param category category
     * @return this room
     */
    public WaitingRoom addNPCCategory(int slot, NPCCategory category) {
        if (slot < MIN_SLOT || slot > MAX_SLOT) {
            throw new IllegalArgumentException("The slot has to be between " + MIN_SLOT + " y " + MAX_SLOT);
        }

        this.categories.put(slot, category);
        return this;
    }

    public boolean isSetup() {
        return this.setup;
    }

    public Config getConfig() {
        return this.config;
    }

    public void setClearBlocks(boolean clearBlocks) {
        this.clearBlocks = clearBlocks;
    }

    /**
     * Category of the stats menu (for example "Solo", "Doubles", "Total").
     */
    public static class NPCCategory {

        private final Function<Player, String> name;
        private final List<NPCData> data = new ArrayList<>();
        private ItemStack icon = new ItemStack(Material.PAPER);

        public NPCCategory(Function<Player, String> name) {
            this.name = name;
        }

        public NPCCategory withIcon(ItemStack icon) {
            this.icon = icon;
            return this;
        }

        public NPCCategory addData(NPCData data) {
            this.data.add(data);
            return this;
        }

        public List<NPCData> getData() {
            return Collections.unmodifiableList(this.data);
        }

    }

    /**
     * One entry of a category: name and value, both per player.
     */
    public static class NPCData {

        private final Function<Player, String> prefix;
        private final Function<Player, String> value;

        public NPCData(Function<Player, String> prefix, Function<Player, String> value) {
            this.prefix = prefix;
            this.value = value;
        }

        /**
         * @return a blank line to separate
         */
        public static NPCData empty() {
            return new NPCData(null, null);
        }

    }

    /**
     * Waiting room locations.
     */
    public static class Config {

        private final LocationWrapper spawn;
        private final LocationWrapper firstPos;
        private final LocationWrapper secondPos;
        private final LocationWrapper npcLocation;

        /**
         * @param spawn where players appear
         * @param firstPos one corner of the room (to remove it)
         * @param secondPos the other corner
         * @param npcLocation where the stats NPC goes, can be null
         */
        public Config(LocationWrapper spawn, LocationWrapper firstPos, LocationWrapper secondPos, LocationWrapper npcLocation) {
            this.spawn = spawn;
            this.firstPos = firstPos;
            this.secondPos = secondPos;
            this.npcLocation = npcLocation;
        }

        public LocationWrapper getSpawn() {
            return this.spawn;
        }

        public LocationWrapper getFirstPos() {
            return this.firstPos;
        }

        public LocationWrapper getSecondPos() {
            return this.secondPos;
        }

        public LocationWrapper getNpcLocation() {
            return this.npcLocation;
        }

    }

    private class StatisticsGUI extends GameInventory {

        StatisticsGUI(Player owner) {
            super(owner, lang(owner, "waiting-room.gui.name"), 45);

            this.setHorizontalLine(0, 8, pane(9));
            this.setHorizontalLine(36, 44, pane(9));

            this.setItem(4, new ItemBuilder(icon)
                    .withName(lang(owner, "waiting-room.gui.item.name").replace("%game%", game.getDisplayName()))
                    .withLore(lang(owner, "waiting-room.gui.item.lore"))
                    .withAllItemFlags()
                    .build());

            for (Map.Entry<Integer, NPCCategory> entry : categories.entrySet()) {
                final NPCCategory category = entry.getValue();
                final List<String> lore = new ArrayList<>();

                for (NPCData data : category.data) {
                    if (data.prefix == null || data.value == null) {
                        lore.add("");
                        continue;
                    }

                    lore.add(ChatColor.DARK_GRAY + Symbols.DOT_BOLD + " " + ChatColor.GRAY + data.prefix.apply(owner) + ": " + ChatColor.AQUA + data.value.apply(owner));
                }

                this.setItem(entry.getKey(), new ItemBuilder(category.icon)
                        .withName(lang(owner, "waiting-room.gui.item.name").replace("%game%", category.name.apply(owner)))
                        .withLore(lore)
                        .withAllItemFlags()
                        .build());
            }
        }

    }

    private class Handler implements Listener {

        @EventHandler
        public void onJoin(GameJoinEvent event) {
            if (event.getGame() != game) {
                return;
            }

            final Player player = event.getGamePlayer().getPlayer();

            player.teleport(getSpawn());

            createNPC(player);
        }

        @EventHandler
        public void onLeave(GameLeaveEvent event) {
            if (event.getGame() == game) {
                removeNPC(event.getGamePlayer().getUniqueId());
            }
        }

        @EventHandler
        public void onMove(PlayerMoveEvent event) {
            if (config.getFirstPos() == null || config.getSecondPos() == null || !game.getState().isAccessible()) {
                return;
            }

            final double minY = Math.min(config.getFirstPos().getY(), config.getSecondPos().getY());

            if (event.getTo().getY() < minY - 5 && game.contains(event.getPlayer())) {
                event.getPlayer().teleport(getSpawn());
            }
        }

    }

}
