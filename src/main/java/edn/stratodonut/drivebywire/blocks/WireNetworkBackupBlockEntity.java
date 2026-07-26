package edn.stratodonut.drivebywire.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

// --- COMPAT SHIM FOR MODS BUILT AGAINST THE ORIGINAL DRIVEBYWIRE --- //
// * NetworkBackupDriveBlockEntity extends this instead of BlockEntity
public abstract class WireNetworkBackupBlockEntity extends BlockEntity {
    protected CompoundTag pendingBackupData;
    protected boolean needsRestore;
    protected int restoreRetryCooldown;
    protected int restoreAttempts;

    protected WireNetworkBackupBlockEntity(final BlockEntityType<?> type, final BlockPos pos, final BlockState blockState) {
        super(type, pos, blockState);
    }
}