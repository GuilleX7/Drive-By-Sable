package edn.lakeopossmc.drivebysable.command;

import edn.lakeopossmc.drivebysable.cable.CableNetworkManager;
import edn.lakeopossmc.drivebysable.cable.CableNetworkManager.IncomingConnection;
import edn.lakeopossmc.drivebysable.cable.graph.CableNetworkNode.CableNetworkSink;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// --- THE CHANNELS ON ONE ENDPOINT --- //
public final class CableChannels {
    private CableChannels() {
    }

    public static Map<String, List<CableNetworkSink>> sending(
            final ServerLevel level,
            final CableNetworkManager manager,
            final CableEndpoint endpoint
    ) {
        final Map<String, List<CableNetworkSink>> channels = new LinkedHashMap<>();
        manager.getConnections(endpoint.pos()).forEach((channel, sinks) -> {
            if (belongsTo(level, endpoint, channel)) {
                channels.put(channel, List.copyOf(sinks));
            }
        });
        return channels;
    }

    public static Map<String, List<IncomingConnection>> receiving(
            final ServerLevel level,
            final CableNetworkManager manager,
            final CableEndpoint endpoint
    ) {
        final Map<String, List<IncomingConnection>> channels = new LinkedHashMap<>();
        for (final IncomingConnection incoming : manager.getIncoming(endpoint.pos())) {
            if (endpoint.hasModule()
                    && !SourceModules.receivedBy(level, endpoint.pos(), incoming, endpoint.module())) {
                continue;
            }
            final String name = incoming.isModule() ? incoming.sinkChannel() : incoming.direction().getName();
            channels.computeIfAbsent(name, ignored -> new ArrayList<>()).add(incoming);
        }
        return channels;
    }

    public static int outputCount(final Map<String, List<CableNetworkSink>> sending) {
        final List<Long> seen = new ArrayList<>();
        for (final List<CableNetworkSink> sinks : sending.values()) {
            for (final CableNetworkSink sink : sinks) {
                if (!seen.contains(sink.position())) {
                    seen.add(sink.position());
                }
            }
        }
        return seen.size();
    }

    public static int sourceCount(final Map<String, List<IncomingConnection>> receiving) {
        final List<Long> seen = new ArrayList<>();
        for (final List<IncomingConnection> incoming : receiving.values()) {
            for (final IncomingConnection connection : incoming) {
                if (!seen.contains(connection.source().asLong())) {
                    seen.add(connection.source().asLong());
                }
            }
        }
        return seen.size();
    }

    public static int sourceCount(final List<IncomingConnection> incoming) {
        return sourceCount(Map.of("", incoming));
    }

    private static boolean belongsTo(final ServerLevel level, final CableEndpoint endpoint, final String channel) {
        if (endpoint.hasModule()) {
            return SourceModules.sendsFrom(level, endpoint.pos(), channel, endpoint.module());
        }
        return true;
    }
}