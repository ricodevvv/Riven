package dev.ricodevvv.riven.scoreboard;

import dev.ricodevvv.riven.Riven;
import dev.ricodevvv.riven.util.PacketUtil;
import dev.ricodevvv.riven.util.Reflection;
import net.minecraft.server.v1_8_R3.IScoreboardCriteria;
import net.minecraft.server.v1_8_R3.Packet;
import net.minecraft.server.v1_8_R3.PacketPlayOutScoreboardDisplayObjective;
import net.minecraft.server.v1_8_R3.PacketPlayOutScoreboardObjective;
import net.minecraft.server.v1_8_R3.PacketPlayOutScoreboardScore;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;

/**
 * Packet based sidebar, one per player. Port of Hyrame's HyriScoreboard:
 * it swaps between two objectives on every update so it never flickers.
 */
public class Sidebar {

    private static final int MAX_LINE_LENGTH = 40;
    private static final int MAX_TITLE_LENGTH = 32;

    protected final Map<Integer, SidebarLine> lines = new TreeMap<>();
    protected final Player player;

    protected String name;
    protected String displayName;
    protected boolean shown;

    /**
     * @param player scoreboard owner
     * @param name internal name (15 characters max)
     * @param displayName title shown on top
     */
    public Sidebar(Player player, String name, String displayName) {
        this.player = player;
        this.name = name.length() > 15 ? name.substring(0, 15) : name;
        this.displayName = this.trim(displayName, MAX_TITLE_LENGTH);
    }

    /**
     * Sets a static line.
     *
     * @param line line number (0 is the top one)
     * @param value text
     */
    public void setLine(int line, String value) {
        this.lines.put(line, new SidebarLine(value));
    }

    /**
     * Sets a line that updates by itself.
     *
     * @param line line number
     * @param value initial text
     * @param update action that changes the text
     * @param ticks how often it runs, in ticks
     */
    public void setLine(int line, String value, Consumer<SidebarLine> update, int ticks) {
        this.lines.put(line, new SidebarLine(value, new SidebarLine.Update(update, ticks)));
    }

    /**
     * Adds a blank line that doesn't clash with the other blank ones.
     *
     * @param line line number
     */
    public void addBlankLine(int line) {
        final StringBuilder value = new StringBuilder();

        for (Map.Entry<Integer, SidebarLine> entry : this.lines.entrySet()) {
            if (entry.getKey() != line && entry.getValue().getValue().equals(value.toString())) {
                value.append(" ");
            }
        }
        this.setLine(line, value.toString());
    }

    public void removeLine(int line) {
        this.lines.remove(line);
    }

    /**
     * Shows the scoreboard and hides whatever the player had before.
     */
    public void show() {
        if (this.shown) {
            return;
        }

        Riven.get().getScoreboardManager().setSidebar(this.player, this);

        this.shown = true;

        PacketUtil.sendPacket(this.player, this.objectivePacket(this.name, 0));
        this.sendLines();
        PacketUtil.sendPacket(this.player, this.displayPacket(this.name));
    }

    /**
     * Takes the scoreboard off the screen.
     */
    public void hide() {
        if (!this.shown) {
            return;
        }

        this.shown = false;

        PacketUtil.sendPacket(this.player, this.objectivePacket(this.name, 1));
        Riven.get().getScoreboardManager().removeSidebar(this.player, this);
    }

    /**
     * Re-sends every line without flickering.
     */
    public synchronized void updateLines() {
        if (!this.shown) {
            return;
        }

        final String oldName = this.name;

        this.name = this.name.endsWith("1") ? this.name.substring(0, this.name.length() - 1) : this.name + "1";

        PacketUtil.sendPacket(this.player, this.objectivePacket(this.name, 0));
        this.sendLines();
        PacketUtil.sendPacket(this.player, this.displayPacket(this.name));
        PacketUtil.sendPacket(this.player, this.objectivePacket(oldName, 1));
    }

    /**
     * Called by the manager every tick for self-updating lines.
     */
    public void onTick() {
        boolean updated = false;

        for (SidebarLine line : this.lines.values()) {
            final SidebarLine.Update update = line.getUpdate();

            if (update != null && update.onTick(line)) {
                updated = true;
            }
        }

        if (updated) {
            this.updateLines();
        }
    }

    private void sendLines() {
        final int size = this.lines.size();
        int index = 0;

        for (SidebarLine line : this.lines.values()) {
            final PacketPlayOutScoreboardScore packet = new PacketPlayOutScoreboardScore();

            Reflection.setField("a", packet, this.trim(line.getValue(), MAX_LINE_LENGTH));
            Reflection.setField("b", packet, this.name);
            Reflection.setField("c", packet, size - index - 1);
            Reflection.setField("d", packet, PacketPlayOutScoreboardScore.EnumScoreboardAction.CHANGE);

            PacketUtil.sendPacket(this.player, packet);
            index++;
        }
    }

    private Packet<?> objectivePacket(String objectiveName, int mode) {
        final PacketPlayOutScoreboardObjective packet = new PacketPlayOutScoreboardObjective();

        Reflection.setField("a", packet, objectiveName);
        Reflection.setField("b", packet, this.displayName);
        Reflection.setField("c", packet, IScoreboardCriteria.EnumScoreboardHealthDisplay.INTEGER);
        Reflection.setField("d", packet, mode);

        return packet;
    }

    private Packet<?> displayPacket(String objectiveName) {
        final PacketPlayOutScoreboardDisplayObjective packet = new PacketPlayOutScoreboardDisplayObjective();

        Reflection.setField("a", packet, 1);
        Reflection.setField("b", packet, objectiveName);

        return packet;
    }

    private String trim(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() > max ? value.substring(0, max) : value;
    }

    /**
     * Changes the scoreboard title.
     *
     * @param displayName new title
     */
    public void setDisplayName(String displayName) {
        this.displayName = this.trim(displayName, MAX_TITLE_LENGTH);

        if (this.shown) {
            PacketUtil.sendPacket(this.player, this.objectivePacket(this.name, 2));
        }
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public Player getPlayer() {
        return this.player;
    }

    public boolean isShown() {
        return this.shown;
    }

    public Map<Integer, SidebarLine> getLines() {
        return this.lines;
    }

}
