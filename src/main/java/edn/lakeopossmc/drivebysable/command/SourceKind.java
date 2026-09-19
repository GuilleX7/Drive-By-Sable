package edn.lakeopossmc.drivebysable.command;

import edn.lakeopossmc.drivebysable.blocks.AbstractDirectionalHubBlock;
import edn.lakeopossmc.drivebysable.blocks.CableTypewriterHubBlock;
import edn.lakeopossmc.drivebysable.blocks.IntegratedSensorBusBlock;
import edn.lakeopossmc.drivebysable.blocks.MultiChannelCableBusBlock;
import edn.lakeopossmc.drivebysable.cable.MultiChannelCableSource;
import edn.lakeopossmc.drivebysable.cable.SubTargetCableEndpoint;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.Locale;

// --- WHAT KIND OF SOURCE A BLOCK IS --- //
// * Named in command output so it is clear what is being read at a glance
public enum SourceKind {
    HUB,
    BUS,
    MODULES,
    BLOCK;

    public static SourceKind of(final Level level, final BlockPos pos) {
        final Block block = level.getBlockState(pos).getBlock();

        if (block instanceof AbstractDirectionalHubBlock || block instanceof CableTypewriterHubBlock) {
            return HUB;
        }
        if (block instanceof MultiChannelCableBusBlock || block instanceof IntegratedSensorBusBlock) {
            return BUS;
        }
        // * Anything that splits itself into named parts or channels
        if (block instanceof SubTargetCableEndpoint || block instanceof MultiChannelCableSource) {
            return MODULES;
        }
        // * A plain block
        return BLOCK;
    }

    @Override
    public String toString() {
        return name().toLowerCase(Locale.ROOT);
    }
}