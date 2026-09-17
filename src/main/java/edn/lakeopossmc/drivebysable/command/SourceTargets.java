package edn.lakeopossmc.drivebysable.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.sublevel.SubLevel;
import edn.lakeopossmc.drivebysable.CableConfig;
import edn.lakeopossmc.drivebysable.cable.CableNetworkManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// --- WHICH SOURCES A /cable COMMAND ACTS ON --- //
// * nearest | looking | at <x y z> | radius <blocks> | all
public final class SourceTargets {

    // * Further than hand reach
    private static final double LOOK_REACH = 64.0;

    private static final SimpleCommandExceptionType NONE_FOUND = new SimpleCommandExceptionType(
            Component.translatable("commands.drivebysable.target.none"));
    private static final SimpleCommandExceptionType NOT_LOOKING = new SimpleCommandExceptionType(
            Component.translatable("commands.drivebysable.target.not_looking"));
    private static final DynamicCommandExceptionType NOT_A_SOURCE = new DynamicCommandExceptionType(
            block -> Component.translatableEscape("commands.drivebysable.target.not_a_source", block));
    private static final DynamicCommandExceptionType NONE_AT = new DynamicCommandExceptionType(
            pos -> Component.translatableEscape("commands.drivebysable.target.none_at", pos));
    private static final DynamicCommandExceptionType NONE_IN_RADIUS = new DynamicCommandExceptionType(
            radius -> Component.translatableEscape("commands.drivebysable.target.none_in_radius", radius));
    private static final DynamicCommandExceptionType RADIUS_TOO_LARGE = new DynamicCommandExceptionType(
            limit -> Component.translatableEscape("commands.drivebysable.target.radius_too_large", limit));

    private SourceTargets() {
    }

    public enum Kind {
        NEAREST,
        LOOKING,
        AT,
        RADIUS,
        ALL
    }

    // * What a target resolved to. Sources are ordered nearest first
    public record Selection(Kind kind, ServerLevel level, List<BlockPos> sources) {
    }

    @FunctionalInterface
    public interface Resolver {
        Selection resolve(CommandContext<CommandSourceStack> context) throws CommandSyntaxException;
    }

    @FunctionalInterface
    public interface Finisher {
        void finish(ArgumentBuilder<CommandSourceStack, ?> node, Resolver resolver);
    }

    // * allowMany false leaves out radius and all, for commands that only make sense on one Source
    public static <T extends ArgumentBuilder<CommandSourceStack, T>> T attach(
            final T parent,
            final boolean allowMany,
            final Finisher finisher
    ) {
        parent.then(finish(Commands.literal("nearest"), SourceTargets::nearest, finisher));
        parent.then(finish(Commands.literal("looking"), SourceTargets::looking, finisher));
        parent.then(Commands.literal("at")
                .then(finish(Commands.argument("pos", BlockPosArgument.blockPos()), SourceTargets::at, finisher)));

        if (allowMany) {
            parent.then(Commands.literal("radius")
                    .then(finish(Commands.argument("blocks", IntegerArgumentType.integer(1)),
                            SourceTargets::radius, finisher)));
            parent.then(finish(Commands.literal("all"), SourceTargets::all, finisher));
        }

        return parent;
    }

    private static <N extends ArgumentBuilder<CommandSourceStack, ?>> N finish(
            final N node,
            final Resolver resolver,
            final Finisher finisher
    ) {
        finisher.finish(node, resolver);
        return node;
    }

    //#region // --- RESOLVERS --- //
    private static Selection nearest(final CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final ServerLevel level = context.getSource().getLevel();
        final List<BlockPos> sorted = sortedByDistance(context.getSource());
        if (sorted.isEmpty()) {
            throw NONE_FOUND.create();
        }
        return new Selection(Kind.NEAREST, level, List.of(sorted.get(0)));
    }

