package edn.lakeopossmc.drivebysable.network;

import edn.lakeopossmc.drivebysable.DriveBySableMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;
import edn.lakeopossmc.drivebysable.client.BackupDriveHighlightClient;

// --- WHERE A LOAD PUT ITS CONNECTIONS --- //
// * Sent alongside the report
public record BackupDriveHighlightPacket(
        BlockPos drivePos,
        List<BlockPos> sources,
        List<String> modules
) implements CustomPacketPayload {

    public static final Type<BackupDriveHighlightPacket> TYPE =
            new Type<>(DriveBySableMod.asResource("backup_drive_highlight"));

    public static final StreamCodec<ByteBuf, BackupDriveHighlightPacket> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, BackupDriveHighlightPacket::drivePos,
                    BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list()), BackupDriveHighlightPacket::sources,
                    ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), BackupDriveHighlightPacket::modules,
                    BackupDriveHighlightPacket::new
            );

    @Override
    public Type<BackupDriveHighlightPacket> type() {
        return TYPE;
    }

    public static void handle(final BackupDriveHighlightPacket payload, final IPayloadContext context) {
        // * Handed straight to the client
        context.enqueueWork(() -> BackupDriveHighlightClient.show(
                payload.drivePos(),
                payload.sources(),
                payload.modules()
        ));
    }

}