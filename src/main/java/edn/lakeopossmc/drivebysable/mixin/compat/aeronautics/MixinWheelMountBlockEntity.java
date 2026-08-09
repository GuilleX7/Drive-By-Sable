package edn.lakeopossmc.drivebysable.mixin.compat.aeronautics;

import edn.lakeopossmc.drivebysable.compat.CableRedstoneCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// --- LET WHEEL MOUNT READ CABLES FOR BRAKE AND STEER --- //
// * Pseudo since mod may not be loaded
@Pseudo
@Mixin(targets = "dev.ryanhcode.offroad.content.blocks.wheel_mount.WheelMountBlockEntity", remap = false, priority = 1100)
public abstract class MixinWheelMountBlockEntity {

    private static final String GET_SIGNAL =
            "Lnet/minecraft/world/level/Level;getSignal(Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;)I";

    // * Brake physics, server side
    @Redirect(
            method = "sable$physicsTick(Ldev/ryanhcode/sable/sublevel/ServerSubLevel;Ldev/ryanhcode/sable/api/physics/handle/RigidBodyHandle;D)V",
            at = @At(value = "INVOKE", target = GET_SIGNAL),
            remap = false,
            require = 0,
            expect = 0
    )
    private int drivebysable$useCableForBrakeSignal(final Level level, final BlockPos pos, final Direction direction) {
        return CableRedstoneCompat.getSignalIncludingReverseCable(level, pos, direction);
    }

    // * Client side brake
    @Redirect(
            method = "tick()V",
            at = @At(value = "INVOKE", target = GET_SIGNAL),
            remap = false,
            require = 0,
            expect = 0
    )
    private int drivebysable$useCableForClientBrakeSignal(final Level level, final BlockPos pos, final Direction direction) {
        return CableRedstoneCompat.getSignalIncludingReverseCable(level, pos, direction);
    }

    // * Left steer
    @Redirect(
            method = "getSteeringSignal()I",
            at = @At(value = "INVOKE", target = GET_SIGNAL, ordinal = 0),
            remap = false,
            require = 0,
            expect = 0
    )
    private int drivebysable$useCableForLeftSteeringSignal(final Level level, final BlockPos pos, final Direction direction) {
        return CableRedstoneCompat.getSignalIncludingReverseCable(level, pos, direction);
    }

    // * Right steer
    @Redirect(
            method = "getSteeringSignal()I",
            at = @At(value = "INVOKE", target = GET_SIGNAL, ordinal = 1),
            remap = false,
            require = 0,
            expect = 0
    )
    private int drivebysable$useCableForRightSteeringSignal(final Level level, final BlockPos pos, final Direction direction) {
        return CableRedstoneCompat.getSignalIncludingReverseCable(level, pos, direction);
    }
}