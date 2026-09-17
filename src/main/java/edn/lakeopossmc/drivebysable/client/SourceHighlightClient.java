package edn.lakeopossmc.drivebysable.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPoint;
import dev.ryanhcode.sable.sublevel.SubLevel;
import edn.lakeopossmc.drivebysable.DriveBySableMod;
import edn.lakeopossmc.drivebysable.cable.BackupDriveCapture;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

// --- CLIENT SIDE OF /dbs highlight --- //
@EventBusSubscriber(modid = DriveBySableMod.MOD_ID, value = Dist.CLIENT)
public final class SourceHighlightClient {

    private static final String SOURCE_SLOT = "drivebysable:sourceHighlight:source:";
    private static final String OUTPUT_SLOT = "drivebysable:sourceHighlight:output:";

    private static final int SOURCE_COLOR = ArmInteractionPoint.Mode.TAKE.getColor();
    private static final int OUTPUT_COLOR = ArmInteractionPoint.Mode.DEPOSIT.getColor();
    private static final int LINK_COLOR = 0xFF0000;

    private static final float DIM_FACTOR = 0.8F;
    private static final int SOURCE_COLOR_DIM = darken(SOURCE_COLOR);
    private static final int OUTPUT_COLOR_DIM = darken(OUTPUT_COLOR);
    private static final int LINK_COLOR_DIM = darken(LINK_COLOR);

    private static final int BLINK_PERIOD = 16;
    private static final int BLINK_HALF = 8;

    private static final float OUTLINE_WIDTH = 1 / 16.0F;
    private static final double LINK_THICKNESS = 1 / 64.0D;

    private static final AABB UNIT_CUBE = new AABB(0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D);

    private static final Set<BlockPos> sources = new LinkedHashSet<>();
    private static final Set<BlockPos> outputs = new LinkedHashSet<>();
    private static final List<BlockPos[]> links = new ArrayList<>();

    @Nullable
    private static ResourceKey<Level> dimension;
    private static int ticksRemaining;

    private SourceHighlightClient() {
    }

    public static void show(
            final List<BlockPos> newSources,
            final List<BlockPos> newOutputs,
            final List<Integer> owners,
            final int ticks
    ) {
        clear();

        final Minecraft minecraft = Minecraft.getInstance();
        if (ticks <= 0 || minecraft.level == null) {
            return;
        }

        for (final BlockPos source : newSources) {
            sources.add(source.immutable());
        }

        final int count = Math.min(newOutputs.size(), owners.size());
        for (int index = 0; index < count; index++) {
            final BlockPos output = newOutputs.get(index).immutable();
            final int owner = owners.get(index);

            // * A block that is also a Source keeps the Source colour
            if (!sources.contains(output)) {
                outputs.add(output);
            }
            if (owner >= 0 && owner < newSources.size()) {
                links.add(new BlockPos[]{newSources.get(owner), output});
            }
        }

        dimension = minecraft.level.dimension();
        ticksRemaining = ticks;
    }

    public static void clear() {
        sources.clear();
        outputs.clear();
        links.clear();
        dimension = null;
        ticksRemaining = 0;
    }

    @SubscribeEvent
    public static void onClientTick(final ClientTickEvent.Post event) {
        if (ticksRemaining <= 0) {
            return;
        }

        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || !minecraft.level.dimension().equals(dimension)) {
            clear();
            return;
        }

        final boolean bright = isBrightPhase();
        final int sourceColor = bright ? SOURCE_COLOR : SOURCE_COLOR_DIM;
        final int outputColor = bright ? OUTPUT_COLOR : OUTPUT_COLOR_DIM;

        for (final BlockPos pos : sources) {
            outline(minecraft.level, SOURCE_SLOT, pos, sourceColor);
        }
        for (final BlockPos pos : outputs) {
            outline(minecraft.level, OUTPUT_SLOT, pos, outputColor);
        }

        if (--ticksRemaining <= 0) {
            clear();
        }
    }

    private static void outline(final Level level, final String slot, final BlockPos pos, final int color) {
        Outliner.getInstance()
                .showAABB(slot + pos.asLong(), blockBounds(level, pos))
                .colored(color)
                .lineWidth(OUTLINE_WIDTH)
                .disableLineNormals();
    }

    //#region // --- CONNECTION LINES --- //
    @SubscribeEvent
    public static void onRenderLevel(final RenderLevelStageEvent event) {
        if (ticksRemaining <= 0 || links.isEmpty() || event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }

        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        final Vec3 camera = event.getCamera().getPosition();
        final PoseStack poseStack = event.getPoseStack();
        final MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        final VertexConsumer buffer = buffers.getBuffer(RenderType.debugQuads());
        final int color = isBrightPhase() ? LINK_COLOR : LINK_COLOR_DIM;

        for (final BlockPos[] link : links) {
            drawLink(
                    poseStack,
                    buffer,
                    camera,
                    worldCentreOf(minecraft.level, link[0]),
                    worldCentreOf(minecraft.level, link[1]),
                    color
            );
        }

        buffers.endBatch(RenderType.debugQuads());
    }

    private static void drawLink(
            final PoseStack poseStack,
            final VertexConsumer buffer,
            final Vec3 camera,
            final Vec3 from,
            final Vec3 to,
            final int color
    ) {
        final Vec3 delta = to.subtract(from);
        final double length = delta.length();
        if (length < 1.0E-4D) {
            return;
        }

        final double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        final float yaw = (float) Mth.atan2(delta.x, delta.z);
        final float pitch = (float) -Mth.atan2(delta.y, horizontal);

        poseStack.pushPose();
        poseStack.translate(from.x - camera.x, from.y - camera.y, from.z - camera.z);
        poseStack.mulPose(Axis.YP.rotation(yaw));
        poseStack.mulPose(Axis.XP.rotation(pitch));

        ClientCableNetworkHandler.renderLocalBox(
                new AABB(0.0D, 0.0D, 0.0D, 0.0D, 0.0D, length),
                poseStack,
                buffer,
                color,
                LINK_THICKNESS
        );

        poseStack.popPose();
    }

    private static Vec3 worldCentreOf(final Level level, final BlockPos pos) {
        final Vec3 centre = blockBounds(level, pos).getCenter();
        final SubLevel subLevel = BackupDriveCapture.subLevelOf(level, pos);
        return subLevel == null ? centre : subLevel.logicalPose().transformPosition(centre);
    }
    //#endregion

    @SubscribeEvent
    public static void onLevelUnload(final LevelEvent.Unload event) {
        clear();
    }

    private static boolean isBrightPhase() {
        return AnimationTickHolder.getTicks() % BLINK_PERIOD < BLINK_HALF;
    }

    private static int darken(final int color) {
        final int red = (int) (((color >> 16) & 0xFF) * DIM_FACTOR);
        final int green = (int) (((color >> 8) & 0xFF) * DIM_FACTOR);
        final int blue = (int) ((color & 0xFF) * DIM_FACTOR);
        return (red << 16) | (green << 8) | blue;
    }

    private static AABB blockBounds(final Level level, final BlockPos pos) {
        final VoxelShape shape = level.getBlockState(pos).getShape(level, pos);
        return (shape.isEmpty() ? UNIT_CUBE : shape.bounds()).move(pos);
    }
}