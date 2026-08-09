package edn.lakeopossmc.drivebysable.cable;

import edn.lakeopossmc.drivebysable.util.CableOutlineBox;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

// --- CONTRACT FOR BLOCKS MADE OF SEPARATELY TARGETABLE PARTS --- //
// * A dashpanel is one block but many modules
public interface SubTargetCableEndpoint {

    // * Sub target currently under the players crosshair
    @Nullable
    String cable$pickSubTarget(Level level, BlockPos pos, Player player);

    // * All sub targets on this block, used for validation
    List<String> cable$getSubTargets(Level level, BlockPos pos);

    // * Resolve which sub target owns a stored channel name
    @Nullable
    String cable$subTargetForChannel(Level level, BlockPos pos, String channel);

    // * Can this sub target act as a cable source
    boolean cable$isSourceSubTarget(Level level, BlockPos pos, String subTarget);

    // * Can this sub target act as a cable sink
    boolean cable$isSinkSubTarget(Level level, BlockPos pos, String subTarget);

    // --- CLIENT ONLY --- //
    // * World space oriented boxes wrapping the sub target, used for outlines
    List<CableOutlineBox> cable$getSubTargetOutline(Level level, BlockPos pos, String subTarget);
}
