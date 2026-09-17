package edn.lakeopossmc.drivebysable.command;

import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPoint;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

// --- HOW A SOURCE READS IN CHAT --- //
// * Everything shown here is where the block appears in the world
public final class SourceText {
    private SourceText() {
    }

    // * Where the centre of the block appears in the world
    public static Vec3 worldCentre(final Level level, final BlockPos pos) {
        return Sable.HELPER.projectOutOfSubLevel(level, Vec3.atCenterOf(pos));
    }

    public static BlockPos worldBlock(final Level level, final BlockPos pos) {
        return BlockPos.containing(worldCentre(level, pos));
    }

    private static final int BLOCK_COLOR = ArmInteractionPoint.Mode.TAKE.getColor();

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
                    .withStyle(style -> style.withColor(BLOCK_COLOR)));
        }

        final String name = subLevel.getName();
        final MutableComponent label = name == null || name.isBlank()
                ? Component.translatable("commands.drivebysable.level.unnamed")
                : Component.literal(name);
        final String id = subLevel.getUniqueId().toString();

        return bracketed(label.withStyle(style -> style.withColor(BLOCK_COLOR)))
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