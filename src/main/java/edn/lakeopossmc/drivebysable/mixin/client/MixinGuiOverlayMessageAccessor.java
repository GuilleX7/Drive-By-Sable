package edn.lakeopossmc.drivebysable.mixin.client;

import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

// --- EXPOSE THE ACTION BAR FADE TIMER --- //
// * setOverlayMessage always stamps this at 60
// * Writing the field directly is the only way to cut that short
@Mixin(Gui.class)
public interface MixinGuiOverlayMessageAccessor {
    @Accessor("overlayMessageTime")
    void drivebysable$setOverlayMessageTime(int ticks);
}