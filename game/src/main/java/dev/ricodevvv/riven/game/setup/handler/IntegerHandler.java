package dev.ricodevvv.riven.game.setup.handler;

import dev.ricodevvv.riven.game.setup.ConfigProcess;
import org.bukkit.Bukkit;

/**
 * A number above zero typed on a sign.
 */
public class IntegerHandler extends ConfigOptionHandler<Integer> {

    public IntegerHandler(ConfigProcess<?> process) {
        super(process);
    }

    @Override
    public void handle() {
        Bukkit.getScheduler().runTaskLater(this.process.getManager().getPlugin(), () -> CountInput.ask(this.process, "setup.sign.value", 1, this::complete), 5L);
    }

}
