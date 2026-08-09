package edn.lakeopossmc.drivebysable.compat.synaxis;

import com.verr1.synaxis.foundation.controllerwire.ControllerWireNetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;

// --- READS SYNAXIS CONTROLLER WIRE SIGNAL --- //
// * Synaxis ships mixins that inject into Drive-By-Wire's WireRedstoneCompact and MixinServerLevel
// * Drive-By-Sable is a separate mod with its own package, so those never apply here
public final class SynaxisCableBridge {

    private static final String MOD_ID = "synaxis";
    private static final boolean LOADED = ModList.get().isLoaded(MOD_ID);

    private SynaxisCableBridge() {
    }

    public static boolean isLoaded() {
        return LOADED;
    }

    // * Vanilla convention: consumer C asked about C + d in direction d
    public static int controllerWireSignalForForwardQuery(
        final Level level,
        final BlockPos queriedPos,
        final Direction queriedDirection
    ) {
        return signalAt(level, queriedPos.relative(queriedDirection.getOpposite()), queriedDirection);
    }

    // * Reversed convention: the caller passed the direction back toward itself
    public static int controllerWireSignalForReversedQuery(
        final Level level,
        final BlockPos queriedPos,
        final Direction queriedDirection
    ) {
        return signalAt(level, queriedPos.relative(queriedDirection), queriedDirection.getOpposite());
    }

    private static int signalAt(final Level level, final BlockPos pos, final Direction face) {
        if (!LOADED || level == null) {
            return 0;
        }

        final ControllerWireNetworkManager manager = ControllerWireNetworkManager.get(level);
        if (manager == null || !manager.hasRuntimeSignals()) {
            return 0;
        }
        return manager.getSignalAt(pos, face);
    }
}