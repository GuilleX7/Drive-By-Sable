package edn.lakeopossmc.drivebysable.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPoint;
import dev.ryanhcode.sable.sublevel.SubLevel;
import edn.lakeopossmc.drivebysable.DriveBySableMod;
import edn.lakeopossmc.drivebysable.cable.BackupDriveCapture;
import edn.lakeopossmc.drivebysable.network.SourceHighlightPacket;
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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;

// --- CLIENT SIDE OF /dbs highlight --- //
@EventBusSubscriber(modid = DriveBySableMod.MOD_ID, value = Dist.CLIENT)
public final class SourceHighlightClient {

    private static final String TARGET_SLOT = "drivebysable:sourceHighlight:target:";
    private static final String CONNECTED_SLOT = "drivebysable:sourceHighlight:connected:";

    private static final int SOURCE_COLOR = ArmInteractionPoint.Mode.TAKE.getColor();
    private static final int OUTPUT_COLOR = ArmInteractionPoint.Mode.DEPOSIT.getColor();
    private static final int LINK_COLOR = 0xFF0000;

    private static final float DIM_FACTOR = 0.8F;
    private static final int SOURCE_COLOR_DIM = darken(SOURCE_COLOR);
    private static final int OUTPUT_COLOR_DIM = darken(OUTPUT_COLOR);
    private static final int LINK_COLOR_DIM = darken(LINK_COLOR);

    private static final int BLINK_PERIOD = 16;
    private static final int BLINK_HALF = 8;

    private static final float STRONG_WIDTH = 1 / 24.0F;
    private static final float WEAK_WIDTH = 1 / 64.0F;
    private static final double LINK_THICKNESS = 1 / 48.0D;

    private static final AABB UNIT_CUBE = new AABB(0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D);

    private static final Map<BlockPos, Boolean> blockTargets = new LinkedHashMap<>();
    private static final Map<BlockPos, Boolean> blockConnected = new LinkedHashMap<>();
    // * Module endpoints, drawn by the block's own renderer through moduleOutlinesFor
    private static final Map<BlockPos, Map<String, Boolean>> moduleTargets = new LinkedHashMap<>();
    private static final Map<BlockPos, Map<String, Boolean>> moduleConnected = new LinkedHashMap<>();
    // * Target then connected, one pair per connection
    private static final List<Endpoint[]> links = new ArrayList<>();

    private record Endpoint(BlockPos pos, String module) {
    }

    @Nullable
    private static ResourceKey<Level> dimension;
    private static int ticksRemaining;
    private static boolean infinite;

    private SourceHighlightClient() {
    }

    public static void show(final SourceHighlightPacket packet) {
        clear();

        final Minecraft minecraft = Minecraft.getInstance();
        if (packet.ticks() == 0 || minecraft.level == null) {
            return;
        }

        final List<Endpoint> targeted = new ArrayList<>(packet.targets().size());
        for (final SourceHighlightPacket.HighlightEndpoint endpoint : packet.targets()) {
            targeted.add(add(endpoint, true));
        }

        final int count = Math.min(packet.connected().size(), packet.owners().size());
        for (int i = 0; i < count; i++) {
            final Endpoint other = add(packet.connected().get(i), false);

            final int owner = packet.owners().get(i);
            if (owner >= 0 && owner < targeted.size()) {
                links.add(new Endpoint[]{targeted.get(owner), other});
            }
        }

        dimension = minecraft.level.dimension();
        ticksRemaining = packet.ticks();
        infinite = packet.ticks() == SourceHighlightPacket.INFINITE;
    }

