package edn.stratodonut.drivebywire.wire;

import edn.lakeopossmc.drivebysable.cable.BackupDriveRequirement;
import edn.lakeopossmc.drivebysable.cable.CableNetworkManager;
import edn.lakeopossmc.drivebysable.cable.graph.CableNetworkNode.CableNetworkSink;
import edn.stratodonut.drivebywire.wire.graph.WireNetworkNode;
import edn.stratodonut.drivebywire.wire.graph.WireNetworkNode.WireNetworkSink;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import dev.ryanhcode.sable.api.schematic.SubLevelSchematicSerializationContext;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

// --- COMPAT SHIM FOR MODS BUILT AGAINST THE ORIGINAL DRIVEBYWIRE --- //
// * Kept under the original package and class name so compat mods resolve
public final class WireNetworkManager {
    private final CableNetworkManager real;

    private WireNetworkManager(final CableNetworkManager real) {
        this.real = real;
    }

    public static WireNetworkManager get(final Level level) {
        return new WireNetworkManager(CableNetworkManager.get(level));
    }

    // * Converts the real sink type into the shim
    public Map<Long, Map<String, Set<WireNetworkSink>>> getNetwork() {
        final Map<Long, Map<String, Set<WireNetworkSink>>> converted = new HashMap<>();
        for (final Map.Entry<Long, Map<String, Set<CableNetworkSink>>> sourceEntry : this.real.getNetwork().entrySet()) {
            final Map<String, Set<WireNetworkSink>> perChannel = new HashMap<>();
            for (final Map.Entry<String, Set<CableNetworkSink>> channelEntry : sourceEntry.getValue().entrySet()) {
                perChannel.put(
                        channelEntry.getKey(),
                        channelEntry.getValue().stream().map(WireNetworkSink::from).collect(Collectors.toSet())
                );
            }
            converted.put(sourceEntry.getKey(), perChannel);
        }
        return converted;
    }

    // * Reflection based callers record is returned directly here
    // * If sublevel has connections, Backup Drive must exist
    public CableNetworkManager.BackupSnapshot createBackupSnapshot(final Level level, final BlockPos backupPos, final Direction savedFacing) {
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

    public static boolean hasConnection(
            final Level level,
            final BlockPos source,
            final BlockPos sinkPos,
            final Direction sinkDirection,
            final String channel
    ) {
        return CableNetworkManager.hasConnection(level, source, sinkPos, sinkDirection, channel);
    }

    public static ConnectionResult createConnection(
            final Level level,
            final BlockPos source,
            final BlockPos sinkPos,
            final Direction sinkDirection,
            final String channel
    ) {
        return ConnectionResult.from(CableNetworkManager.createConnection(level, source, sinkPos, sinkDirection, channel));
    }

    // * Mirrors the real enum
    public enum ConnectionResult {
        OK(""),
        FAIL_EXISTS("Connection already exists!"),
        FAIL_TOO_MANY_SOURCES("Exceeded source limit for this structure!"),
        FAIL_TOO_MANY_SINKS("Exceeded sink limit for this source!"),
        FAIL_SAME_BLOCK("Source and sink must be different blocks!"),
        FAIL_INVALID_CHANNEL("This channel is not available on this source!");

        private final String description;

        ConnectionResult(final String description) {
            this.description = description;
        }

        private static ConnectionResult from(final CableNetworkManager.ConnectionResult real) {
            return ConnectionResult.valueOf(real.name());
        }

        public boolean isSuccess() {
            return this == OK;
        }

        public String getDescription() {
            return description;
        }
    }
}