package edn.lakeopossmc.drivebysable.client;

import com.simibubi.create.AllSpecialTextures;
import dev.ryanhcode.sable.Sable;
import edn.lakeopossmc.drivebysable.DriveBySableMod;
import dev.ryanhcode.sable.sublevel.SubLevel;
import edn.lakeopossmc.drivebysable.CableBlocks;
import edn.lakeopossmc.drivebysable.cable.BackupDriveBounds;
import edn.lakeopossmc.drivebysable.cable.BackupDriveCapture;
import edn.lakeopossmc.drivebysable.cable.CableNetworkManager;
import edn.lakeopossmc.drivebysable.cable.graph.CableNetworkNode.CableNetworkSink;
import edn.lakeopossmc.drivebysable.network.CableNetworkRequestSyncPacket;
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
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.*;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;

// --- BOUNDING BOX PREVIEW FOR A BACKUP DRIVE --- //
// * Shown after the screen closes
@EventBusSubscriber(modid = DriveBySableMod.MOD_ID, value = Dist.CLIENT)
public final class BackupDrivePreview {

    private static final String OUTLINE_SLOT = "drivebysable:backupDrivePreview";
    private static final String SOURCE_SLOT = "drivebysable:backupDrivePreviewSource";
    private static final int OUTLINE_COLOR = 0x6886C5;
    private static final float OUTLINE_LINE_WIDTH = 0.0625F;

    private static final int SOURCE_VALID_COLOR = 0x55FF55;
    private static final int SOURCE_PARTIAL_COLOR = 0xFFC24A;
    private static final int SOURCE_INVALID_COLOR = 0xFF4444;

    private static final AABB UNIT_CUBE = new AABB(0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D);

    // * Module level highlights
    private static final Map<BlockPos, Map<String, Integer>> moduleOutlines = new LinkedHashMap<>();

    // * Whole block sources drawn this tick
    private static final Set<BlockPos> blockOutlines = new LinkedHashSet<>();

    // * What each preview judged this tick
    private record CachedTally(AABB box, long tick, int[] tally) {
    }

    private static final Map<BlockPos, CachedTally> tallies = new LinkedHashMap<>();

    private static final int REFRESH_TICKS = 5;

    public static final int TALLY_VALID = 0;
    public static final int TALLY_PARTIAL = 1;
    public static final int TALLY_SOURCE_UNREACHABLE = 2;
    public static final int TALLY_OUTPUTS_OUTSIDE = 3;
    private static final int TALLY_SIZE = 4;

    // * Which sublevels the region touches this tick
    private static Set<UUID> cachedIntersecting;
    private static AABB cachedWorldBox;

    // * One preview per drive keyed by position
    private record Preview(AABB box, ResourceKey<Level> dimension) {
    }

    private static final Map<BlockPos, Preview> previews = new LinkedHashMap<>();

    private static final int RESYNC_INTERVAL = 20;
    private static int resyncCountdown;

    private BackupDrivePreview() {
    }

    public static void show(final Level level, final BlockPos drivePos, final AABB box) {
        previews.put(drivePos.immutable(), new Preview(box, level.dimension()));
        resyncCountdown = RESYNC_INTERVAL;
    }

    // * One drive's preview
    public static void clear(final BlockPos drivePos) {
        previews.remove(drivePos);
        moduleOutlines.clear();
        blockOutlines.clear();
    }

    // * Every preview
    public static void clear() {
        previews.clear();
        moduleOutlines.clear();
        blockOutlines.clear();
    }

    public static boolean isActive(final BlockPos drivePos) {
        return previews.containsKey(drivePos);
    }

    public static boolean isActive() {
        return !previews.isEmpty();
    }

    public static Map<String, Integer> moduleOutlinesFor(final BlockPos pos) {
        return moduleOutlines.getOrDefault(pos, Map.of());
    }

    public static boolean isOutlining(final BlockPos pos) {
        return blockOutlines.contains(pos);
    }

    public static int[] tallyFor(final Level level, final BlockPos drivePos, final AABB box) {
        final long now = level.getGameTime();
        final CachedTally cached = tallies.get(drivePos);

        // * Recomputed when the region changes
        if (cached != null && cached.box().equals(box) && now - cached.tick() < REFRESH_TICKS) {
            return cached.tally();
        }

        final int[] tally = computeTally(level, drivePos, box);
        tallies.put(drivePos.immutable(), new CachedTally(box, now, tally));
        return tally;
    }

