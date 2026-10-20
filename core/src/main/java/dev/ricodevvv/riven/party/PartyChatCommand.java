package dev.ricodevvv.riven.party;

import dev.ricodevvv.riven.command.CommandContext;
import dev.ricodevvv.riven.command.RivenCommand;

import java.util.Collections;
import java.util.List;

/**
 * {@code /pc <message>} to talk to the party, or {@code /pc} alone to toggle party chat.
 */
public class PartyChatCommand extends RivenCommand {

    private final PartyManager parties;

    public PartyChatCommand(PartyManager parties) {
        super("pc", "partychat");
        this.parties = parties;
        this.playerOnly = true;
    }

    @Override
    protected void execute(CommandContext context) {
        if (context.size() == 0) {
            this.parties.toggleChat(context.getPlayer());
            return;
        }
        this.parties.chat(context.getPlayer(), context.join(0));
    }

    @Override
    protected List<String> complete(CommandContext context) {
        return Collections.emptyList();
    }

}
