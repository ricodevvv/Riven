package dev.ricodevvv.riven.packet;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;

/**
 * Hooks into every player's Netty pipeline so packets can be read or cancelled.
 * Based on Hyrame's PacketInterceptor.
 */
public class PacketInterceptor implements Listener {

    private static final String PACKET_HANDLER_KEY = "packet_handler";

    private final JavaPlugin plugin;
    private final String key;
    private final Map<Class<?>, List<PacketHandler>> handlers = new ConcurrentHashMap<>();

    public PacketInterceptor(JavaPlugin plugin) {
        this.plugin = plugin;
        this.key = plugin.getName().toLowerCase() + "_interceptor";
    }

    /**
     * Starts listening: injects everyone online and everyone who joins.
     */
    public void start() {
        Bukkit.getPluginManager().registerEvents(this, this.plugin);

        for (Player player : Bukkit.getOnlinePlayers()) {
            this.inject(player);
        }
    }

    /**
     * Removes the interceptor from every player.
     */
    public void stop() {
        HandlerList.unregisterAll(this);

        for (Player player : Bukkit.getOnlinePlayers()) {
            this.uninject(player);
        }

        this.handlers.clear();
    }

    /**
     * Registers a handler for a packet class.
     *
     * @param packetClass NMS packet class
     * @param handler handler
     */
    public void addHandler(Class<?> packetClass, PacketHandler handler) {
        this.handlers.computeIfAbsent(packetClass, clazz -> new CopyOnWriteArrayList<>()).add(handler);
    }

    public void removeHandler(PacketHandler handler) {
        for (List<PacketHandler> list : this.handlers.values()) {
            list.remove(handler);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        this.inject(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        this.uninject(event.getPlayer());
    }

    private void inject(Player player) {
        final Channel channel = this.getChannel(player);

        if (channel == null) {
            return;
        }

        channel.eventLoop().execute(() -> {
            final ChannelPipeline pipeline = channel.pipeline();

            if (pipeline.get(PACKET_HANDLER_KEY) != null && pipeline.get(this.key) == null) {
                pipeline.addBefore(PACKET_HANDLER_KEY, this.key, new Handler(player));
            }
        });
    }

    private void uninject(Player player) {
        final Channel channel = this.getChannel(player);

        if (channel == null) {
            return;
        }

        channel.eventLoop().execute(() -> {
            if (channel.pipeline().get(this.key) != null) {
                channel.pipeline().remove(this.key);
            }
        });
    }

    private Channel getChannel(Player player) {
        final CraftPlayer craftPlayer = (CraftPlayer) player;

        if (craftPlayer.getHandle().playerConnection == null) {
            return null;
        }
        return craftPlayer.getHandle().playerConnection.networkManager.channel;
    }

    private boolean handle(Player player, Object packet, boolean receive) {
        final List<PacketHandler> list = this.handlers.get(packet.getClass());

        if (list == null || list.isEmpty()) {
            return false;
        }

        boolean cancelled = false;

        for (PacketHandler handler : list) {
            try {
                cancelled |= receive ? handler.onReceive(player, packet) : handler.onSend(player, packet);
            } catch (Exception e) {
                this.plugin.getLogger().log(Level.SEVERE, "Error handling packet " + packet.getClass().getSimpleName(), e);
            }
        }
        return cancelled;
    }

    private class Handler extends ChannelDuplexHandler {

        private final Player owner;

        Handler(Player owner) {
            this.owner = owner;
        }

        @Override
        public void write(ChannelHandlerContext ctx, Object packet, ChannelPromise promise) throws Exception {
            if (handle(this.owner, packet, false)) {
                return;
            }
            super.write(ctx, packet, promise);
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object packet) throws Exception {
            if (handle(this.owner, packet, true)) {
                return;
            }
            super.channelRead(ctx, packet);
        }

    }

}