    private static int[] computeTally(final Level level, final BlockPos drivePos, final AABB box) {
        final int[] tally = new int[TALLY_SIZE];
        final CableNetworkManager manager = CableNetworkManager.get(level);
        final SubLevel driveSubLevel = BackupDriveCapture.subLevelOf(level, drivePos);

        for (final Map.Entry<Long, Map<String, Set<CableNetworkSink>>> entry : manager.getNetwork().entrySet()) {
            final BlockPos source = BlockPos.of(entry.getKey());
            if (level.getBlockState(source).isAir()) {
                continue;
            }

            if (!BackupDriveBounds.contains(box, source)
                    && !overlapsBoxInWorldSpace(level, box, driveSubLevel, source)) {
                continue;
            }

            for (final Map.Entry<String, Map<String, Set<CableNetworkSink>>> moduleEntry
                    : BackupDriveCapture.groupByModule(level, source, entry.getValue()).entrySet()) {

                final BackupDriveCapture.Status status =
                        BackupDriveCapture.classify(level, box, driveSubLevel, source, moduleEntry.getValue());

                tally[switch (status) {
                    case VALID -> TALLY_VALID;
                    case PARTIAL -> TALLY_PARTIAL;
                    case INVALID -> BackupDriveCapture.isSourceCapturable(level, box, driveSubLevel, source)
                            ? TALLY_OUTPUTS_OUTSIDE
                            : TALLY_SOURCE_UNREACHABLE;
                }]++;
            }
        }
        return tally;
    }

    public static boolean isOutliningModule(final BlockPos pos, final String module) {
        return moduleOutlines.getOrDefault(pos, Map.of()).containsKey(module);
    }

    public static AABB boundsFor(final BlockPos drive, final int[] offset, final int[] size, final int rotationSteps) {
        return BackupDriveBounds.of(drive, offset, size, rotationSteps);
    }


    @SubscribeEvent
    public static void onClientTick(final ClientTickEvent.Post event) {
        if (previews.isEmpty()) {
            return;
        }

        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            clear();
            return;
        }

        if (--resyncCountdown <= 0) {
            resyncCountdown = RESYNC_INTERVAL;
            PacketDistributor.sendToServer(CableNetworkRequestSyncPacket.INSTANCE);
        }

