package edn.lakeopossmc.drivebysable.network;

import edn.lakeopossmc.drivebysable.DriveBySableMod;
import edn.lakeopossmc.drivebysable.compat.keytranslator.PlayerMovementKeybinds;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

// --- SYNC PLAYERS REAL MOVEMENT KEYBINDS TO SERVER --- //
public record MovementKeybindsPacket(int forward, int back, int left, int right, int jump, int sneak) implements CustomPacketPayload {
    public static final Type<MovementKeybindsPacket> TYPE = new Type<>(DriveBySableMod.asResource("movement_keybinds_sync"));
    public static final StreamCodec<ByteBuf, MovementKeybindsPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, MovementKeybindsPacket::forward,
            ByteBufCodecs.VAR_INT, MovementKeybindsPacket::back,
            ByteBufCodecs.VAR_INT, MovementKeybindsPacket::left,
            ByteBufCodecs.VAR_INT, MovementKeybindsPacket::right,
            ByteBufCodecs.VAR_INT, MovementKeybindsPacket::jump,
            ByteBufCodecs.VAR_INT, MovementKeybindsPacket::sneak,
            MovementKeybindsPacket::new
    );

    @Override
    public Type<MovementKeybindsPacket> type() {
        return TYPE;
    }

    public static void handle(final MovementKeybindsPacket payload, final IPayloadContext context) {
        if (!(context.player() instanceof final ServerPlayer player)) {
            return;
        }

        PlayerMovementKeybinds.update(player.getUUID(), payload.forward(), payload.back(), payload.left(), payload.right(), payload.jump(), payload.sneak());
    }
}