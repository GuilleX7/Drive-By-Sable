package edn.lakeopossmc.drivebysable.mixin.compat.aeroworks;

import com.mred231.aeroworks.content.controls.ConsoleBlock;
import edn.lakeopossmc.drivebysable.cable.MultiChannelCableSource;
import edn.lakeopossmc.drivebysable.compat.aeroworks.AeroworksConsoleCableBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

import java.util.List;

// --- MAKES THE AEROWORKS CONSOLE A DYNAMIC CABLE SOURCE --- //
// * Pseudo since Aeroworks may not be loaded
// * Targets ConsoleBlock
@Pseudo
@Mixin(ConsoleBlock.class)
public abstract class MixinAeroworksConsoleBlock implements MultiChannelCableSource {

    @Override
    public List<String> cable$getChannels(final Level level, final BlockPos pos) {
        return AeroworksConsoleCableBridge.getChannels(level, pos);
    }

    @Override
    public String cable$nextChannel(final Level level, final BlockPos pos, final String current, final boolean forward) {
        return AeroworksConsoleCableBridge.nextChannel(level, pos, current, forward);
    }
}