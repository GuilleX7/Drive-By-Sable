package edn.lakeopossmc.drivebysable.client;

import edn.lakeopossmc.drivebysable.DriveBySableMod;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

import javax.annotation.Nullable;
import java.util.LinkedHashSet;
import java.util.Set;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import com.mojang.math.Axis;
import dev.ryanhcode.sable.sublevel.SubLevel;
import edn.lakeopossmc.drivebysable.cable.BackupDriveCapture;
import net.minecraft.util.Mth;
import java.util.LinkedHashMap;
import java.util.Map;

// --- WHAT A LOAD JUST TOUCHED --- //
// * Shown for a few seconds after a load
@EventBusSubscriber(modid = DriveBySableMod.MOD_ID, value = Dist.CLIENT)
public final class BackupDriveLoadHighlight {

    private static final String SLOT = "drivebysable:loadHighlight";

    private static final int BLINK_A = 0x708DAD;
    private static final int BLINK_B = 0x90ADCD;
    private static final int BLINK_PERIOD = 16;
    private static final int BLINK_HALF = 8;
    private static final float LINE_WIDTH = 1 / 32.0F;

    private static final double LINK_THICKNESS = LINE_WIDTH;

    private static final int LINK_FADE_TICKS = 15;

    private static final int DISPLAY_TICKS = 120;

    private static final AABB UNIT_CUBE = new AABB(0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D);

    private static final Set<BlockPos> highlighted = new LinkedHashSet<>();

    private static final Set<BlockPos> linkedSources = new LinkedHashSet<>();

    private static final Map<BlockPos, Set<String>> moduleOutlines = new LinkedHashMap<>();

    @Nullable
    private static BlockPos linkedDrive;

    private static int fadeTicks;

    @Nullable
    private static ResourceKey<Level> dimension;
    private static int ticksRemaining;

    private BackupDriveLoadHighlight() {
    }

    public static void show(final Level level, final BlockPos drive, final Map<BlockPos, Set<String>> sources) {
        highlighted.clear();
        moduleOutlines.clear();
        linkedSources.clear();

        highlighted.add(drive.immutable());

        for (final Map.Entry<BlockPos, Set<String>> entry : sources.entrySet()) {
            final BlockPos source = entry.getKey().immutable();
            linkedSources.add(source);

            for (final String module : entry.getValue()) {
                if (module.isEmpty()) {
                    highlighted.add(source);
                } else {
                    moduleOutlines.computeIfAbsent(source, ignored -> new LinkedHashSet<>()).add(module);
                }
            }
        }

        linkedDrive = drive.immutable();

        dimension = level.dimension();
        ticksRemaining = DISPLAY_TICKS;
        fadeTicks = LINK_FADE_TICKS;
    }

    public static boolean isOutlining(final BlockPos pos) {
        return ticksRemaining > 0 && highlighted.contains(pos);
    }

    public static boolean isOutliningModule(final BlockPos pos, final String module) {
        return ticksRemaining > 0 && moduleOutlines.getOrDefault(pos, Set.of()).contains(module);
    }

    public static Map<String, Integer> moduleOutlinesFor(final BlockPos pos) {
        final Set<String> modules = moduleOutlines.get(pos);
        if (modules == null || modules.isEmpty()) {
            return Map.of();
        }

        final int color = blinkColor();
        final Map<String, Integer> outlines = new LinkedHashMap<>();
        for (final String module : modules) {
            outlines.put(module, color);
        }
        return outlines;
    }

    public static void clear() {
        highlighted.clear();
        moduleOutlines.clear();
        linkedSources.clear();
        linkedDrive = null;
        dimension = null;
        ticksRemaining = 0;
        fadeTicks = 0;
    }

    @SubscribeEvent
    public static void onClientTick(final ClientTickEvent.Post event) {
        if (ticksRemaining <= 0 || highlighted.isEmpty()) {
            return;
        }

        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || !minecraft.level.dimension().equals(dimension)) {
            clear();
            return;
        }

        ticksRemaining--;

        final int color = blinkColor();

        for (final BlockPos pos : highlighted) {
            Outliner.getInstance()
                    .showAABB(SLOT + pos.asLong(), blockBounds(minecraft.level, pos))
                    .colored(color)
                    .lineWidth(LINE_WIDTH)
                    .disableLineNormals();
        }

        if (ticksRemaining <= 0) {
            highlighted.clear();
            moduleOutlines.clear();
        }
    }

    @SubscribeEvent
    public static void onFadeTick(final ClientTickEvent.Post event) {
        if (ticksRemaining > 0 || fadeTicks <= 0) {
            return;
        }

        fadeTicks--;
        if (fadeTicks <= 0) {
            linkedSources.clear();
            linkedDrive = null;
        }
    }

    //#region // --- LINES BACK TO THE DRIVE --- //
    // * Drawn here rather than through the outliner
    @SubscribeEvent
    public static void onRenderLevel(final RenderLevelStageEvent event) {
        if (linkedDrive == null || linkedSources.isEmpty() || (ticksRemaining <= 0 && fadeTicks <= 0)) {
            return;
        }
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
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

        final int color = blinkColor();
        final Vec3 driveCentre = worldCentreOf(minecraft.level, linkedDrive);

        final double thickness = ticksRemaining > 0
                ? LINK_THICKNESS
                : LINK_THICKNESS * fadeTicks / LINK_FADE_TICKS;

        if (thickness <= 0.0D) {
            return;
        }

        for (final BlockPos source : linkedSources) {
            drawLink(poseStack, buffer, camera, worldCentreOf(minecraft.level, source), driveCentre, color, thickness);
        }

        buffers.endBatch(RenderType.debugQuads());
    }

    private static void drawLink(
            final PoseStack poseStack,
            final VertexConsumer buffer,
            final Vec3 camera,
            final Vec3 from,
            final Vec3 to,
            final int color,
            final double thickness
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
                thickness
        );

        poseStack.popPose();
    }

    // * The middle of whatever shape the block has
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

    private static int blinkColor() {
        return AnimationTickHolder.getTicks() % BLINK_PERIOD < BLINK_HALF ? BLINK_A : BLINK_B;
    }

    private static AABB blockBounds(final Level level, final BlockPos pos) {
        final VoxelShape shape = level.getBlockState(pos).getShape(level, pos);
        return (shape.isEmpty() ? UNIT_CUBE : shape.bounds()).move(pos);
    }
}