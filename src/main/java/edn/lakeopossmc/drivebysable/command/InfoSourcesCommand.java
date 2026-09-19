package edn.lakeopossmc.drivebysable.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import edn.lakeopossmc.drivebysable.cable.CableNetworkManager;
import edn.lakeopossmc.drivebysable.cable.graph.CableNetworkNode.CableNetworkSink;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;

import java.util.List;
import java.util.Map;

// --- /dbs info <target> [summarize | getChannel <name> | getLevel ] --- //
// * Reads what a Source is doing
public final class InfoSourcesCommand {

    private static final int LISTED = 16;

    private static final SimpleCommandExceptionType SINGLE_ONLY = new SimpleCommandExceptionType(
            Component.translatable("commands.drivebysable.info.single_only"));
    private static final DynamicCommandExceptionType NO_SUCH_CHANNEL = new DynamicCommandExceptionType(
            channel -> Component.translatableEscape("commands.drivebysable.info.no_such_channel", channel));

    private InfoSourcesCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return SourceTargets.attach(
                Commands.literal("info").requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS)),
                true,
                (node, resolver) -> {
                    node.executes(context -> summary(context, resolver.resolve(context)));
                    node.then(Commands.literal("summarize")
                            .executes(context -> channels(context, single(resolver.resolve(context)))));
                    node.then(Commands.literal("getLevel")
                            .executes(context -> level(context, single(resolver.resolve(context)))));
                    node.then(Commands.literal("getChannel")
                            .then(Commands.argument("name", StringArgumentType.string())
                                    .suggests(channelSuggestions(resolver))
                                    .executes(context -> channel(
                                            context,
                                            single(resolver.resolve(context)),
                                            StringArgumentType.getString(context, "name")))));
                }
        );
    }

    // * Tab completion offers the channels the targeted Source actually has
    private static SuggestionProvider<CommandSourceStack> channelSuggestions(final SourceTargets.Resolver resolver) {
        return (context, builder) -> {
            final SourceTargets.Selection selection;
            try {
                selection = resolver.resolve(context);
            } catch (final CommandSyntaxException ignored) {
                // * Nothing targeted yet, nothing to suggest
                return builder.buildFuture();
            }

            final CableNetworkManager manager = CableNetworkManager.get(selection.level());
            return SharedSuggestionProvider.suggest(
                    manager.getConnections(selection.sources().get(0)).keySet(), builder);
        };
    }

    //#region // --- VIEWS --- //
    private static int summary(
            final CommandContext<CommandSourceStack> context,
            final SourceTargets.Selection selection
    ) {
        final ServerLevel level = selection.level();
        final CableNetworkManager manager = CableNetworkManager.get(level);
        final List<BlockPos> sources = selection.sources();

        if (sources.size() == 1) {
            return send(context, channelReport(level, manager, sources.get(0)));
        }

        int outputs = 0;
        for (final BlockPos pos : sources) {
            outputs += manager.getOutputPositions(pos).size();
        }

        final MutableComponent message = Component.translatable(
                "commands.drivebysable.info.multiple",
                SourceText.sourceNumber(sources.size()),
                SourceText.outputNumber(outputs)
        ).withStyle(ChatFormatting.GRAY);

        message.append("\n");

        for (int i = 0; i < Math.min(LISTED, sources.size()); i++) {
            final BlockPos pos = sources.get(i);
            message.append("\n").append(Component.translatable(
                    "commands.drivebysable.list.bullet",
                    Component.translatable(
                            "commands.drivebysable.info.entry",
                            drillDown(level, pos),
                            SourceText.sourceNumber(manager.getConnections(pos).size()),
                            SourceText.outputNumber(manager.getOutputPositions(pos).size()),
                            SourceText.sourceType(level, pos))));
        }
        if (sources.size() > LISTED) {
            message.append("\n").append(Component.translatable(
                    "commands.drivebysable.list.more", sources.size() - LISTED));
        }

        return send(context, message);
    }

    private static int channels(
            final CommandContext<CommandSourceStack> context,
            final Single target
    ) {
        return send(context, channelReport(target.level(), target.manager(), target.pos()));
    }

    // * What the Source is, then each of its channels
    private static MutableComponent channelReport(
            final ServerLevel level,
            final CableNetworkManager manager,
            final BlockPos pos
    ) {
        final Map<String, List<CableNetworkSink>> connections = manager.getConnections(pos);
        final Map<String, Integer> signals = manager.getSourceSignals(pos);

        final MutableComponent message = Component.translatable(
                "commands.drivebysable.info.header",
                SourceText.describe(level, pos),
                SourceText.sourceType(level, pos)
        ).withStyle(ChatFormatting.GRAY);

        message.append("\n");

        int listed = 0;
        for (final Map.Entry<String, List<CableNetworkSink>> entry : connections.entrySet()) {
            if (listed++ >= LISTED) {
                message.append("\n").append(Component.translatable(
                        "commands.drivebysable.list.more", connections.size() - LISTED));
                break;
            }

            message.append("\n").append(Component.translatable(
                    "commands.drivebysable.list.bullet",
                    Component.translatable(
                            "commands.drivebysable.info.channel.entry",
                            SourceText.channel(entry.getKey()),
                            SourceText.signal(signals.getOrDefault(entry.getKey(), 0)),
                            SourceText.outputNumber(entry.getValue().size()))));
        }

        return message;
    }

    private static int channel(
            final CommandContext<CommandSourceStack> context,
            final Single target,
            final String name
    ) throws CommandSyntaxException {
        final List<CableNetworkSink> sinks = target.manager().getConnections(target.pos()).get(name);
        if (sinks == null) {
            throw NO_SUCH_CHANNEL.create(name);
        }

        final MutableComponent message = Component.translatable(
                "commands.drivebysable.info.channel.header",
                SourceText.channel(name),
                SourceText.signal(target.manager().getSourceSignals(target.pos()).getOrDefault(name, 0)),
                SourceText.outputNumber(sinks.size())
        ).withStyle(ChatFormatting.GRAY);

        message.append("\n");

        for (int i = 0; i < Math.min(LISTED, sinks.size()); i++) {
            final CableNetworkSink sink = sinks.get(i);
            final BlockPos sinkPos = sink.blockPos();
            final Component where = SourceText.describeOutput(target.level(), sinkPos);

            message.append("\n").append(Component.translatable(
                    "commands.drivebysable.list.bullet",
                    sink.isModule()
                            ? Component.translatable(
                            "commands.drivebysable.info.output.module",
                            where,
                            SourceText.outputChannel(sink.sinkChannel()))
                            : Component.translatable(
                            "commands.drivebysable.info.output.face",
                            where,
                            SourceText.side(Direction.from3DDataValue(sink.direction())))));
        }
        if (sinks.size() > LISTED) {
            message.append("\n").append(Component.translatable(
                    "commands.drivebysable.list.more", sinks.size() - LISTED));
        }

        return send(context, message);
    }

    // * Which level the Source lives on
    private static int level(
            final CommandContext<CommandSourceStack> context,
            final Single target
    ) {
        return send(context, Component.translatable(
                "commands.drivebysable.info.level",
                SourceText.describe(target.level(), target.pos())
        ).withStyle(ChatFormatting.GRAY));
    }
    //#endregion

    // * The detail views only make sense for one Source at a time
    private record Single(ServerLevel level, CableNetworkManager manager, BlockPos pos) {
    }

    private static Single single(final SourceTargets.Selection selection) throws CommandSyntaxException {
        if (selection.sources().size() != 1) {
            throw SINGLE_ONLY.create();
        }
        return new Single(
                selection.level(),
                CableNetworkManager.get(selection.level()),
                selection.sources().get(0));
    }

    // * Clicking a listed Source runs info on it
    private static Component drillDown(final ServerLevel level, final BlockPos pos) {
        return SourceText.describe(level, pos).withStyle(style -> style.withClickEvent(new ClickEvent(
                ClickEvent.Action.RUN_COMMAND,
                "/dbs info at " + pos.getX() + " " + pos.getY() + " " + pos.getZ())));
    }

    private static int send(final CommandContext<CommandSourceStack> context, final MutableComponent message) {
        final Component feedback = SourceText.message(message);
        context.getSource().sendSuccess(() -> feedback, false);
        return 1;
    }
}