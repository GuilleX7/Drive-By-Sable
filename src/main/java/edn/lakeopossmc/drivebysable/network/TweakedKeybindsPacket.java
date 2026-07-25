package edn.lakeopossmc.drivebysable.network;

import edn.lakeopossmc.drivebysable.compat.PlayerTweakedKeybinds;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

// --- SYNC PLAYERS TWEAKED CONTROLLER KEYBOARD BINDS TO SERVER --- //
// * Only sent when the tweaked controllers mod is loaded and using custom mappings
public record TweakedKeybindsPacket(int[] keycodes) implements CustomPacketPayload {
    public static final Type<TweakedKeybindsPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("drivebysable", "tweaked_keybinds_sync"));

    public static final StreamCodec<FriendlyByteBuf, TweakedKeybindsPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public TweakedKeybindsPacket decode(final FriendlyByteBuf buf) {
            final int count = buf.readVarInt();
            final int[] keycodes = new int[count];
            for (int i = 0; i < count; i++) {
                keycodes[i] = buf.readVarInt();
            }
            return new TweakedKeybindsPacket(keycodes);
        }

        @Override
        public void encode(final FriendlyByteBuf buf, final TweakedKeybindsPacket packet) {
            buf.writeVarInt(packet.keycodes().length);
            for (final int keycode : packet.keycodes()) {
                buf.writeVarInt(keycode);
            }
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(final TweakedKeybindsPacket payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof final ServerPlayer player)) {
                return;
            }

            PlayerTweakedKeybinds.update(player.getUUID(), payload.keycodes());
        });
    }
}