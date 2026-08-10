package edn.lakeopossmc.drivebysable.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsClient;
import edn.lakeopossmc.drivebysable.client.CableHoverTip;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

// --- KEEP THE CABLE TOOL TIP VISIBLE WHILE SNEAKING --- //
@Mixin(ValueSettingsClient.class)
public abstract class MixinValueSettingsClient {

    @ModifyExpressionValue(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lcom/simibubi/create/foundation/blockEntity/behaviour/ValueSettingsInputHandler;canInteract(Lnet/minecraft/world/entity/player/Player;)Z"
        ),
        remap = false,
        require = 0,
        expect = 0
    )
    private boolean drivebysable$keepCableTipWhileSneaking(final boolean original) {
        // * Spectators are excluded before the flag is raised
        return original || CableHoverTip.isShowing();
    }
}