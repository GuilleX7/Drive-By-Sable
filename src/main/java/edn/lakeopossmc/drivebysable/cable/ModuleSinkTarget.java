package edn.lakeopossmc.drivebysable.cable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

// --- CONTRACT FOR BLOCKS THAT ACCEPT CABLES ON NAMED CHANNELS --- //
// * Normal sinks are a block face and receive signal through neighbour updates
// * A module sink is addressed by name instead
public interface ModuleSinkTarget {

    // * Every channel on this block that can receive a signal
    List<String> cable$getSinkChannels(Level level, BlockPos pos);

    // * Channels belonging to one sub target, used for scroll cycling
    default List<String> cable$getSinkChannels(final Level level, final BlockPos pos, @Nullable final String subTarget) {
        return cable$getSinkChannels(level, pos);
    }

    @Nullable
    default String cable$nextSinkChannel(
            final Level level,
            final BlockPos pos,
            @Nullable final String subTarget,
            final String current,
            final boolean forward
    ) {
        final List<String> channels = cable$getSinkChannels(level, pos, subTarget);
        if (channels.isEmpty()) {
            return null;
        }

        final int index = channels.indexOf(current);
        if (index == -1) {
            return channels.getFirst();
        }
        return channels.get(Math.floorMod(index + (forward ? 1 : -1), channels.size()));
    }

    // * Push a value into the named channel, returns false when the channel is gone
    boolean cable$applySinkSignal(Level level, BlockPos pos, String channel, int signal);
}
