package edn.lakeopossmc.drivebysable.network;

import edn.lakeopossmc.drivebysable.DriveBySableMod;
import edn.lakeopossmc.drivebysable.blocks.NetworkBackupDriveBlockEntity;
import edn.lakeopossmc.drivebysable.menu.BackupDriveMenu;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;

// --- PLAYER CLEARED A BACKUP DRIVE --- //
// * Discards the stored snapshot and puts the region back to its defaults
public record BackupDriveResetPacket(BlockPos drivePos) implements CustomPacketPayload {

    public static final Type<BackupDriveResetPacket> TYPE =
            new Type<>(DriveBySableMod.asResource("backup_drive_reset"));

    public static final StreamCodec<ByteBuf, BackupDriveResetPacket> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, BackupDriveResetPacket::drivePos,
                    BackupDriveResetPacket::new
            );

    @Override
    public Type<BackupDriveResetPacket> type() {
        return TYPE;
    }

    public static void handle(final BackupDriveResetPacket payload, final IPayloadContext context) {
        if (!(context.player() instanceof final ServerPlayer player)) {
            return;
        }

        // * Authorised by the open menu
        if (!(player.containerMenu instanceof final BackupDriveMenu menu)
                || !menu.getDrivePos().equals(payload.drivePos())) {
            return;
        }

        final NetworkBackupDriveBlockEntity drive = menu.getDrive();
        if (drive == null || drive.isRemoved()) {
            return;
        }

        final Level level = drive.getLevel();
        final boolean hadData = drive.hasStoredSnapshot();

        drive.clearStoredSnapshot();

        if (hadData && level != null) {
            level.playSound(
                    null,
                    menu.getDrivePos(),
                    SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(),
                    SoundSource.BLOCKS,
                    0.7F,
                    1.0F
            );
        }
    }
}