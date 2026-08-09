package edn.lakeopossmc.drivebysable.cable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

// --- CONTRACT FOR MULTI CHANNEL SOURCES --- //
// * Hub blocks implement this to expose their channel list
// * Sub target aware blocks (dashpanels) additionally scope channels per module
public interface MultiChannelCableSource {
    List<String> cable$getChannels(Level level, BlockPos pos);

    String cable$nextChannel(Level level, BlockPos pos, String current, boolean forward);

    // * Channels belonging to a single sub target
    // * Default treats the whole block as one sub target
    default List<String> cable$getChannels(final Level level, final BlockPos pos, @Nullable final String subTarget) {
        return cable$getChannels(level, pos);
    }

    @Nullable
    default String cable$nextChannel(
            final Level level,
            final BlockPos pos,
            @Nullable final String subTarget,
            final String current,
            final boolean forward
    ) {
        return cable$nextChannel(level, pos, current, forward);
    }

    // * True when this block hands out channels per sub target rather than per block
    default boolean cable$hasSubTargets() {
        return false;
    }
}
