package edn.lakeopossmc.drivebysable.command;

import net.minecraft.commands.Commands;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

// --- /dbs --- //
// * Root for every utility command
public final class CableCommands {
    private CableCommands() {
    }

    public static void register(final RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("dbs")
                .then(RemoveSourcesCommand.build())
                .then(HighlightSourcesCommand.build()));
    }
}