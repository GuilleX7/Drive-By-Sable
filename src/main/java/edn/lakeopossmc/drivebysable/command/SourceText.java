package edn.lakeopossmc.drivebysable.command;

import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPoint;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import edn.lakeopossmc.drivebysable.DriveBySableMod;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.List;

// --- HOW A SOURCE READS IN CHAT --- //
// * Everything shown here is where the block appears in the world
public final class SourceText {

    public static final String CHAT_MARKER = DriveBySableMod.MOD_ID + ":chat";

    public static final String BULLET = "\u2022";
    public static final String BULLET_PREFIX = " " + BULLET + " ";

    private SourceText() {
    }

    public static MutableComponent message(final MutableComponent message) {
        return Component.empty()
                .append(Component.literal("").withStyle(style -> style.withInsertion(CHAT_MARKER)))
                .append(message);
    }

    // * Whether a chat message came from one of our commands
    public static boolean isOurs(final Component message) {
        final List<Component> siblings = message.getSiblings();
        return !siblings.isEmpty() && CHAT_MARKER.equals(siblings.get(0).getStyle().getInsertion());
    }

    // * Where the centre of the block appears in the world
    public static Vec3 worldCentre(final Level level, final BlockPos pos) {
        return Sable.HELPER.projectOutOfSubLevel(level, Vec3.atCenterOf(pos));
    }

    public static BlockPos worldBlock(final Level level, final BlockPos pos) {
        return BlockPos.containing(worldCentre(level, pos));
    }

    private static final int BLOCK_COLOR = ArmInteractionPoint.Mode.TAKE.getColor();
    private static final int OUTPUT_COLOR = ArmInteractionPoint.Mode.DEPOSIT.getColor();

    // * "[Lever] at [12, 64, -30] for level: [World]"
    public static MutableComponent describe(final Level level, final BlockPos pos) {
        return Component.translatable(
                "commands.drivebysable.source",
                blockName(level, pos),
                coordinates(level, pos),
                levelLabel(level, pos)
        ).withStyle(ChatFormatting.GRAY);
    }

    public static Component blockName(final Level level, final BlockPos pos) {
        final BlockState state = level.getBlockState(pos);
        // * A sublevel that is not loaded reads as air
        final MutableComponent name = state.isAir()
                ? Component.translatable("commands.drivebysable.source.unloaded")
                : state.getBlock().getName();
        return bracketed(name.withStyle(style -> style.withColor(BLOCK_COLOR)));
    }

    // * Green like vanilla /locate, clicking fills in a teleport
    public static Component coordinates(final Level level, final BlockPos pos) {
        final BlockPos world = worldBlock(level, pos);
        return bracketed(Component.translatable("chat.coordinates", world.getX(), world.getY(), world.getZ())
                .withStyle(ChatFormatting.GREEN))
                .withStyle(style -> style
                        .withClickEvent(new ClickEvent(
                                ClickEvent.Action.SUGGEST_COMMAND,
                                "/tp @s " + world.getX() + " " + world.getY() + " " + world.getZ()))
                        .withHoverEvent(new HoverEvent(
                                HoverEvent.Action.SHOW_TEXT,
                                Component.translatable("commands.drivebysable.coordinates.hover"))));
    }

    public static Component levelLabel(final Level level, final BlockPos pos) {
        final SubLevel subLevel = Sable.HELPER.getContaining(level, pos);
        if (subLevel == null) {
            return bracketed(Component.translatable("commands.drivebysable.level.world")
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        }

        final String name = subLevel.getName();
        final MutableComponent label = name == null || name.isBlank()
                ? Component.translatable("commands.drivebysable.level.unnamed")
                : Component.literal(name);
        final String id = subLevel.getUniqueId().toString();

        return bracketed(label.withStyle(ChatFormatting.LIGHT_PURPLE))
                .withStyle(style -> style
                        .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, id))
                        .withHoverEvent(new HoverEvent(
                                HoverEvent.Action.SHOW_TEXT,
                                Component.translatable("commands.drivebysable.level.hover", id))));
    }

    private static MutableComponent bracketed(final Component inner) {
        return Component.empty()
                .append(Component.literal("[").withStyle(ChatFormatting.WHITE))
                .append(inner)
                .append(Component.literal("]").withStyle(ChatFormatting.WHITE));
    }

    public static Component number(final int value) {
        return bracketed(Component.literal(String.valueOf(value)).withStyle(ChatFormatting.GREEN));
    }

    public static Component sourceNumber(final int value) {
        return number(value, BLOCK_COLOR);
    }

    public static Component outputNumber(final int value) {
        return number(value, OUTPUT_COLOR);
    }

    public static Component number(final int value, final int color) {
        return bracketed(Component.literal(String.valueOf(value)).withStyle(style -> style.withColor(color)));
    }

    public static Component clickable(final Component word, final String command, final Component hover) {
        return bracketed(word.copy().withStyle(ChatFormatting.GREEN))
                .withStyle(style -> style
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover)));
    }

    public static Component dimension(final ServerLevel level) {
        return bracketed(Component.literal(level.dimension().location().toString())
                .withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    public static Component connections(final int count) {
        return count == 1
                ? Component.translatable("commands.drivebysable.connections.one")
                : Component.translatable("commands.drivebysable.connections.many", count);
    }

    public static Component sources(final int count) {
        return count == 1
                ? Component.translatable("commands.drivebysable.sources.one")
                : Component.translatable("commands.drivebysable.sources.many", count);
    }
}