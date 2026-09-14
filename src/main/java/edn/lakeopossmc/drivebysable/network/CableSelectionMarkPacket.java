package edn.lakeopossmc.drivebysable.network;

import edn.lakeopossmc.drivebysable.CableItems;
import edn.lakeopossmc.drivebysable.DriveBySableMod;
import edn.lakeopossmc.drivebysable.util.CableSelectionMark;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

// --- ASKS THE SERVER TO STAMP THE SELECTION ONTO THE HELD TOOL --- //
public record CableSelectionMarkPacket(boolean marked, long source, String module) implements CustomPacketPayload {

    public static final Type<CableSelectionMarkPacket> TYPE =
            new Type<>(DriveBySableMod.asResource("cable_selection_mark"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CableSelectionMarkPacket> STREAM_CODEC =
            StreamCodec.of(
                    (buffer, packet) -> {
                        buffer.writeBoolean(packet.marked());
                        buffer.writeLong(packet.source());
                        buffer.writeUtf(packet.module());
                    },
                    buffer -> new CableSelectionMarkPacket(
                            buffer.readBoolean(),
                            buffer.readLong(),
                            buffer.readUtf()
                    )
            );

    public static CableSelectionMarkPacket clear() {
        return new CableSelectionMarkPacket(false, 0L, "");
    }

    public static CableSelectionMarkPacket of(final BlockPos source, final String module) {
        return new CableSelectionMarkPacket(true, source.asLong(), module == null ? "" : module);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(final CableSelectionMarkPacket packet, final IPayloadContext context) {
        if (!(context.player() instanceof final ServerPlayer player)) {
            return;
        }

        // * Any older mark comes off first
        final Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            CableSelectionMark.remove(inventory.getItem(slot));
        }

        if (!packet.marked()) {
            return;
        }

        final ItemStack tool = heldTool(player);
        if (tool.isEmpty()) {
            return;
        }

        CableSelectionMark.put(
                tool,
                BlockPos.of(packet.source()),
                packet.module().isEmpty() ? null : packet.module(),
                player.level().dimension()
        );
    }

    // * Main hand first, so an offhand cable does not steal the mark from the one clicked
    private static ItemStack heldTool(final ServerPlayer player) {
        for (final InteractionHand hand : InteractionHand.values()) {
            final ItemStack stack = player.getItemInHand(hand);
            if (stack.is(CableItems.CABLE.get()) || stack.is(CableItems.CABLE_CUTTER.get())) {
                return stack;
            }
        }

        return ItemStack.EMPTY;
    }
}