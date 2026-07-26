package edn.lakeopossmc.drivebysable.cable;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import dev.ryanhcode.sable.sublevel.plot.PlotChunkHolder;
import edn.lakeopossmc.drivebysable.blocks.NetworkBackupDriveBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

// --- ENFORCES BACKUP DRIVE PRESENCE FOR THIRD PARTY CAPTURE --- //
// * Standard create schematics already require a real Backup Drive block
// * Third party compat should not be able to bypass that
public final class BackupDriveRequirement {
    private BackupDriveRequirement() {
    }

    // * Whole sublevel is searched
    public static boolean existsInSameSubLevel(final Level level, final BlockPos anchorPos) {
        final SubLevel subLevel = Sable.HELPER.getContaining(level, anchorPos);
        if (subLevel == null) {
            return false;
        }

        final LevelPlot plot = subLevel.getPlot();
        for (final PlotChunkHolder holder : plot.getLoadedChunks()) {
            for (final BlockEntity blockEntity : holder.getChunk().getBlockEntities().values()) {
                if (blockEntity instanceof NetworkBackupDriveBlockEntity) {
                    return true;
                }
            }
        }
        return false;
    }

    // * Used where the exact set of positions is already known
    public static boolean existsAmong(final Level level, final Iterable<BlockPos> positions) {
        for (final BlockPos pos : positions) {
            if (level.getBlockEntity(pos) instanceof NetworkBackupDriveBlockEntity) {
                return true;
            }
        }
        return false;
    }
}