package edn.stratodonut.drivebywire.network;

import edn.lakeopossmc.drivebysable.network.CableNetworkFullSyncPacket;
import net.minecraft.server.level.ServerPlayer;

// --- COMPAT SHIM FOR MODS BUILT AGAINST THE ORIGINAL DRIVEBYWIRE --- //
public final class WireNetworkFullSyncPacket {
    private WireNetworkFullSyncPacket() {
    }

    public static void sendTo(final ServerPlayer player) {
        CableNetworkFullSyncPacket.sendTo(player);
    }
}