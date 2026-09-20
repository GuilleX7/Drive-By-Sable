package edn.lakeopossmc.drivebysable.command;

import edn.lakeopossmc.drivebysable.cable.ModuleSinkTarget;
import edn.lakeopossmc.drivebysable.cable.MultiChannelCableSource;
import edn.lakeopossmc.drivebysable.cable.SubTargetCableEndpoint;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.Locale;

// --- WHAT KIND OF BLOCK THIS IS --- //
// * Panel: holds modules or module channels
// * Bus: many channels of both kinds, but no modules
// * Hub: many channels, none of them sink channels
// * Block: only the world channel, or nothing that can be told apart
public enum SourceKind {
    HUB,
    BUS,
    BLOCK,
    PANEL;

    public static SourceKind of(final Level level, final BlockPos pos) {
        final Block block = level.getBlockState(pos).getBlock();

        if (block instanceof final SubTargetCableEndpoint endpoint
                && !endpoint.cable$getSubTargets(level, pos).isEmpty()) {
            return PANEL;
        }

        final boolean sends = block instanceof MultiChannelCableSource;
        final boolean receives = block instanceof ModuleSinkTarget;

        if (sends && receives) {
            return BUS;
        }
        if (sends) {
            return HUB;
        }
        if (receives) {
            return PANEL;
        }
        return BLOCK;
    }

    @Override
    public String toString() {
        return name().toLowerCase(Locale.ROOT);
    }
}