package edn.lakeopossmc.drivebysable.network;

import dev.ryanhcode.sable.sublevel.SubLevel;
import edn.lakeopossmc.drivebysable.DriveBySableMod;
import edn.lakeopossmc.drivebysable.blocks.NetworkBackupDriveBlockEntity;
import edn.lakeopossmc.drivebysable.cable.BackupDriveCapture;
import edn.lakeopossmc.drivebysable.menu.BackupDriveMenu;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

// --- REMEMBERS THE REGION A PLAYER SET UP --- //
// * Sent when the screen closes
public record BackupDriveRegionPacket(
        BlockPos drivePos,
        BlockPos offset,
        BlockPos size,
        int rotationSteps
) implements CustomPacketPayload {

    private static final double MAX_REACH_SQR = 64.0D * 64.0D;

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

        final NetworkBackupDriveBlockEntity drive =
                player.containerMenu instanceof final BackupDriveMenu menu
                        ? (menu.getDrivePos().equals(payload.drivePos()) ? menu.getDrive() : null)
                        : reachableDrive(player, payload.drivePos());

        if (drive == null || drive.isRemoved()) {
            return;
        }

        drive.setRegion(payload.offset(), payload.size(), payload.rotationSteps());
    }

    private static NetworkBackupDriveBlockEntity reachableDrive(final ServerPlayer player, final BlockPos drivePos) {
        final Level level = player.level();
        if (!level.isLoaded(drivePos)) {
            return null;
        }

        final SubLevel subLevel = BackupDriveCapture.subLevelOf(level, drivePos);
        final Vec3 playerPos = subLevel == null
                ? player.position()
                : subLevel.logicalPose().transformPositionInverse(player.position());

        if (playerPos.distanceToSqr(Vec3.atCenterOf(drivePos)) > MAX_REACH_SQR) {
            return null;
        }

        return level.getBlockEntity(drivePos) instanceof final NetworkBackupDriveBlockEntity drive ? drive : null;
    }
}