    private static Endpoint add(final SourceHighlightPacket.HighlightEndpoint endpoint, final boolean target) {
        final BlockPos pos = endpoint.pos().immutable();

        if (!endpoint.module().isEmpty()) {
            final Map<BlockPos, Map<String, Boolean>> into = target ? moduleTargets : moduleConnected;
            into.computeIfAbsent(pos, ignored -> new LinkedHashMap<>()).put(endpoint.module(), endpoint.source());
            if (target) {
                final Map<String, Boolean> weaker = moduleConnected.get(pos);
                if (weaker != null) {
                    weaker.remove(endpoint.module());
                }
            }
            return new Endpoint(pos, endpoint.module());
        }

        if (target) {
            blockTargets.put(pos, endpoint.source());
            blockConnected.remove(pos);
        } else if (!blockTargets.containsKey(pos)) {
            blockConnected.put(pos, endpoint.source());
        }
        return new Endpoint(pos, "");
    }

    public static Map<String, Integer> moduleOutlinesFor(final BlockPos pos) {
        if (!isActive()) {
            return Map.of();
        }

        final Map<String, Boolean> targeted = moduleTargets.get(pos);
        final Map<String, Boolean> other = moduleConnected.get(pos);
        if (targeted == null && other == null) {
            return Map.of();
        }

        final boolean bright = isBrightPhase();
        final Map<String, Integer> outlines = new LinkedHashMap<>();
        if (other != null) {
            other.forEach((module, source) -> outlines.put(module, colorFor(source, bright)));
        }
        if (targeted != null) {
            targeted.forEach((module, source) -> outlines.put(module, colorFor(source, bright)));
        }
        return outlines;
    }

    private static int colorFor(final boolean source, final boolean bright) {
        if (source) {
            return bright ? SOURCE_COLOR : SOURCE_COLOR_DIM;
        }
        return bright ? OUTPUT_COLOR : OUTPUT_COLOR_DIM;
    }

    public static void clear() {
        blockTargets.clear();
        blockConnected.clear();
        moduleTargets.clear();
        moduleConnected.clear();
        links.clear();
        dimension = null;
        ticksRemaining = 0;
        infinite = false;
    }

    @SubscribeEvent
    public static void onClientTick(final ClientTickEvent.Post event) {
        if (!isActive()) {
            return;
        }

        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || !minecraft.level.dimension().equals(dimension)) {
            clear();
            return;
        }

        final boolean bright = isBrightPhase();

        // * The side asked about is drawn heavier than whatever it is connected to
        blockTargets.forEach((pos, source) ->
                outline(minecraft.level, TARGET_SLOT, pos, colorFor(source, bright), STRONG_WIDTH));
        blockConnected.forEach((pos, source) ->
                outline(minecraft.level, CONNECTED_SLOT, pos, colorFor(source, bright), WEAK_WIDTH));

        if (!infinite && --ticksRemaining <= 0) {
            clear();
        }
    }

    private static void outline(
            final Level level,
            final String slot,
            final BlockPos pos,
            final int color,
            final float width
    ) {
        Outliner.getInstance()
                .showAABB(slot + pos.asLong(), blockBounds(level, pos))
                .colored(color)
                .lineWidth(width)
                .disableLineNormals();
    }

    //#region // --- CONNECTION LINES --- //
    @SubscribeEvent
    public static void onRenderLevel(final RenderLevelStageEvent event) {
        if (!isActive() || links.isEmpty() || event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
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

        for (final Endpoint[] link : links) {
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

    private static Vec3 worldCentreOf(final Level level, final Endpoint endpoint) {
        final Vec3 centre = endpoint.module().isEmpty()
                ? blockBounds(level, endpoint.pos()).getCenter()
                : ClientCableNetworkHandler.moduleAnchor(level, endpoint.pos(), endpoint.module());
        final SubLevel subLevel = BackupDriveCapture.subLevelOf(level, endpoint.pos());
        return subLevel == null ? centre : subLevel.logicalPose().transformPosition(centre);
    }
    //#endregion

    @SubscribeEvent
    public static void onLevelUnload(final LevelEvent.Unload event) {
        clear();
    }

    private static boolean isActive() {
        return infinite || ticksRemaining > 0;
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