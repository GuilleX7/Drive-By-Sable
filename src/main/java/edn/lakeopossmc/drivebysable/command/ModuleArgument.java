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

// --- READS A @mod[name] OUT OF A COMMAND --- //
// * @mod[any] stands for every module on the target
public final class ModuleArgument implements ArgumentType<String> {

    public static final String ANY = "any";

    private static final Collection<String> EXAMPLES = List.of("@mod[any]", "@mod[left_panel]");

    private static final SimpleCommandExceptionType EXPECTED_MODULE = new SimpleCommandExceptionType(
            Component.translatable("commands.drivebysable.argument.module.expected"));

    private ModuleArgument() {
    }

    public static ModuleArgument module() {
        return new ModuleArgument();
    }

    // * The module name, or ANY, or null when the command was given no @mod
    public static String getModule(final CommandContext<?> context, final String name) {
        return context.getArgument(name, String.class);
    }

    public static boolean isAny(final String module) {
        return ANY.equalsIgnoreCase(module);
    }

    @Override
    public String parse(final StringReader reader) throws CommandSyntaxException {
        final int start = reader.getCursor();
        if (!reader.canRead(5) || !reader.getString().startsWith("@mod[", start)) {
            throw EXPECTED_MODULE.createWithContext(reader);
        }
        reader.setCursor(start + "@mod[".length());

        final StringBuilder name = new StringBuilder();
        while (reader.canRead() && reader.peek() != ']') {
            name.append(reader.read());
        }
        reader.expect(']');

        final String module = name.toString().trim();
        if (module.isEmpty()) {
            throw EXPECTED_MODULE.createWithContext(reader);
        }
        return module;
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(
            final CommandContext<S> context,
            final SuggestionsBuilder builder
    ) {
        if ("@mod[".startsWith(builder.getRemaining()) || builder.getRemaining().isEmpty()) {
            builder.suggest("@mod[" + ANY + "]");
        }
        return builder.buildFuture();
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}