    private static Selection looking(final CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final ServerPlayer player = context.getSource().getPlayerOrException();
        final ServerLevel level = player.serverLevel();

        // * Sable routes block raycasts through sublevels, so this lands on ship blocks too
        final HitResult hit = player.pick(LOOK_REACH, 1.0F, false);
        if (!(hit instanceof final BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) {
            throw NOT_LOOKING.create();
        }

        final BlockPos pos = blockHit.getBlockPos();
        if (!CableNetworkManager.get(level).isSource(pos)) {
            throw NOT_A_SOURCE.create(SourceText.describe(level, pos));
        }
        return new Selection(Kind.LOOKING, level, List.of(pos.immutable()));
    }

    // * A world position, or a stored position as used by clickable chat lines
    private static Selection at(final CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final ServerLevel level = context.getSource().getLevel();
        final BlockPos typed = BlockPosArgument.getBlockPos(context, "pos");
        final CableNetworkManager manager = CableNetworkManager.get(level);

        if (manager.isSource(typed)) {
            return new Selection(Kind.AT, level, List.of(typed));
        }

        // * Otherwise whichever sublevel block appears at that spot
        final Vec3 centre = Vec3.atCenterOf(typed);
        final List<BlockPos> found = new ArrayList<>();
        for (final SubLevel subLevel : Sable.HELPER.getAllIntersecting(level, new BoundingBox3d(
                centre.x - 0.5, centre.y - 0.5, centre.z - 0.5,
                centre.x + 0.5, centre.y + 0.5, centre.z + 0.5))) {
            final BlockPos local = BlockPos.containing(subLevel.logicalPose().transformPositionInverse(centre));
            if (manager.isSource(local)) {
                found.add(local);
            }
        }

        if (found.isEmpty()) {
            throw NONE_AT.create(Component.translatable("chat.coordinates", typed.getX(), typed.getY(), typed.getZ()));
        }
        return new Selection(Kind.AT, level, List.of(found.get(0)));
    }

    private static Selection radius(final CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final int blocks = IntegerArgumentType.getInteger(context, "blocks");
        final int limit = CableConfig.CONFIG.commandRadiusLimit.get();
        if (blocks > limit) {
            throw RADIUS_TOO_LARGE.create(limit);
        }

        final ServerLevel level = context.getSource().getLevel();
        final Vec3 origin = origin(context.getSource());
        final double maxDistanceSqr = (double) blocks * blocks;

        final List<BlockPos> inside = new ArrayList<>();
        for (final BlockPos pos : sortedByDistance(context.getSource())) {
            if (SourceText.worldCentre(level, pos).distanceToSqr(origin) > maxDistanceSqr) {
                break;
            }
            inside.add(pos);
        }

        if (inside.isEmpty()) {
            throw NONE_IN_RADIUS.create(blocks);
        }
        return new Selection(Kind.RADIUS, level, inside);
    }

    // * Everything in the command's dimension
    private static Selection all(final CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final List<BlockPos> sorted = sortedByDistance(context.getSource());
        if (sorted.isEmpty()) {
            throw NONE_FOUND.create();
        }
        return new Selection(Kind.ALL, context.getSource().getLevel(), sorted);
    }
    //#endregion

    private static Vec3 origin(final CommandSourceStack source) {
        return Sable.HELPER.projectOutOfSubLevel(source.getLevel(), source.getPosition());
    }

    private static List<BlockPos> sortedByDistance(final CommandSourceStack source) {
        final ServerLevel level = source.getLevel();
        final Vec3 origin = origin(source);
        final List<BlockPos> sources = CableNetworkManager.get(level).getSourcePositions();

        final Map<BlockPos, Double> distances = new HashMap<>(sources.size());
        for (final BlockPos pos : sources) {
            distances.put(pos, SourceText.worldCentre(level, pos).distanceToSqr(origin));
        }
        sources.sort(Comparator.<BlockPos>comparingDouble(distances::get));
        return sources;
    }
}