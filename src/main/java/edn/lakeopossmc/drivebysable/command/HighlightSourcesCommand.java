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
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

// --- /dbs highlight <target> [<seconds> | infinite] | /dbs highlight clear --- //
// * Outlines the targeted Sources and everything they drive, for the player running it only
public final class HighlightSourcesCommand {

    private static final int DEFAULT_SECONDS = 10;
    private static final int INFINITE = -1;
    private static final int MAX_SECONDS = 300;
    private static final int MAX_SOURCES = 512;
    private static final int MAX_LINKS = 4096;
    private static final int LISTED_SOURCES = 8;

    private HighlightSourcesCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        final LiteralArgumentBuilder<CommandSourceStack> highlight = Commands.literal("highlight")
                .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS));

        highlight.then(Commands.literal("off").executes(HighlightSourcesCommand::clear));

        return SourceTargets.attach(highlight, true, (node, resolver) -> node
                .executes(context -> run(context, resolver.resolve(context), DEFAULT_SECONDS))
                .then(Commands.argument("seconds", IntegerArgumentType.integer(1, MAX_SECONDS))
                        .executes(context -> run(
                                context,
                                resolver.resolve(context),
                                IntegerArgumentType.getInteger(context, "seconds"))))
                .then(Commands.literal("infinite")
                        .executes(context -> run(context, resolver.resolve(context), INFINITE))));
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
                seconds == INFINITE ? SourceHighlightPacket.INFINITE : seconds * 20
        ));

        final Component feedback = feedback(level, shown, distinctOutputs.size(), seconds, selection.sources().size());
        source.sendSuccess(() -> feedback, false);
        return shown.size();
    }

    private static int clear(final CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        PacketDistributor.sendToPlayer(context.getSource().getPlayerOrException(), SourceHighlightPacket.clear());
        context.getSource().sendSuccess(
                () -> SourceText.message(
                        Component.translatable("commands.drivebysable.highlight.cleared").withStyle(ChatFormatting.GRAY)),
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
        final boolean forever = seconds == INFINITE;
        final Component clearHint = Component.translatable(
                "commands.drivebysable.highlight.clear_hint",
                SourceText.clickable(
                        Component.translatable("commands.drivebysable.here"),
                        "/dbs highlight clear",
                        Component.translatable("commands.drivebysable.highlight.here.hover"))
        ).withStyle(ChatFormatting.GRAY);

        // * One Source reads as a sentence, the rest get a heading and a list
        if (shown.size() == 1) {
            final MutableComponent message = Component.translatable(
                    "commands.drivebysable.highlight.single",
                    SourceText.describe(level, shown.get(0))
            ).withStyle(ChatFormatting.GRAY);

            return SourceText.message(message.append("\n").append(forever
                    ? clearHint
                    : Component.translatable(
                    "commands.drivebysable.highlight.single.duration",
                    SourceText.number(seconds),
                    SourceText.clickable(
                            Component.translatable("commands.drivebysable.here"),
                            "/dbs highlight clear",
                            Component.translatable("commands.drivebysable.highlight.here.hover"))
            ).withStyle(ChatFormatting.GRAY)));
        }

        final MutableComponent message = (forever
                ? Component.translatable(
                "commands.drivebysable.highlight.multiple.infinite",
                SourceText.sourceNumber(shown.size()),
                SourceText.outputNumber(outputs))
                : Component.translatable(
                "commands.drivebysable.highlight.multiple",
                SourceText.sourceNumber(shown.size()),
                SourceText.outputNumber(outputs),
                SourceText.number(seconds))
        ).withStyle(ChatFormatting.GRAY);

        message.append("\n");

        for (int i = 0; i < Math.min(LISTED_SOURCES, shown.size()); i++) {
            message.append("\n").append(Component.translatable(
                    "commands.drivebysable.list.bullet", SourceText.describe(level, shown.get(i))));
        }
        if (shown.size() > LISTED_SOURCES) {
            message.append("\n").append(Component.translatable(
                    "commands.drivebysable.list.more", shown.size() - LISTED_SOURCES));
        }

        if (matched > shown.size()) {
            message.append("\n").append(Component.translatable(
                    "commands.drivebysable.highlight.truncated",
                    SourceText.sourceNumber(shown.size()),
                    SourceText.sourceNumber(matched)));
        }

        return SourceText.message(message.append("\n\n").append(clearHint));
    }
}