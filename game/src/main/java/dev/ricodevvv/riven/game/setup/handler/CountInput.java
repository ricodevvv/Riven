package dev.ricodevvv.riven.game.setup.handler;

import dev.ricodevvv.riven.game.setup.ConfigProcess;
import dev.ricodevvv.riven.input.SignInput;
import org.bukkit.Bukkit;

import java.util.function.IntConsumer;

/**
 * Asks for a number on a sign, like HyriBuild did with its SignGUI. Asks again if the number is wrong.
 */
final class CountInput {

    private CountInput() {}

    /**
     * @param process process
     * @param titleKey key of the two bottom lines of the sign ("Number of" / "locations")
     * @param minimum lowest accepted value
     * @param callback what to do with the number
     */
    static void ask(ConfigProcess<?> process, String titleKey, int minimum, IntConsumer callback) {
        final String[] title = process.text(titleKey).split("\n");

        new SignInput((player, lines) -> {
            try {
                final int value = Integer.parseInt(lines[0].trim());

                if (value >= minimum) {
                    callback.accept(value);
                    return;
                }
            } catch (NumberFormatException ignored) {
            }

            process.sendMessage(process.text("setup.process.invalid-number"));

            Bukkit.getScheduler().runTaskLater(process.getManager().getPlugin(), () -> ask(process, titleKey, minimum, callback), 5L);
        }).withLines("", "^^^^^^^^", title.length > 0 ? title[0] : "", title.length > 1 ? title[1] : "").open(process.getPlayer());
    }

}
