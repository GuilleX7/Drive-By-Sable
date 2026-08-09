package edn.lakeopossmc.drivebysable.compat.toolgun;

import dev.ryanhcode.sable.api.schematic.SubLevelSchematicSerializationContext;
import edn.lakeopossmc.drivebysable.cable.BackupDriveRequirement;
import edn.lakeopossmc.drivebysable.cable.CableNetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;

// --- REFLECTION SURFACE FOR THE AERONAUTICS TOOLGUN --- //
public final class ToolgunCableApi {
    private final CableNetworkManager real;

    private ToolgunCableApi(final CableNetworkManager real) {
        this.real = real;
    }

    public static ToolgunCableApi get(final Level level) {
        return new ToolgunCableApi(CableNetworkManager.get(level));
    }

    // * A sublevel with connections but no backup drive saves an empty snapshot
    public CableNetworkManager.BackupSnapshot createBackupSnapshot(
            final Level level,
            final BlockPos backupPos,
            final Direction savedFacing
    ) {
        final CableNetworkManager.BackupSnapshot snapshot = this.real.createBackupSnapshot(level, backupPos, savedFacing);
        if (snapshot.internalConnections() > 0 && !BackupDriveRequirement.existsInSameSubLevel(level, backupPos)) {
            return new CableNetworkManager.BackupSnapshot(new CompoundTag(), 0, snapshot.internalConnections());
        }
        return snapshot;
    }

    public CableNetworkManager.RestoreResult restoreBackupSnapshot(
            final Level level,
            final BlockPos backupBlockPos,
            final Direction facing,
            final CompoundTag snapshot
    ) {
        return this.real.restoreBackupSnapshot(level, backupBlockPos, facing, snapshot);
    }

    public static CompoundTag transformBackupSnapshotForPlacement(
            final CompoundTag snapshot,
            final BlockPos schematicBackupPos,
            final SubLevelSchematicSerializationContext context
    ) {
        return CableNetworkManager.transformBackupSnapshotForPlacement(snapshot, schematicBackupPos, context);
    }

    public static int computeWorldSignal(final Level level, final BlockPos pos) {
        return CableNetworkManager.computeWorldSignal(level, pos);
    }

    public static void trySetSignalAt(final Level level, final BlockPos source, final String channel, final int value) {
        CableNetworkManager.trySetSignalAt(level, source, channel, value);
    }
}