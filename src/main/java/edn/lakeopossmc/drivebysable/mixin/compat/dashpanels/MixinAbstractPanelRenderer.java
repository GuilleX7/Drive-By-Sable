package edn.lakeopossmc.drivebysable.mixin.compat.dashpanels;

import com.mojang.blaze3d.vertex.PoseStack;
import edn.lakeopossmc.drivebysable.client.ClientCableNetworkHandler;
import moth.boxxed.panels.api.module.Module;
import moth.boxxed.panels.api.panel.AbstractPanelBlockEntity;
import moth.boxxed.panels.api.panel.AbstractPanelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.function.BiConsumer;

// --- DRAWS CABLE MODULE OUTLINES IN THE PANEL'S OWN POSE --- //
// * Pseudo since the mod may not be loaded
@Pseudo
@Mixin(AbstractPanelRenderer.class)
public abstract class MixinAbstractPanelRenderer {

    // * Selection boxes sit slightly proud of the module
    private static final double DRIVEBYSABLE$OUTLINE_INFLATE = 0.002D;
    // * In block units, so 1/32 is half a pixel
    private static final double DRIVEBYSABLE$OUTLINE_THICKNESS = 1 / 32.0D;
    private static final AABB DRIVEBYSABLE$FALLBACK_SHAPE = new AABB(0, 0, 0, 1 / 16D, 1 / 16D, 1 / 16D);

    @Inject(method = "renderModules", at = @At("TAIL"))
    private void drivebysable$renderCableOutlines(
            final AbstractPanelBlockEntity panel,
            final float partialTick,
            final PoseStack poseStack,
            final MultiBufferSource bufferSource,
            final int packedLight,
            final int packedOverlay,
            final CallbackInfo ci
    ) {
        final Map<String, Integer> outlines = ClientCableNetworkHandler.moduleOutlinesFor(panel.getBlockPos());
        if (outlines.isEmpty()) {
            return;
        }

        // * Same chain dashpanels walks before calling Module#renderOutline
        poseStack.pushPose();
        panel.renderTransform(poseStack);

        final BiConsumer<Module, PoseStack> individualModuleTransform = panel.getIndividualModuleTransform();
        outlines.forEach((moduleName, color) -> {
            final Module module = panel.getModules().normalGet(moduleName);
            if (module == null) {
                return;
            }

            poseStack.pushPose();
            individualModuleTransform.accept(module, poseStack);

            ClientCableNetworkHandler.renderLocalBox(
                    drivebysable$boundsOf(module).inflate(DRIVEBYSABLE$OUTLINE_INFLATE),
                    poseStack,
                    bufferSource.getBuffer(RenderType.debugQuads()),
                    color,
                    DRIVEBYSABLE$OUTLINE_THICKNESS
            );

            poseStack.popPose();
        });

        poseStack.popPose();
    }

    // * Modules that report no shape still need something visible to select
    private static AABB drivebysable$boundsOf(final Module module) {
        final VoxelShape shape = module.getVoxelShape();
        return shape == null || shape.isEmpty() ? DRIVEBYSABLE$FALLBACK_SHAPE : shape.bounds();
    }
}