        // * Rebuilt from scratch each tick
        moduleOutlines.clear();
        blockOutlines.clear();
        previews.entrySet().removeIf(entry -> {
            final BlockPos drive = entry.getKey();
            final Preview preview = entry.getValue();

            if (!minecraft.level.dimension().equals(preview.dimension())
                    || !minecraft.level.getBlockState(drive).is(CableBlocks.BACKUP_DRIVE.get())) {
                return true;
            }

            Outliner.getInstance()
                    .showAABB(OUTLINE_SLOT + drive.asLong(), preview.box())
                    .colored(OUTLINE_COLOR)
                    .lineWidth(OUTLINE_LINE_WIDTH)
                    .withFaceTexture(AllSpecialTextures.CHECKERED);

            showSourceOutlines(minecraft.level, drive, preview.box());
            return false;
        });
    }

    //#region // --- SOURCES INSIDE THE BOX --- //
    private static void showSourceOutlines(final Level level, final BlockPos previewDrive, final AABB box) {
        final CableNetworkManager manager = CableNetworkManager.get(level);
        if (manager == null) {
            return;
        }

        cachedIntersecting = null;
        cachedWorldBox = null;
        final SubLevel driveSubLevel = BackupDriveCapture.subLevelOf(
                level, previewDrive
        );

        for (final Map.Entry<Long, Map<String, Set<CableNetworkSink>>> entry : manager.getNetwork().entrySet()) {
            final BlockPos source = BlockPos.of(entry.getKey());

            if (level.getBlockState(source).isAir()) {
                continue;
            }

            if (!BackupDriveBounds.contains(box, source)
                    && !overlapsBoxInWorldSpace(level, box, driveSubLevel, source)) {
                continue;
            }

            // * Split by module first
            final Map<String, Map<String, Set<CableNetworkSink>>> byModule =
                    BackupDriveCapture.groupByModule(level, source, entry.getValue());

            for (final Map.Entry<String, Map<String, Set<CableNetworkSink>>> moduleEntry : byModule.entrySet()) {
                final BackupDriveCapture.Status status = BackupDriveCapture.classify(
                        level, box, driveSubLevel, source, moduleEntry.getValue()
                );

                final int color = switch (status) {
                    case VALID -> SOURCE_VALID_COLOR;
                    case PARTIAL -> SOURCE_PARTIAL_COLOR;
                    case INVALID -> SOURCE_INVALID_COLOR;
                };

                final String module = moduleEntry.getKey();
                if (module.isEmpty()) {
                    blockOutlines.add(source.immutable());
                    Outliner.getInstance()
                            .showAABB(SOURCE_SLOT + previewDrive.asLong() + ":" + source.asLong(), blockBounds(level, source))
                            .colored(color)
                            .lineWidth(OUTLINE_LINE_WIDTH);
                } else {
                    moduleOutlines
                            .computeIfAbsent(source.immutable(), ignored -> new LinkedHashMap<>())
                            .put(module, color);
                }
            }
        }
    }

    private static boolean overlapsBoxInWorldSpace(
            final Level level,
            final AABB box,
            final SubLevel driveSubLevel,
            final BlockPos source
    ) {
        final SubLevel sourceSubLevel = BackupDriveCapture.subLevelOf(level, source);
        if (BackupDriveCapture.isSameLevel(driveSubLevel, sourceSubLevel)) {
            return false;
        }

        // * Compared in drive's own space rather than world space
        final AABB sourceWorld = worldSpaceBounds(level, source);
        final AABB sourceInDriveSpace = driveSubLevel == null
                ? sourceWorld
                : transformToLocal(sourceWorld, driveSubLevel);

        if (sourceSubLevel != null) {
            final AABB worldBox = worldBox(box, driveSubLevel);
            if (!intersectingSubLevels(level, worldBox).contains(sourceSubLevel.getUniqueId())) {
                return false;
            }
        }

        return box.intersects(sourceInDriveSpace);
    }

    private static AABB worldBox(final AABB box, final SubLevel driveSubLevel) {
        if (cachedWorldBox != null) {
            return cachedWorldBox;
        }

        cachedWorldBox = driveSubLevel == null ? box : transformToWorld(box, driveSubLevel);
        return cachedWorldBox;
    }

    // * Recomputed once per tick and cached
    private static Set<UUID> intersectingSubLevels(final Level level, final AABB box) {
        if (cachedIntersecting != null) {
            return cachedIntersecting;
        }

        final Set<UUID> ids = new HashSet<>();
        for (final SubLevel subLevel : Sable.HELPER.getAllIntersecting(level, new BoundingBox3d(
                box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ))) {
            ids.add(subLevel.getUniqueId());
        }

        cachedIntersecting = ids;
        return ids;
    }

    private static AABB transformToLocal(final AABB world, final SubLevel subLevel) {
        final Pose3dc pose = subLevel.logicalPose();

        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double minZ = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;
        double maxZ = -Double.MAX_VALUE;

        for (int index = 0; index < 8; index++) {
            final Vec3 corner = pose.transformPositionInverse(new Vec3(
                    (index & 1) == 0 ? world.minX : world.maxX,
                    (index & 2) == 0 ? world.minY : world.maxY,
                    (index & 4) == 0 ? world.minZ : world.maxZ
            ));

            minX = Math.min(minX, corner.x);
            minY = Math.min(minY, corner.y);
            minZ = Math.min(minZ, corner.z);
            maxX = Math.max(maxX, corner.x);
            maxY = Math.max(maxY, corner.y);
            maxZ = Math.max(maxZ, corner.z);
        }

        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private static AABB transformToWorld(final AABB local, final SubLevel subLevel) {
        final Pose3dc pose = subLevel.logicalPose();

        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double minZ = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;
        double maxZ = -Double.MAX_VALUE;

        for (int index = 0; index < 8; index++) {
            final Vec3 corner = pose.transformPosition(new Vec3(
                    (index & 1) == 0 ? local.minX : local.maxX,
                    (index & 2) == 0 ? local.minY : local.maxY,
                    (index & 4) == 0 ? local.minZ : local.maxZ
            ));

            minX = Math.min(minX, corner.x);
            minY = Math.min(minY, corner.y);
            minZ = Math.min(minZ, corner.z);
            maxX = Math.max(maxX, corner.x);
            maxY = Math.max(maxY, corner.y);
            maxZ = Math.max(maxZ, corner.z);
        }

        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    // * Drawn where the player can see it
    private static AABB worldSpaceBounds(final Level level, final BlockPos pos) {
        final SubLevel subLevel = BackupDriveCapture.subLevelOf(level, pos);
        final AABB local = blockBounds(level, pos);
        return subLevel == null ? local : transformToWorld(local, subLevel);
    }

    private static AABB blockBounds(final Level level, final BlockPos pos) {
        final VoxelShape shape = level.getBlockState(pos).getShape(level, pos);
        return (shape.isEmpty() ? UNIT_CUBE : shape.bounds()).move(pos);
    }
    //#endregion

    @SubscribeEvent
    public static void onLevelUnload(final LevelEvent.Unload event) {
        clear();
    }
}