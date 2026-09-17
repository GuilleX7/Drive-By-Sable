package edn.lakeopossmc.drivebysable.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import edn.lakeopossmc.drivebysable.cable.CableNetworkManager;
import edn.lakeopossmc.drivebysable.network.SourceHighlightPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

// --- /dbs highlight <target> [for <seconds>] | /dbs highlight off --- //
// * Outlines the targeted Sources and everything they drive, for the player running it only
public final class HighlightSourcesCommand {

    private static final int DEFAULT_SECONDS = 10;
    private static final int MAX_SECONDS = 300;
    private static final int MAX_SOURCES = 512;
    private static final int MAX_LINKS = 4096;

    private HighlightSourcesCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        final LiteralArgumentBuilder<CommandSourceStack> highlight = Commands.literal("highlight")
                .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS));

        highlight.then(Commands.literal("off").executes(HighlightSourcesCommand::clear));

        return SourceTargets.attach(highlight, true, (node, resolver) -> node
                .executes(context -> run(context, resolver.resolve(context), DEFAULT_SECONDS))
                .then(Commands.literal("for")
                        .then(Commands.argument("seconds", IntegerArgumentType.integer(1, MAX_SECONDS))
                                .executes(context -> run(
                                        context,
                                        resolver.resolve(context),
                                        IntegerArgumentType.getInteger(context, "seconds"))))));
    }

    private static int run(
            final CommandContext<CommandSourceStack> context,
            final SourceTargets.Selection selection,
            final int seconds
    ) throws CommandSyntaxException {
        final CommandSourceStack source = context.getSource();
        final ServerPlayer player = source.getPlayerOrException();
        final ServerLevel level = selection.level();
        final CableNetworkManager manager = CableNetworkManager.get(level);

        final List<BlockPos> shown = selection.sources().size() > MAX_SOURCES
                ? selection.sources().subList(0, MAX_SOURCES)
                : selection.sources();

        final List<BlockPos> outputs = new ArrayList<>();
        final List<Integer> owners = new ArrayList<>();
        final Set<BlockPos> distinctOutputs = new LinkedHashSet<>();

        for (int index = 0; index < shown.size(); index++) {
            for (final BlockPos output : manager.getOutputPositions(shown.get(index))) {
                if (outputs.size() >= MAX_LINKS) {
                    break;
                }
                outputs.add(output);
                owners.add(index);
                distinctOutputs.add(output);
            }
        }

        PacketDistributor.sendToPlayer(player, new SourceHighlightPacket(
                List.copyOf(shown),
                outputs,
                owners,
                seconds * 20
        ));

        final Component feedback = feedback(level, shown, distinctOutputs.size(), seconds, selection.sources().size());
        source.sendSuccess(() -> feedback, false);
        return shown.size();
    }

    private static int clear(final CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        PacketDistributor.sendToPlayer(context.getSource().getPlayerOrException(), SourceHighlightPacket.clear());
        context.getSource().sendSuccess(
                () -> Component.translatable("commands.drivebysable.highlight.cleared").withStyle(ChatFormatting.GRAY),
                false
        );
        return 1;
    }

    private static Component feedback(
            final ServerLevel level,
            final List<BlockPos> shown,
            final int outputs,
            final int seconds,
            final int matched
    ) {
        final Component duration = seconds(seconds);

        final MutableComponent message = shown.size() == 1
                ? Component.translatable(
                        "commands.drivebysable.highlight.single",
                        SourceText.describe(level, shown.get(0)),
                        outputs(outputs),
                        duration)
                : Component.translatable(
                        "commands.drivebysable.highlight.multiple",
                        SourceText.sources(shown.size()),
                        outputs(outputs),
                        duration);
        message.withStyle(ChatFormatting.GRAY);

        if (matched > shown.size()) {
            message.append("\n").append(Component.translatable(
                    "commands.drivebysable.highlight.truncated", shown.size(), matched));
        }

        return message;
    }

    private static Component outputs(final int count) {
        return count == 1
                ? Component.translatable("commands.drivebysable.outputs.one")
                : Component.translatable("commands.drivebysable.outputs.many", count);
    }

    private static Component seconds(final int count) {
        return count == 1
                ? Component.translatable("commands.drivebysable.seconds.one")
                : Component.translatable("commands.drivebysable.seconds.many", count);
    }
}