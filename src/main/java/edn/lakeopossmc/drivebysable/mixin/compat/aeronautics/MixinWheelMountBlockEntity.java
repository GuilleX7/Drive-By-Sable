package edn.lakeopossmc.drivebysable.mixin.compat.aeronautics;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import edn.lakeopossmc.drivebysable.compat.CableRedstoneCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

// --- LET WHEEL MOUNT READ CABLES FOR BRAKE AND STEER --- //
// * Pseudo since the mod may not be loaded
@Pseudo
@Mixin(targets = "dev.ryanhcode.offroad.content.blocks.wheel_mount.WheelMountBlockEntity", remap = false, priority = 900)
public abstract class MixinWheelMountBlockEntity {

    private static final String GET_SIGNAL =
            "Lnet/minecraft/world/level/Level;getSignal(Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;)I";

    // * Brake physics, server side
    @ModifyExpressionValue(
            method = "sable$physicsTick(Ldev/ryanhcode/sable/sublevel/ServerSubLevel;Ldev/ryanhcode/sable/api/physics/handle/RigidBodyHandle;D)V",
            at = @At(value = "INVOKE", target = GET_SIGNAL),
            remap = false,
            require = 0,
            expect = 0
    )
    private int drivebysable$addCableToBrakeSignal(final int original) {
        return drivebysable$widenFromAbove(original);
    }

    // * Client side brake
    @ModifyExpressionValue(
            method = "tick()V",
            at = @At(value = "INVOKE", target = GET_SIGNAL),
            remap = false,
            require = 0,
            expect = 0
    )
    private int drivebysable$addCableToClientBrakeSignal(final int original) {
        return drivebysable$widenFromAbove(original);
    }

    // * getSteeringSignal reads the clockwise neighbour first
    @ModifyExpressionValue(
            method = "getSteeringSignal()I",
            at = @At(value = "INVOKE", target = GET_SIGNAL, ordinal = 0),
            remap = false,
            require = 0,
            expect = 0
    )
    private int drivebysable$addCableToClockwiseSteeringSignal(final int original) {
        return drivebysable$widenFromSide(original, true);
    }

    // * And the counter clockwise neighbour second
    @ModifyExpressionValue(
            method = "getSteeringSignal()I",
            at = @At(value = "INVOKE", target = GET_SIGNAL, ordinal = 1),
            remap = false,
            require = 0,
            expect = 0
    )
    private int drivebysable$addCableToCounterClockwiseSteeringSignal(final int original) {
        return drivebysable$widenFromSide(original, false);
    }

    // * Rebuilds getSignal(pos.above(), DOWN)
    private int drivebysable$widenFromAbove(final int original) {
        final BlockEntity self = (BlockEntity) (Object) this;
        final Level level = self.getLevel();
        if (level == null) {
            return original;
        }

        return Math.max(
                original,
                CableRedstoneCompat.cableSignalForReversedQuery(level, self.getBlockPos().above(), Direction.DOWN)
        );
    }

    // * Rebuilds getSignal(pos.relative(side), side.getOpposite())
    private int drivebysable$widenFromSide(final int original, final boolean clockWise) {
        final BlockEntity self = (BlockEntity) (Object) this;
        final Level level = self.getLevel();
        if (level == null) {
            return original;
        }

        // * Offroad reads it off its own block
        final BlockState state = self.getBlockState();
        if (!state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            return original;
        }

        final Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        final Direction side = clockWise ? facing.getClockWise() : facing.getCounterClockWise();
        final BlockPos queriedPos = self.getBlockPos().relative(side);

        return Math.max(
                original,
                CableRedstoneCompat.cableSignalForReversedQuery(level, queriedPos, side.getOpposite())
        );
    }
}