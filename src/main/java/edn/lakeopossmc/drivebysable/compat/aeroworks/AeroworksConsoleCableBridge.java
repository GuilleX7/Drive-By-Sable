package edn.lakeopossmc.drivebysable.compat.aeroworks;

import com.mred231.aeroworks.compat.drivebywire.ConsoleWireChannels;
import com.mred231.aeroworks.content.controls.ConsoleBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

// --- BRIDGES AEROWORKS CONSOLES INTO CABLE NETWORK --- //
// * Aeroworks channel list is dynamic per console
public final class AeroworksConsoleCableBridge {
    private AeroworksConsoleCableBridge() {
    }

    public static List<String> getChannels(final Level level, final BlockPos pos) {
        final ConsoleBlockEntity console = getConsole(level, pos);
        return console == null ? List.of() : ConsoleWireChannels.channelsFor(console);
    }

    public static String nextChannel(final Level level, final BlockPos pos, final String current, final boolean forward) {
        final ConsoleBlockEntity console = getConsole(level, pos);
        return console == null ? null : ConsoleWireChannels.nextChannel(console, current, forward);
    }

    @Nullable
    private static ConsoleBlockEntity getConsole(final Level level, final BlockPos pos) {
        return level.getBlockEntity(pos) instanceof final ConsoleBlockEntity console ? console : null;
    }
}