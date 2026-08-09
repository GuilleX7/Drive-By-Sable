package edn.lakeopossmc.drivebysable.cable.graph;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

// --- ONE SINK ENDPOINT IN THE NETWORK --- //
// * Tracks each source input feeding this sink face or module channel
public class CableNetworkNode {
    private final Map<InputKey, Integer> inputs = new HashMap<>();
    private final long position;
    private final int direction;

    public CableNetworkNode(final BlockPos pos, final Direction direction) {
        this(pos.asLong(), direction.get3DDataValue());
    }

    public CableNetworkNode(final long position, final int direction) {
        this.position = position;
        this.direction = direction;
    }

    // * Zero signal just clears the input
    public boolean setInput(final InputKey key, final int signal) {
        if (signal <= 0) {
            return inputs.remove(key) != null;
        }

        final Integer previous = inputs.put(key, signal);
        return previous == null || previous != signal;
    }

    public boolean isEmpty() {
        return inputs.isEmpty();
    }

    // * Strongest input wins, like vanilla redstone
    public int getSignal() {
        return inputs.values().stream().max(Comparator.naturalOrder()).orElse(0);
    }

    public long getPosition() {
        return position;
    }

    public int getDirection() {
        return direction;
    }

    public record InputKey(long sourcePos, String channel) {
    }

    // --- SINK ADDRESS --- //
    // * A sink is either a block face (sinkChannel empty) or a named module channel
    public record CableNetworkSink(long position, int direction, String sinkChannel) {
        public static final String BLOCK_FACE = "";
        private static final int CANONICAL_MODULE_DIRECTION = Direction.UP.get3DDataValue();

        public CableNetworkSink {
            sinkChannel = sinkChannel == null ? BLOCK_FACE : sinkChannel;
        }

        // * Legacy constructor, block face sinks only
        public CableNetworkSink(final long position, final int direction) {
            this(position, direction, BLOCK_FACE);
        }

        public static CableNetworkSink of(final BlockPos pos, final Direction direction) {
            return new CableNetworkSink(pos.asLong(), direction.get3DDataValue(), BLOCK_FACE);
        }

        // * Sub target sink, direction is ignored for these
        public static CableNetworkSink ofModule(final BlockPos pos, final String sinkChannel) {
            return new CableNetworkSink(pos.asLong(), CANONICAL_MODULE_DIRECTION, sinkChannel);
        }

        public static CableNetworkSink of(final BlockPos pos, final Direction direction, final String sinkChannel) {
            return sinkChannel == null || sinkChannel.isEmpty()
                    ? of(pos, direction)
                    : ofModule(pos, sinkChannel);
        }

        public boolean isModule() {
            return !sinkChannel.isEmpty();
        }

        public BlockPos blockPos() {
            return BlockPos.of(position);
        }

        public Direction facing() {
            return Direction.from3DDataValue(direction);
        }

        // * Key used for the module node map, direction dropped
        public ModuleSinkKey moduleKey() {
            return new ModuleSinkKey(position, sinkChannel);
        }
    }

    // --- MODULE NODE KEY --- //
    // * Module sinks are addressed by position plus channel
    public record ModuleSinkKey(long position, String channel) {
        public static ModuleSinkKey of(final BlockPos pos, final String channel) {
            return new ModuleSinkKey(pos.asLong(), channel);
        }

        public BlockPos blockPos() {
            return BlockPos.of(position);
        }
    }
}
