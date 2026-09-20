package edn.lakeopossmc.drivebysable.command;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// --- LONG LISTS --- //
// * What a command could not fit is kept
public final class CableLists {

    public static final int PAGE = 16;

    private static final Map<String, List<Component>> remaining = new HashMap<>();

    private CableLists() {
    }

    // * Adds the first page to the message and keeps the rest for /dbs listNext
    public static void append(
            final CommandSourceStack source,
            final MutableComponent message,
            final List<Component> lines
    ) {
        final int shown = Math.min(PAGE, lines.size());
        for (int i = 0; i < shown; i++) {
            message.append("\n").append(bullet(lines.get(i)));
        }

        final String key = source.getTextName();
        if (lines.size() > shown) {
            remaining.put(key, new ArrayList<>(lines.subList(shown, lines.size())));
            message.append("\n").append(Component.translatable(
                    "commands.drivebysable.list.remaining",
                    SourceText.number(lines.size() - shown),
                    SourceText.clickable(
                            Component.translatable("commands.drivebysable.list.next_word"),
                            "/dbs listNext",
                            Component.translatable("commands.drivebysable.list.next_word.hover"))
            ).withStyle(ChatFormatting.GRAY));
            return;
        }
        remaining.remove(key);
    }

    // * The next page
    public static int next(final CommandSourceStack source) {
        final String key = source.getTextName();
        final List<Component> left = remaining.get(key);

        if (left == null || left.isEmpty()) {
            remaining.remove(key);
            final Component empty = SourceText.message(Component
                    .translatable("commands.drivebysable.list.empty")
                    .withStyle(ChatFormatting.GRAY));
            source.sendSuccess(() -> empty, false);
            return 0;
        }

        final int shown = Math.min(PAGE, left.size());
        final MutableComponent message = Component.translatable(
                "commands.drivebysable.list.more",
                SourceText.number(shown)
        ).withStyle(ChatFormatting.GRAY);

        for (int i = 0; i < shown; i++) {
            message.append("\n").append(bullet(left.get(i)));
        }

        left.subList(0, shown).clear();
        if (left.isEmpty()) {
            remaining.remove(key);
        } else {
            message.append("\n").append(Component.translatable(
                    "commands.drivebysable.list.remaining",
                    SourceText.number(left.size()),
                    SourceText.clickable(
                            Component.translatable("commands.drivebysable.list.next_word"),
                            "/dbs listNext",
                            Component.translatable("commands.drivebysable.list.next_word.hover"))
            ).withStyle(ChatFormatting.GRAY));
        }

        final Component feedback = SourceText.message(message);
        source.sendSuccess(() -> feedback, false);
        return shown;
    }

    private static Component bullet(final Component line) {
        return Component.translatable("commands.drivebysable.list.bullet", line);
    }
}