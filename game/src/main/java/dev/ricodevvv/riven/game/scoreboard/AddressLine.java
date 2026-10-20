package dev.ricodevvv.riven.game.scoreboard;

import dev.ricodevvv.riven.scoreboard.SidebarLine;
import org.bukkit.ChatColor;

import java.util.function.Consumer;

/**
 * The server address animation at the bottom of the scoreboard: a bright letter running through the text. Port of Hyrame's IPLine.
 */
public class AddressLine implements Consumer<SidebarLine> {

    private static final int PAUSE = 20;

    private final String address;
    private int count;
    private int charIndex;

    public AddressLine(String address) {
        this.address = address;
    }

    @Override
    public void accept(SidebarLine line) {
        if (this.count++ < PAUSE) {
            return;
        }

        if (this.charIndex >= this.address.length()) {
            line.setValue(ChatColor.DARK_AQUA + this.address);

            this.count = 0;
            this.charIndex = 0;
            return;
        }

        final String start = this.address.substring(0, this.charIndex);
        final String character = this.address.substring(this.charIndex, this.charIndex + 1);
        final String end = this.address.substring(this.charIndex + 1);

        line.setValue(ChatColor.DARK_AQUA + start + ChatColor.AQUA + character + ChatColor.DARK_AQUA + end);

        this.charIndex++;
    }

}
