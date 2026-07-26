package edn.stratodonut.drivebywire.compat;

import edn.lakeopossmc.drivebysable.compat.CableRedstoneCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

// --- COMPAT SHIM FOR MODS BUILT AGAINST THE ORIGINAL DRIVEBYWIRE --- //
// * Synaxis mixes into getSignalIncludingReverseWire by name
// * drivebysable method is named getSignalIncludingReverseCable
public final class WireRedstoneCompat {
    private WireRedstoneCompat() {
    }

    public static int getSignalIncludingReverseWire(final Level level, final BlockPos queriedPos, final Direction queriedDirection) {
        return CableRedstoneCompat.getSignalIncludingReverseCable(level, queriedPos, queriedDirection);
    }
}