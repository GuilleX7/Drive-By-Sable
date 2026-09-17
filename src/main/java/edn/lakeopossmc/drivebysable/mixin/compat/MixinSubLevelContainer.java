package edn.lakeopossmc.drivebysable.mixin.compat;

import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.SubLevel;
import edn.lakeopossmc.drivebysable.cable.CableNetworkManager;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// --- A FRESHLY ALLOCATED PLOT STARTS WITH NO WIRING --- //
// * Assembly, splitting, and every schematic paste go through here
@Mixin(value = SubLevelContainer.class, remap = false)
public abstract class MixinSubLevelContainer {
    @Inject(method = "allocateNewSubLevel", at = @At("RETURN"))
    private void drivebysable$clearReusedPlot(final Pose3d pose, final CallbackInfoReturnable<SubLevel> cir) {
        final SubLevel subLevel = cir.getReturnValue();
        if (subLevel == null || !(((SubLevelContainer) (Object) this).getLevel() instanceof final ServerLevel level)) {
            return;
        }

        // * Runs before moveBlocks, so the blocks being assembled are remapped in afterwards
        CableNetworkManager.purgeRemovedPlot(
                level,
                subLevel.getPlot(),
                "plot reused by new sub-level " + subLevel.getUniqueId()
        );
    }
}
