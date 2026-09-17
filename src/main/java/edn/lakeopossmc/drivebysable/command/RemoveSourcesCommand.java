package edn.lakeopossmc.drivebysable.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import edn.lakeopossmc.drivebysable.cable.CableNetworkManager;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// --- /cable remove <target> --- //
// * Disconnects every connection driven by the targeted Sources
public final class RemoveSourcesCommand {

    // * How long "all" waits for
    private static final long CONFIRM_WINDOW_TICKS = 200L;
    // * Sources listed by name before the rest are summarised
    private static final int LISTED_SOURCES = 8;

    // * Who has an "all" waiting to be confirmed
    private static final Map<String, Long> pendingConfirmations = new HashMap<>();

    private RemoveSourcesCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return SourceTargets.attach(
                Commands.literal("remove").requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS)),
                true,
                (node, resolver) -> node.executes(context -> run(context, resolver.resolve(context)))
        );
    }

    private static int run(
            final CommandContext<CommandSourceStack> context,
            final SourceTargets.Selection selection
    ) {
        final CommandSourceStack source = context.getSource();
        final ServerLevel level = selection.level();
        final CableNetworkManager manager = CableNetworkManager.get(level);

        if (selection.kind() == SourceTargets.Kind.ALL && !confirm(source, level)) {
            int pending = 0;
            for (final BlockPos pos : selection.sources()) {
                pending += manager.countConnectionsFrom(pos);
            }
            final Component prompt = confirmationPrompt(level, selection.sources().size(), pending);
            source.sendSuccess(() -> prompt, false);
            return 0;
        }

        // * Described before removal, while the counts are still there to read
        final List<Component> removed = new ArrayList<>();
        int connections = 0;
        for (final BlockPos pos : selection.sources()) {
            final int count = manager.countConnectionsFrom(pos);
            final MutableComponent description = SourceText.describe(level, pos);

            // * Commands never refund Cables, there is no player whose inventory they came from
            if (manager.removeAllFromSourceInternal(null, level, pos)) {
                connections += count;
                removed.add(description);
            }
        }

        final Component feedback = feedback(removed, connections);
        source.sendSuccess(() -> feedback, true);
        return removed.size();
    }

    private static boolean confirm(final CommandSourceStack source, final ServerLevel level) {
        final String key = source.getTextName() + "|" + level.dimension().location();
        final long now = level.getGameTime();
        final Long expires = pendingConfirmations.get(key);

        if (expires != null && now <= expires) {
            pendingConfirmations.remove(key);
            return true;
        }

        pendingConfirmations.put(key, now + CONFIRM_WINDOW_TICKS);
        return false;
    }

    private static Component confirmationPrompt(final ServerLevel level, final int sources, final int connections) {
        final String dimension = level.dimension().location().toString();
        final Component button = Component.translatable("commands.drivebysable.remove.confirm.button")
                .withStyle(style -> style
                        .withColor(ChatFormatting.RED)
                        .withBold(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/cable remove all"))
                        .withHoverEvent(new HoverEvent(
                                HoverEvent.Action.SHOW_TEXT,
                                Component.translatable("commands.drivebysable.remove.confirm.hover", dimension))));

        return Component.translatable(
                "commands.drivebysable.remove.confirm",
                SourceText.connections(connections),
                SourceText.sources(sources),
                dimension,
                button
        ).withStyle(ChatFormatting.YELLOW);
    }

    private static Component feedback(final List<Component> removed, final int connections) {
        if (removed.size() == 1) {
            return Component.translatable("commands.drivebysable.remove.single", removed.get(0))
                    .withStyle(ChatFormatting.GRAY);
        }

        final MutableComponent message = Component.translatable(
                "commands.drivebysable.remove.multiple",
                SourceText.connections(connections),
                SourceText.sources(removed.size())
        ).withStyle(ChatFormatting.GRAY);

        for (int i = 0; i < Math.min(LISTED_SOURCES, removed.size()); i++) {
            message.append("\n").append(Component.translatable("commands.drivebysable.list.bullet", removed.get(i)));
        }
        if (removed.size() > LISTED_SOURCES) {
            message.append("\n").append(Component.translatable(
                    "commands.drivebysable.list.more", removed.size() - LISTED_SOURCES));
        }

        return message;
    }
}