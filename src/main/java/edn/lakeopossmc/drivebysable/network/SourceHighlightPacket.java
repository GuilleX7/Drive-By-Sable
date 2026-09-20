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
        List<HighlightEndpoint> targets,
        List<HighlightEndpoint> connected,
        List<Integer> owners,
        int ticks
) implements CustomPacketPayload {

    public static final int INFINITE = -1;

    public record HighlightEndpoint(BlockPos pos, String module, boolean source) {
        public static final StreamCodec<ByteBuf, HighlightEndpoint> STREAM_CODEC =
                StreamCodec.composite(
                        BlockPos.STREAM_CODEC, HighlightEndpoint::pos,
                        ByteBufCodecs.STRING_UTF8, HighlightEndpoint::module,
                        ByteBufCodecs.BOOL, HighlightEndpoint::source,
                        HighlightEndpoint::new
                );
    }

    public static final Type<SourceHighlightPacket> TYPE =
            new Type<>(DriveBySableMod.asResource("source_highlight"));

    public static final StreamCodec<ByteBuf, SourceHighlightPacket> STREAM_CODEC =
            StreamCodec.composite(
                    HighlightEndpoint.STREAM_CODEC.apply(ByteBufCodecs.list()), SourceHighlightPacket::targets,
                    HighlightEndpoint.STREAM_CODEC.apply(ByteBufCodecs.list()), SourceHighlightPacket::connected,
                    ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), SourceHighlightPacket::owners,
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
        context.enqueueWork(() -> SourceHighlightClient.show(payload));
    }
}