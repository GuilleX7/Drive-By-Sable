package edn.lakeopossmc.drivebysable.mixin;

import edn.lakeopossmc.drivebysable.compat.CableRedstoneCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.SignalGetter;
import net.minecraft.world.level.block.ComparatorBlock;
import net.minecraft.world.level.block.DiodeBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// --- CABLE SIGNAL AS A COMPARATOR SIDE INPUT --- //
// * The MixinServerLevel blend covers everything that reads power through getSignal
// * This covers getAlternateSignal
@Mixin(DiodeBlock.class)
public abstract class MixinDiodeBlock {

    // * Scoped to comparators on purpose
    @Inject(method = "getAlternateSignal", at = @At("RETURN"), cancellable = true)
    private void drivebysable$blendCableSideInput(
            final SignalGetter signalGetter,
            final BlockPos pos,
            final BlockState state,
            final CallbackInfoReturnable<Integer> cir
    ) {
        if (!((Object) this instanceof ComparatorBlock) || !(signalGetter instanceof final Level level)) {
            return;
        }

        final Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);

        // * The two side faces of the comparator
        final int cableSignal = Math.max(
                CableRedstoneCompat.cableSignalOnFace(level, pos, facing.getClockWise()),
                CableRedstoneCompat.cableSignalOnFace(level, pos, facing.getCounterClockWise())
        );

        if (cableSignal > cir.getReturnValueI()) {
            cir.setReturnValue(cableSignal);
        }
    }
}