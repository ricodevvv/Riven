package dev.ricodevvv.riven.input;

import dev.ricodevvv.riven.Riven;
import dev.ricodevvv.riven.util.PacketUtil;
import net.minecraft.server.v1_8_R3.BlockPosition;
import net.minecraft.server.v1_8_R3.Blocks;
import net.minecraft.server.v1_8_R3.IChatBaseComponent;
import net.minecraft.server.v1_8_R3.PacketPlayOutBlockChange;
import net.minecraft.server.v1_8_R3.PacketPlayOutOpenSignEditor;
import net.minecraft.server.v1_8_R3.TileEntitySign;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R3.block.CraftSign;
import org.bukkit.entity.Player;

/**
 * Opens the editor of a fake sign to ask the player for some text. Port of Hyrame's SignGUI.
 * Whatever they type gets to the callback.
 */
public class SignInput {

    private final Callback callback;
    private String[] lines = {"", "", "", ""};

    public SignInput(Callback callback) {
        this.callback = callback;
    }

    /**
     * Sets the four starting lines of the sign.
     *
     * @param lines exactly four lines
     * @return this input
     */
    public SignInput withLines(String... lines) {
        if (lines.length != 4) {
            throw new IllegalArgumentException("A sign has 4 lines");
        }

        this.lines = lines;
        return this;
    }

    /**
     * Opens the editor for the player.
     *
     * @param player player
     */
    public void open(Player player) {
        final Location location = player.getLocation();
        final BlockPosition position = new BlockPosition(location.getBlockX(), Math.max(0, location.getBlockY() - 5), location.getBlockZ());
        final PacketPlayOutBlockChange blockChange = new PacketPlayOutBlockChange(((CraftWorld) player.getWorld()).getHandle(), position);

        blockChange.block = Blocks.STANDING_SIGN.getBlockData();

        PacketUtil.sendPacket(player, blockChange);

        final IChatBaseComponent[] components = CraftSign.sanitizeLines(this.lines);
        final TileEntitySign sign = new TileEntitySign();

        sign.a(position);

        System.arraycopy(components, 0, sign.lines, 0, sign.lines.length);

        PacketUtil.sendPacket(player, sign.getUpdatePacket());
        PacketUtil.sendPacket(player, new PacketPlayOutOpenSignEditor(position));

        Riven.get().getSignInputManager().register(player.getUniqueId(), this, position);
    }

    Callback getCallback() {
        return this.callback;
    }

    /**
     * Sign answer.
     */
    @FunctionalInterface
    public interface Callback {

        /**
         * @param player player who typed
         * @param lines the four lines they left
         */
        void call(Player player, String[] lines);

    }

}
