package dev.ricodevvv.riven.input;

import dev.ricodevvv.riven.packet.PacketHandler;
import dev.ricodevvv.riven.packet.PacketInterceptor;
import dev.ricodevvv.riven.util.PacketUtil;
import net.minecraft.server.v1_8_R3.BlockPosition;
import net.minecraft.server.v1_8_R3.IChatBaseComponent;
import net.minecraft.server.v1_8_R3.PacketPlayInUpdateSign;
import net.minecraft.server.v1_8_R3.PacketPlayOutBlockChange;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Listens to the signs the client sends back and answers the {@link SignInput} that opened them.
 */
public class SignInputManager {

    private final JavaPlugin plugin;
    private final PacketInterceptor packetInterceptor;
    private final Map<UUID, Entry> inputs = new ConcurrentHashMap<>();
    private PacketHandler handler;

    public SignInputManager(JavaPlugin plugin, PacketInterceptor packetInterceptor) {
        this.plugin = plugin;
        this.packetInterceptor = packetInterceptor;
    }

    public void start() {
        this.handler = new PacketHandler() {
            @Override
            public boolean onReceive(Player player, Object packet) {
                final Entry entry = inputs.remove(player.getUniqueId());

                if (entry == null) {
                    return false;
                }

                final IChatBaseComponent[] components = ((PacketPlayInUpdateSign) packet).b();
                final String[] lines = new String[4];

                for (int i = 0; i < lines.length; i++) {
                    lines[i] = i < components.length && components[i] != null ? components[i].c() : "";
                }

                Bukkit.getScheduler().runTask(plugin, () -> {
                    final PacketPlayOutBlockChange blockChange = new PacketPlayOutBlockChange(((CraftWorld) player.getWorld()).getHandle(), entry.position);

                    PacketUtil.sendPacket(player, blockChange);

                    entry.input.getCallback().call(player, lines);
                });
                return true;
            }
        };

        this.packetInterceptor.addHandler(PacketPlayInUpdateSign.class, this.handler);
    }

    public void stop() {
        if (this.handler != null) {
            this.packetInterceptor.removeHandler(this.handler);
        }

        this.inputs.clear();
    }

    void register(UUID playerId, SignInput input, BlockPosition position) {
        this.inputs.put(playerId, new Entry(input, position));
    }

    private static class Entry {

        private final SignInput input;
        private final BlockPosition position;

        Entry(SignInput input, BlockPosition position) {
            this.input = input;
            this.position = position;
        }

    }

}
