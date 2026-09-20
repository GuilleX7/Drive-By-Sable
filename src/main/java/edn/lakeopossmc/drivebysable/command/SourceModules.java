package edn.lakeopossmc.drivebysable.command;

import edn.lakeopossmc.drivebysable.cable.CableNetworkManager.IncomingConnection;
import edn.lakeopossmc.drivebysable.cable.ModuleSinkTarget;
import edn.lakeopossmc.drivebysable.cable.SubTargetCableEndpoint;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.LinkedHashSet;
import java.util.Set;

// --- THE NAMED PARTS OF A BLOCK --- //
public final class SourceModules {
    private SourceModules() {
    }

    public static Set<String> of(final Level level, final BlockPos pos) {
        final Block block = level.getBlockState(pos).getBlock();
        final Set<String> modules = new LinkedHashSet<>();

        if (block instanceof final SubTargetCableEndpoint endpoint) {
            modules.addAll(endpoint.cable$getSubTargets(level, pos));
        }
        if (modules.isEmpty() && block instanceof final ModuleSinkTarget sinkTarget) {
            modules.addAll(sinkTarget.cable$getSinkChannels(level, pos));
        }
        return modules;
    }

    public static String moduleForSinkChannel(final Level level, final BlockPos pos, final String channel) {
        if (channel.isEmpty()) {
            return "";
        }

        final Block block = level.getBlockState(pos).getBlock();
        if (block instanceof final SubTargetCableEndpoint endpoint
                && block instanceof final ModuleSinkTarget sinkTarget) {
            for (final String subTarget : endpoint.cable$getSubTargets(level, pos)) {
                if (sinkTarget.cable$getSinkChannels(level, pos, subTarget).contains(channel)) {
                    return subTarget;
                }
            }
        }
        return channel;
    }

    public static boolean sendsFrom(
            final Level level,
            final BlockPos pos,
            final String channel,
            final String module
    ) {
        final Block block = level.getBlockState(pos).getBlock();
        if (block instanceof final SubTargetCableEndpoint endpoint) {
            return module.equals(endpoint.cable$subTargetForChannel(level, pos, channel));
        }
        return false;
    }

    public static String moduleForChannel(final Level level, final BlockPos pos, final String channel) {
        final Block block = level.getBlockState(pos).getBlock();
        if (block instanceof final SubTargetCableEndpoint endpoint) {
            final String module = endpoint.cable$subTargetForChannel(level, pos, channel);
            return module == null ? "" : module;
        }
        return "";
    }

    // * Whether a connection into this block lands on the named module
    public static boolean receivedBy(final Level level, final BlockPos pos, final IncomingConnection incoming, final String module) {
        return module.equals(moduleForSinkChannel(level, pos, incoming.sinkChannel()));
    }
}