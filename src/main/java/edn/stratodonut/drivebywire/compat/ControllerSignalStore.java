package edn.stratodonut.drivebywire.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

// --- COMPAT SHIM FOR MODS BUILT AGAINST THE ORIGINAL DRIVEBYWIRE --- //
public final class ControllerSignalStore {
    private ControllerSignalStore() {}

    public static void setSignal(Level level, BlockPos pos, String channel, int value) {
        edn.lakeopossmc.drivebysable.compat.ControllerSignalStore.setSignal(level, pos, channel, value);
    }

    public static void clear(Level level, BlockPos pos) {
        edn.lakeopossmc.drivebysable.compat.ControllerSignalStore.clear(level, pos);
    }
}