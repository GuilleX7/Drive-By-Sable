package edn.lakeopossmc.drivebysable.command;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.network.chat.Component;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

// --- READS getChannel[name] OUT OF A COMMAND --- //
public final class ChannelArgument implements ArgumentType<String> {

    private static final String PREFIX = "getChannel[";
    private static final Collection<String> EXAMPLES = List.of("getChannel[redstone]");

    private static final SimpleCommandExceptionType EXPECTED_CHANNEL = new SimpleCommandExceptionType(
            Component.translatable("commands.drivebysable.argument.channel.expected"));

    private ChannelArgument() {
    }

    public static ChannelArgument channel() {
        return new ChannelArgument();
    }

    public static String getChannel(final CommandContext<?> context, final String name) {
        return context.getArgument(name, String.class);
    }

    @Override
    public String parse(final StringReader reader) throws CommandSyntaxException {
        final int start = reader.getCursor();
        if (!reader.canRead(PREFIX.length()) || !reader.getString().startsWith(PREFIX, start)) {
            throw EXPECTED_CHANNEL.createWithContext(reader);
        }
        reader.setCursor(start + PREFIX.length());

        final StringBuilder name = new StringBuilder();
        while (reader.canRead() && reader.peek() != ']') {
            name.append(reader.read());
        }
        reader.expect(']');

        final String channel = name.toString().trim();
        if (channel.isEmpty()) {
            throw EXPECTED_CHANNEL.createWithContext(reader);
        }
        return channel;
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(
            final CommandContext<S> context,
            final SuggestionsBuilder builder
    ) {
        // * Command adds its own suggestions
        return builder.buildFuture();
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}