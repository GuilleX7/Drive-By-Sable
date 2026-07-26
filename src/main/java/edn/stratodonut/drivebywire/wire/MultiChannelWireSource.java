package edn.stratodonut.drivebywire.wire;

import edn.lakeopossmc.drivebysable.cable.MultiChannelCableSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import java.util.List;

// --- COMPAT SHIM FOR MODS BUILT AGAINST THE ORIGINAL DRIVEBYWIRE --- //
// * Bridges into DriveBySable's MultiChannelCableSource
public interface MultiChannelWireSource extends MultiChannelCableSource {
    List<String> getChannels();
    String nextChannel(String currentChannel, boolean reverse);

    @Override
    default List<String> cable$getChannels(Level level, BlockPos pos) {
        return getChannels();
    }

    @Override
    default String cable$nextChannel(Level level, BlockPos pos, String current, boolean forward) {
        return nextChannel(current, forward);
    }
}