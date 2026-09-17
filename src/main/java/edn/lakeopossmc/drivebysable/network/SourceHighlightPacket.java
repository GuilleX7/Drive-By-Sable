package edn.lakeopossmc.drivebysable.network;

import edn.lakeopossmc.drivebysable.DriveBySableMod;
import edn.lakeopossmc.drivebysable.client.SourceHighlightClient;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

// --- WHAT /dbs highlight SHOULD OUTLINE --- //
public record SourceHighlightPacket(
        List<BlockPos> sources,
        List<BlockPos> outputs,
        List<Integer> outputOwners,
        int ticks
) implements CustomPacketPayload {

    public static final int INFINITE = -1;

    public static final Type<SourceHighlightPacket> TYPE =
            new Type<>(DriveBySableMod.asResource("source_highlight"));

    public static final StreamCodec<ByteBuf, SourceHighlightPacket> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list()), SourceHighlightPacket::sources,
                    BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list()), SourceHighlightPacket::outputs,
                    ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), SourceHighlightPacket::outputOwners,
                    ByteBufCodecs.VAR_INT, SourceHighlightPacket::ticks,
                    SourceHighlightPacket::new
            );

    public static SourceHighlightPacket clear() {
        return new SourceHighlightPacket(List.of(), List.of(), List.of(), 0);
    }

    @Override
    public Type<SourceHighlightPacket> type() {
        return TYPE;
    }

    public static void handle(final SourceHighlightPacket payload, final IPayloadContext context) {
        context.enqueueWork(() -> SourceHighlightClient.show(
                payload.sources(),
                payload.outputs(),
                payload.outputOwners(),
                payload.ticks()
        ));
    }
}