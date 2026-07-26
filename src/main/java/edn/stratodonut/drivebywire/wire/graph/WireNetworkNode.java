package edn.stratodonut.drivebywire.wire.graph;

import edn.lakeopossmc.drivebysable.cable.graph.CableNetworkNode;

// --- COMPAT SHIM FOR MODS BUILT AGAINST THE ORIGINAL DRIVEBYWIRE --- //
// * Sable photomancy compiles directly against this type
public final class WireNetworkNode {
    private WireNetworkNode() {
    }

    public record WireNetworkSink(long position, int direction) {
        public static WireNetworkSink from(final CableNetworkNode.CableNetworkSink real) {
            return new WireNetworkSink(real.position(), real.direction());
        }
    }
}