package edn.lakeopossmc.drivebysable.mixin.compat.toolgun;

import edn.lakeopossmc.drivebysable.blocks.NetworkBackupDriveBlockEntity;
import edn.lakeopossmc.drivebysable.compat.toolgun.ToolgunCableApi;
import edn.lakeopossmc.drivebysable.network.CableNetworkFullSyncPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// --- POINT THE TOOLGUN'S REFLECTION AT DRIVE-BY-SABLE --- //
// * Pseudo since mod may not be loaded
@Pseudo
@Mixin(targets = "com.enxv.aeronauticsstructuretool.compat.drivebywire.DriveByWireApiBridge", remap = false)
public abstract class MixinToolgunApiBridge {

    private static final String DRIVEBYSABLE$MANAGER = "edn.stratodonut.drivebywire.wire.WireNetworkManager";
    private static final String DRIVEBYSABLE$BACKUP_BE = "edn.stratodonut.drivebywire.blocks.WireNetworkBackupBlockEntity";
    private static final String DRIVEBYSABLE$FULL_SYNC = "edn.stratodonut.drivebywire.network.WireNetworkFullSyncPacket";

    // * The toolgun asks FML whether drivebywire is present
    @Inject(method = "isInstalled", at = @At("HEAD"), cancellable = true, require = 0)
    private static void drivebysable$reportInstalled(final CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(true);
    }

    // * Every Class.forName in the bridge resolves through here instead
    @Redirect(
            method = {
                    "captureSnapshot",
                    "transformForPlacement",
                    "restoreSnapshot",
                    "clearPendingBackup",
                    "sendFullSync",
                    "computeWorldSignal",
                    "pushWorldSignal"
            },
            at = @At(value = "INVOKE", target = "Ljava/lang/Class;forName(Ljava/lang/String;)Ljava/lang/Class;"),
            require = 0,
            expect = 0
    )
    private static Class<?> drivebysable$resolveApiClass(final String name) throws ClassNotFoundException {
        return switch (name) {
            case DRIVEBYSABLE$MANAGER -> ToolgunCableApi.class;

            case DRIVEBYSABLE$BACKUP_BE -> NetworkBackupDriveBlockEntity.class;

            // * Static sendTo(ServerPlayer)
            case DRIVEBYSABLE$FULL_SYNC -> CableNetworkFullSyncPacket.class;

            default -> Class.forName(name);
        };
    }
}