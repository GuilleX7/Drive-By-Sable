package edn.lakeopossmc.drivebysable.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import edn.lakeopossmc.drivebysable.blocks.IntegratedSensorBusBlockEntity;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import edn.lakeopossmc.drivebysable.blocks.IntegratedSensorBusBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Quaternionf;

// --- DRAWS THE PARTS OF THE SENSOR BUS THAT MOVE --- //
public class IntegratedSensorBusRenderer extends SafeBlockEntityRenderer<IntegratedSensorBusBlockEntity> {

    private static final float PIXEL = 1.0F / 16.0F;

    private static final float SIDE_HEIGHT = 11.5F * PIXEL;

    private static final float NEAR_DEPTH = 4.5F * PIXEL;
    private static final float FAR_DEPTH = 11.5F * PIXEL;

    private static float facingDegrees(final BlockState state) {
        return 180.0F - state.getValue(IntegratedSensorBusBlock.FACING).toYRot();
    }

    private static final float GIMBAL_PIVOT_X = 8.0F * PIXEL;
    private static final float GIMBAL_PIVOT_Y = 16.0F * PIXEL;
    private static final float GIMBAL_PIVOT_Z = 8.0F * PIXEL;

    private static final float BLOCK_CENTRE = 0.5F;
    private static final float PIVOT_OFFSET_X = GIMBAL_PIVOT_X - BLOCK_CENTRE;
    private static final float PIVOT_OFFSET_Y = GIMBAL_PIVOT_Y - BLOCK_CENTRE;
    private static final float PIVOT_OFFSET_Z = GIMBAL_PIVOT_Z - BLOCK_CENTRE;

    public IntegratedSensorBusRenderer(final BlockEntityRendererProvider.Context context) {
        super();
    }

    @Override
    protected void renderSafe(
            final IntegratedSensorBusBlockEntity sensor,
            final float partialTicks,
            final PoseStack poseStack,
            final MultiBufferSource buffers,
            final int light,
            final int overlay
    ) {
        final VertexConsumer consumer = buffers.getBuffer(RenderType.cutout());

        final float angle = sensor.getFanAngle(partialTicks);
        sideMounted(SensorBusPartialModels.FAN_WEST, sensor, light, poseStack, consumer,
                SIDE_HEIGHT, FAR_DEPTH, angle, 0.0F);
        sideMounted(SensorBusPartialModels.FAN_EAST, sensor, light, poseStack, consumer,
                SIDE_HEIGHT, NEAR_DEPTH, angle, 0.0F);

        final float lift = sensor.getAltitudeHandOffset(partialTicks);
        sideMounted(SensorBusPartialModels.HAND_WEST, sensor, light, poseStack, consumer,
                SIDE_HEIGHT, NEAR_DEPTH, 0.0F, lift);
        sideMounted(SensorBusPartialModels.HAND_EAST, sensor, light, poseStack, consumer,
                SIDE_HEIGHT, FAR_DEPTH, 0.0F, lift);

        renderGimbal(sensor, partialTicks, light, poseStack, consumer);
    }

    //#region // --- GIMBAL --- //
    private void renderGimbal(
            final IntegratedSensorBusBlockEntity sensor,
            final float partialTicks,
            final int light,
            final PoseStack poseStack,
            final VertexConsumer consumer
    ) {
        final Quaternionf orientation = sensor.getBaseQuaternion();

        sensor.applyPrimaryQuaternion(orientation, partialTicks);
        apply(SensorBusPartialModels.GIMBAL, sensor, light, poseStack, consumer, orientation);

        sensor.applySecondaryQuaternion(orientation, partialTicks);
        apply(SensorBusPartialModels.COMPASS, sensor, light, poseStack, consumer, orientation);

        sensor.applyCompassQuaternion(orientation, partialTicks);
        apply(SensorBusPartialModels.NEEDLE, sensor, light, poseStack, consumer, orientation);
    }

    private void apply(
            final PartialModel model,
            final IntegratedSensorBusBlockEntity sensor,
            final int light,
            final PoseStack poseStack,
            final VertexConsumer consumer,
            final Quaternionf orientation
    ) {
        CachedBuffers.partial(model, sensor.getBlockState())
                .translate(PIVOT_OFFSET_X, PIVOT_OFFSET_Y, PIVOT_OFFSET_Z)
                .rotateCentered(orientation)
                .translate(BLOCK_CENTRE, BLOCK_CENTRE, BLOCK_CENTRE)
                .light(light)
                .renderInto(poseStack, consumer);
    }
    //#endregion

    private void sideMounted(
            final PartialModel model,
            final IntegratedSensorBusBlockEntity sensor,
            final int light,
            final PoseStack poseStack,
            final VertexConsumer consumer,
            final float pivotY,
            final float pivotZ,
            final float radians,
            final float lift
    ) {
        final BlockState state = sensor.getBlockState();

        // * Reverse call order again
        CachedBuffers.partial(model, state)
                .rotateCenteredDegrees(facingDegrees(state), Direction.UP)
                .translate(0.0F, pivotY + lift, pivotZ)
                .rotateX(radians)
                .translate(0.0F, -pivotY, -pivotZ)
                .light(light)
                .renderInto(poseStack, consumer);
    }
}