package edn.lakeopossmc.drivebysable.client;

import edn.lakeopossmc.drivebysable.DriveBySableMod;
import edn.lakeopossmc.drivebysable.network.BackupDriveRegionPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Optional;

// --- IN WORLD RESIZING OF A DRIVE PREVIEW --- //
@EventBusSubscriber(modid = DriveBySableMod.MOD_ID, value = Dist.CLIENT)
public final class BackupDriveResizeHandler {

    private static final double PICK_DISTANCE = 128.0D;

    private static final double FACE_EPSILON = 1.0E-4D;

    private static final int MIN_SIZE = 1;
    private static final int MAX_SIZE = 64;

    private BackupDriveResizeHandler() {
    }

    @SubscribeEvent
    public static void onMouseScroll(final InputEvent.MouseScrollingEvent event) {
        if (!Screen.hasControlDown()) {
            return;
        }

        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.screen != null) {
            return;
        }

        final int delta = -(int) Math.signum(event.getScrollDeltaY());
        if (delta == 0) {
            return;
        }

        // * Swallowed so the hotbar does not scroll along with the resize
        if (resizeLookedAtFace(minecraft.player, minecraft.level, delta)) {
            event.setCanceled(true);
        }
    }

    // * Refreshes the brightened face every tick
    @SubscribeEvent
    public static void onClientTick(final ClientTickEvent.Post event) {
        final Minecraft minecraft = Minecraft.getInstance();
        BackupDrivePreview.clearHighlightedFaces();

        if (!Screen.hasControlDown()
                || minecraft.player == null
                || minecraft.level == null
                || minecraft.screen != null) {
            return;
        }

        final Aim aim = aimedFace(minecraft.player, minecraft.level);
        if (aim != null) {
            BackupDrivePreview.setHighlightedFace(aim.drivePos(), aim.face());
        }
    }

    private record Aim(BlockPos drivePos, Direction face, boolean normalTowardCamera) {
    }

    // * Picks the nearest preview the player is looking into and nudges the face they hit
    private static boolean resizeLookedAtFace(final Player player, final Level level, final int delta) {
        final Aim aim = aimedFace(player, level);
        if (aim == null) {
            return false;
        }

        return moveFace(aim.drivePos(), aim.face(), aim.normalTowardCamera() ? delta : -delta);
    }

    // * Nearest preview face under the crosshair, or null
    private static Aim aimedFace(final Player player, final Level level) {
        final Vec3 eye = player.getEyePosition();
        final Vec3 end = eye.add(player.getViewVector(1.0F).scale(PICK_DISTANCE));

        BlockPos bestDrive = null;
        Direction bestFace = null;
        double bestDistance = Double.MAX_VALUE;
        boolean bestInside = false;

        for (final BlockPos drivePos : BackupDrivePreview.activeDrives()) {
            if (!BackupDrivePreview.isActive(drivePos, level)) {
                continue;
            }

            final AABB box = BackupDrivePreview.boxFor(drivePos);
            if (box == null) {
                continue;
            }

            final Vec3 localEye = BackupDrivePreview.toDriveSpace(level, drivePos, eye);
            final Vec3 localEnd = BackupDrivePreview.toDriveSpace(level, drivePos, end);

            final boolean inside = box.contains(localEye);
            final Optional<Vec3> hit = inside
                    ? box.clip(localEnd, localEye)
                    : box.clip(localEye, localEnd);
            if (hit.isEmpty()) {
                continue;
            }

            final double distance = localEye.distanceToSqr(hit.get());
            final Direction face = faceAt(box, hit.get());
            if (face != null && distance < bestDistance) {
                bestDistance = distance;
                bestDrive = drivePos;
                bestFace = face;
                bestInside = inside;
            }
        }

        return bestDrive == null ? null : new Aim(bestDrive, bestFace, !bestInside);
    }

    // * Which bound the hit landed on
    private static Direction faceAt(final AABB box, final Vec3 hit) {
        if (Math.abs(hit.x - box.minX) < FACE_EPSILON) return Direction.WEST;
        if (Math.abs(hit.x - box.maxX) < FACE_EPSILON) return Direction.EAST;
        if (Math.abs(hit.y - box.minY) < FACE_EPSILON) return Direction.DOWN;
        if (Math.abs(hit.y - box.maxY) < FACE_EPSILON) return Direction.UP;
        if (Math.abs(hit.z - box.minZ) < FACE_EPSILON) return Direction.NORTH;
        if (Math.abs(hit.z - box.maxZ) < FACE_EPSILON) return Direction.SOUTH;
        return null;
    }

    private static boolean moveFace(final BlockPos drivePos, final Direction worldFace, final int delta) {
        final int[] offset = BackupDrivePreview.offsetFor(drivePos);
        final int[] size = BackupDrivePreview.sizeFor(drivePos);
        if (offset == null || size == null) {
            return false;
        }

        final int rotationSteps = BackupDrivePreview.rotationFor(drivePos);
        final Direction localFace = toLocal(worldFace, rotationSteps);
        final int axis = localFace.getAxis().ordinal();

        final int grown = size[axis] + delta;
        if (grown < MIN_SIZE || grown > MAX_SIZE) {
            return true;
        }

        size[axis] = grown;

        if (localFace.getAxisDirection() == Direction.AxisDirection.NEGATIVE) {
            offset[axis] -= delta;
        }

        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null) {
            BackupDrivePreview.show(minecraft.level, drivePos, offset, size, rotationSteps);
        }

        PacketDistributor.sendToServer(
                BackupDriveRegionPacket.of(drivePos, offset, size, rotationSteps)
        );
        return true;
    }

    private static Direction toLocal(final Direction worldFace, final int rotationSteps) {
        Direction local = worldFace;
        for (int step = 0; step < Math.floorMod(rotationSteps, 4); step++) {
            local = local.getCounterClockWise();
        }
        return local;
    }
}