package edn.lakeopossmc.drivebysable.mixin.compat;

import dev.rew1nd.sableschematicapi.api.blueprint.BlueprintPlaceSession;
import dev.rew1nd.sableschematicapi.api.blueprint.BlueprintPlacedBlock;
import dev.rew1nd.sableschematicapi.api.blueprint.BlueprintSaveSession;
import dev.rew1nd.sableschematicapi.api.blueprint.BlueprintSavedBlock;
import edn.lakeopossmc.drivebysable.CableBlocks;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// --- ENFORCE BACKUP DRIVE FOR PHOTOMANCY BLUEPRINT COMPAT --- //
@Pseudo
@Mixin(targets = "dev.rew1nd.sableschematicapi.compat.drivebywire.DriveByWireBlueprintEvent", remap = false)
public abstract class MixinDriveByWireBlueprintEvent {
    @Inject(method = "onSaveAfterBlocks", at = @At("TAIL"))
    private void drivebysable$requireBackupDriveForSavedConnections(
            final BlueprintSaveSession session,
            final CompoundTag data,
            final CallbackInfo ci
    ) {
        if (!data.contains("connections", Tag.TAG_LIST)) {
            return;
        }

        final ListTag connections = data.getList("connections", Tag.TAG_COMPOUND);
        if (connections.isEmpty()) {
            return;
        }

        boolean hasBackupDrive = false;
        for (final BlueprintSavedBlock block : session.savedBlocks().blocks()) {
            if (block.state().is(CableBlocks.BACKUP_DRIVE.get())) {
                hasBackupDrive = true;
                break;
            }
        }

        if (!hasBackupDrive) {
            data.remove("connections");
            data.remove("skipped_connections");
        }
    }

    @Inject(method = "onPlaceAfterBlockEntities", at = @At("HEAD"))
    private void drivebysable$requireBackupDriveForPlacedConnections(
            final BlueprintPlaceSession session,
            final CompoundTag data,
            final CallbackInfo ci
    ) {
        if (!data.contains("connections", Tag.TAG_LIST)) {
            return;
        }

        final ListTag connections = data.getList("connections", Tag.TAG_COMPOUND);
        if (connections.isEmpty()) {
            return;
        }

        boolean hasBackupDrive = false;
        for (final BlueprintPlacedBlock block : session.placedBlocks().blocks()) {
            if (block.state().is(CableBlocks.BACKUP_DRIVE.get())) {
                hasBackupDrive = true;
                break;
            }
        }

        if (!hasBackupDrive) {
            data.remove("connections");
        }
    }
}