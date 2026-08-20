package edn.lakeopossmc.drivebysable.network;

import edn.lakeopossmc.drivebysable.DriveBySableMod;
import edn.lakeopossmc.drivebysable.blocks.NetworkBackupDriveBlockEntity;
import edn.lakeopossmc.drivebysable.menu.BackupDriveMenu;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;

// --- REMEMBERS THE REGION A PLAYER SET UP --- //
// * Sent when the screen closes
public record BackupDriveRegionPacket(
        BlockPos drivePos,
        BlockPos offset,
        BlockPos size,
        int rotationSteps
) implements CustomPacketPayload {

    public static final Type<BackupDriveRegionPacket> TYPE =
            new Type<>(DriveBySableMod.asResource("backup_drive_region"));

    public static final StreamCodec<ByteBuf, BackupDriveRegionPacket> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, BackupDriveRegionPacket::drivePos,
                    BlockPos.STREAM_CODEC, BackupDriveRegionPacket::offset,
                    BlockPos.STREAM_CODEC, BackupDriveRegionPacket::size,
                    ByteBufCodecs.VAR_INT, BackupDriveRegionPacket::rotationSteps,
                    BackupDriveRegionPacket::new
            );

    // * Convenience for the screen
    public static BackupDriveRegionPacket of(
            final BlockPos drivePos,
            final int[] offset,
            final int[] size,
            final int rotationSteps
    ) {
        return new BackupDriveRegionPacket(
                drivePos,
                new BlockPos(offset[0], offset[1], offset[2]),
                new BlockPos(size[0], size[1], size[2]),
                rotationSteps
        );
    }

    @Override
    public Type<BackupDriveRegionPacket> type() {
        return TYPE;
    }

    public static void handle(final BackupDriveRegionPacket payload, final IPayloadContext context) {
        if (!(context.player() instanceof final ServerPlayer player)) {
            return;
        }

        // * Authorisation comes from the open menu
        if (!(player.containerMenu instanceof final BackupDriveMenu menu)
                || !menu.getDrivePos().equals(payload.drivePos())) {
            return;
        }

        final NetworkBackupDriveBlockEntity drive = menu.getDrive();
        if (drive == null || drive.isRemoved()) {
            return;
        }

        drive.setRegion(payload.offset(), payload.size(), payload.rotationSteps());
    }
}