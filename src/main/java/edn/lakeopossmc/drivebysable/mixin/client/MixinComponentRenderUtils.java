package edn.lakeopossmc.drivebysable.mixin.client;

import com.google.common.collect.Lists;
import edn.lakeopossmc.drivebysable.command.SourceText;
import net.minecraft.ChatFormatting;
import net.minecraft.client.ComponentCollector;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.ComponentRenderUtils;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Optional;

// --- HANGING INDENT FOR /dbs OUTPUT --- //
// * Built purely for readability
@Mixin(ComponentRenderUtils.class)
public abstract class MixinComponentRenderUtils {

    @Inject(method = "wrapComponents", at = @At("HEAD"), cancellable = true)
    private static void drivebysable$hangingIndent(
            final FormattedText component,
            final int maxWidth,
            final Font font,
            final CallbackInfoReturnable<List<FormattedCharSequence>> cir
    ) {
        // * Only messages this mod sent
        if (!(component instanceof final Component message) || !SourceText.isOurs(message)) {
            return;
        }

        final String indent = " ".repeat(drivebysable$indentSpaces(font));
        final FormattedCharSequence indentSequence = FormattedCharSequence.forward(indent, Style.EMPTY);
        final int wrapWidth = Math.max(1, maxWidth - font.width(indent));

        final ComponentCollector collector = new ComponentCollector();
        component.visit((style, text) -> {
            collector.append(FormattedText.of(drivebysable$stripColor(text), style));
            return Optional.empty();
        }, Style.EMPTY);

        final List<FormattedCharSequence> lines = Lists.newArrayList();
        final boolean[] indentThisLine = {false};

        font.getSplitter().splitLines(collector.getResultOrEmpty(), wrapWidth, Style.EMPTY, (line, continuation) -> {
            final FormattedCharSequence visual = Language.getInstance().getVisualOrder(line);
            if (!continuation) {
                indentThisLine[0] = drivebysable$isBullet(line);
                lines.add(visual);
                return;
            }
            lines.add(indentThisLine[0] ? FormattedCharSequence.composite(indentSequence, visual) : visual);
        });

        cir.setReturnValue(lines.isEmpty() ? Lists.newArrayList(FormattedCharSequence.EMPTY) : lines);
    }

    private static int drivebysable$indentSpaces(final Font font) {
        final int spaceWidth = Math.max(1, font.width(" "));
        return Math.max(1, Math.round((float) font.width(SourceText.BULLET_PREFIX) / spaceWidth));
    }

    private static boolean drivebysable$isBullet(final FormattedText line) {
        final StringBuilder plain = new StringBuilder();
        line.visit(text -> {
            plain.append(text);
            // * The bullet sits at the very front, no need to read the whole line
            return plain.length() >= 4 ? Optional.of(Boolean.TRUE) : Optional.empty();
        });
        return plain.toString().stripLeading().startsWith(SourceText.BULLET);
    }

    private static String drivebysable$stripColor(final String text) {
        return Minecraft.getInstance().options.chatColors().get() ? text : ChatFormatting.stripFormatting(text);
    }
}