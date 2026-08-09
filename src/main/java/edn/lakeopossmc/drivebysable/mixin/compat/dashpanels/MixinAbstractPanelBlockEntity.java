package edn.lakeopossmc.drivebysable.mixin.compat.dashpanels;

import edn.lakeopossmc.drivebysable.compat.dashpanels.DashPanelCableBridge;
import moth.boxxed.panels.api.panel.AbstractPanelBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// --- HOOK PANEL TICK FOR CABLE BRIDGE --- //
// * Pseudo since the mod may not be loaded
// * Dashpanels 2.x moved tick onto the abstract base
@Pseudo
@Mixin(AbstractPanelBlockEntity.class)
public abstract class MixinAbstractPanelBlockEntity {
    @Inject(method = "tick", at = @At("TAIL"))
    private void drivebysable$tickCableBridge(
            final Level level,
            final BlockPos blockPos,
            final BlockState blockState,
            final CallbackInfo ci
    ) {
        DashPanelCableBridge.tick(level, blockPos, (AbstractPanelBlockEntity) (Object) this);
    }
}
