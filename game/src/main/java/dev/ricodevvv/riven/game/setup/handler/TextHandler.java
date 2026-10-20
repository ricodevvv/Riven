package dev.ricodevvv.riven.game.setup.handler;

import dev.ricodevvv.riven.Riven;
import dev.ricodevvv.riven.game.setup.ConfigProcess;

/**
 * Free text typed in chat (a display name, a list of builders...).
 */
public class TextHandler extends ConfigOptionHandler<String> {

    public TextHandler(ConfigProcess<?> process) {
        super(process);
    }

    @Override
    public void handle() {
        this.process.sendMessage(this.process.text("setup.chat.prompt"));

        Riven.get().getChatInputManager().await(this.player, (player, text) -> this.complete(text), () -> this.process.getManager().cancel(this.player));
    }

}
