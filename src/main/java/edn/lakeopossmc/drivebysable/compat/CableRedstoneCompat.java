package edn.lakeopossmc.drivebysable.compat;

import edn.lakeopossmc.drivebysable.cable.CableNetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

// --- VANILLA REDSTONE + CABLE SIGNAL BLEND --- //
public final class CableRedstoneCompat {
    private CableRedstoneCompat() {
    }

    // * Sink lookup for the vanilla convention
    public static int cableSignalForForwardQuery(
            final Level level,
            final BlockPos queriedPos,
            final Direction queriedDirection
    ) {
        return CableNetworkManager.get(level)
                .getSignalAt(queriedPos.relative(queriedDirection.getOpposite()), queriedDirection);
    }

    // * Sink lookup for callers that pass the direction back
    public static int cableSignalForReversedQuery(
            final Level level,
            final BlockPos queriedPos,
            final Direction queriedDirection
    ) {
        return CableNetworkManager.get(level)
                .getSignalAt(queriedPos.relative(queriedDirection), queriedDirection.getOpposite());
    }

    // * Vanilla power with no cable contribution at all
    public static int vanillaSignalOnly(
            final Level level,
            final BlockPos queriedPos,
            final Direction queriedDirection
    ) {
        final BlockState state = level.getBlockState(queriedPos);
        final int signal = state.getSignal(level, queriedPos, queriedDirection);
        if (state.shouldCheckWeakPower(level, queriedPos, queriedDirection)) {
            return Math.max(signal, level.getDirectSignalTo(queriedPos));
        }
        return signal;
    }

    // * For per mod redirects on consumers that use the reversed convention
    public static int getSignalIncludingReverseCable(
            final Level level,
            final BlockPos queriedPos,
            final Direction queriedDirection
    ) {
        return Math.max(
                vanillaSignalOnly(level, queriedPos, queriedDirection),
                cableSignalForReversedQuery(level, queriedPos, queriedDirection)
        );
    }
}