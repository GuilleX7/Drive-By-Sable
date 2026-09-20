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
                .then(RemoveCommand.build())
                .then(HighlightCommand.build())
                .then(InfoCommand.build())
                .then(HelpCommand.build())
                .then(Commands.literal("listNext")
                        .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(context -> CableLists.next(context.getSource()))));
    }
}