package edn.lakeopossmc.drivebysable.compat.dashpanels;

import com.mojang.blaze3d.vertex.PoseStack;
import edn.lakeopossmc.drivebysable.util.CableOutlineBox;
import moth.boxxed.panels.api.module.Module;
import moth.boxxed.panels.api.panel.AbstractPanelBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Matrix4f;

import java.util.List;

// --- WORLD SPACE GEOMETRY FOR A SINGLE MODULE --- //
// * Modules live in a flat panel local space
public final class DashPanelModuleGeometry {

    // * Nudge so the outline sits just outside the module surface
    private static final double OUTLINE_INFLATE = 0.002D;

    // * Modules that report no shape still need something selectable
    private static final AABB FALLBACK_SHAPE = new AABB(0, 0, 0, 1 / 16D, 1 / 16D, 1 / 16D);

    private DashPanelModuleGeometry() {
    }

    public static List<CableOutlineBox> outlineFor(final Level level, final BlockPos pos, final String moduleName) {
        final AbstractPanelBlockEntity panel = DashPanelCableBridge.getPanel(level, pos);
        if (panel == null || moduleName == null || moduleName.isEmpty()) {
            return List.of();
        }

        final Module module = panel.getModules().normalGet(moduleName);
        if (module == null) {
            return List.of();
        }

        return List.of(boxFor(panel, module, pos).inflated(OUTLINE_INFLATE));
    }

    // * Centre of the module in world space, used as a cable line endpoint
    public static Vec3 centerFor(final Level level, final BlockPos pos, final String moduleName) {
        final List<CableOutlineBox> boxes = outlineFor(level, pos, moduleName);
        return boxes.isEmpty() ? Vec3.atCenterOf(pos) : boxes.getFirst().center();
    }

    private static CableOutlineBox boxFor(
            final AbstractPanelBlockEntity panel,
            final Module module,
            final BlockPos pos
    ) {
        // * Same chain Dashpanels builds before inverting it to clip a module
        final PoseStack stack = new PoseStack();
        panel.transformPanelClipping(stack);
        final Matrix4f localToBlock = new Matrix4f(stack.last().pose());

        final AABB local = localBounds(module)
                .move(module.getPos().x / 16D, 0, module.getPos().y / 16D);

        return CableOutlineBox.of(local, localToBlock, Vec3.atLowerCornerOf(pos));
    }

    private static AABB localBounds(final Module module) {
        final VoxelShape shape = module.getVoxelShape();
        return shape == null || shape.isEmpty() ? FALLBACK_SHAPE : shape.bounds();
    }
}
