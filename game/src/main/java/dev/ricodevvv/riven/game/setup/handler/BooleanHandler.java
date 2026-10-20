package dev.ricodevvv.riven.game.setup.handler;

import dev.ricodevvv.riven.game.setup.ConfigProcess;
import dev.ricodevvv.riven.game.setup.SetupManager;

/**
 * Yes or no, with HyriBuild's two dyes.
 */
public class BooleanHandler extends ConfigOptionHandler<Boolean> {

    public BooleanHandler(ConfigProcess<?> process) {
        super(process);
    }

    @Override
    public void handle() {
        this.process.getManager().giveItem(this.player, SetupManager.YES_ITEM, 0);
        this.process.getManager().giveItem(this.player, SetupManager.NO_ITEM, 1);
    }

    @Override
    public void complete(Boolean value) {
        this.player.getInventory().clear();

        super.complete(value);
    }

}
