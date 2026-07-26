package edn.stratodonut.drivebywire;

import edn.lakeopossmc.drivebysable.CableBlockEntities;
import edn.lakeopossmc.drivebysable.blocks.NetworkBackupDriveBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;

// --- COMPAT SHIM FOR MODS BUILT AGAINST THE ORIGINAL DRIVEBYWIRE --- //
// * Sable photomancy compiles directly against this class
public final class WireBlockEntities {
    private WireBlockEntities() {
    }

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<NetworkBackupDriveBlockEntity>> BACKUP_BLOCK =
            CableBlockEntities.BACKUP_DRIVE;
}