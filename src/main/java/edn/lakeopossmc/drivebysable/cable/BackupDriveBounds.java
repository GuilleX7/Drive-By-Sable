package edn.lakeopossmc.drivebysable.cable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

// --- THE REGION A BACKUP DRIVE CAPTURES --- //
public final class BackupDriveBounds {

    private BackupDriveBounds() {
    }

    // * Local box from the drive
    // * The offset rotates with the box
    public static AABB of(final BlockPos drive, final int[] offset, final int[] size, final int rotationSteps) {
        final int[] near = {offset[0], offset[1], offset[2]};
        final int[] far = {offset[0] + size[0], offset[1] + size[1], offset[2] + size[2]};

        rotateHorizontally(near, rotationSteps);
        rotateHorizontally(far, rotationSteps);

        return new AABB(
                drive.getX() + Math.min(near[0], far[0]),
                drive.getY() + Math.min(near[1], far[1]),
                drive.getZ() + Math.min(near[2], far[2]),
                drive.getX() + Math.max(near[0], far[0]),
                drive.getY() + Math.max(near[1], far[1]),
                drive.getZ() + Math.max(near[2], far[2])
        );
    }

    private static void rotateHorizontally(final int[] point, final int steps) {
        for (int step = 0; step < Math.floorMod(steps, 4); step++) {
            final int rotatedX = -point[2];
            final int rotatedZ = point[0];
            point[0] = rotatedX;
            point[2] = rotatedZ;
        }
    }

    public static boolean contains(final AABB box, final BlockPos pos) {
        return pos.getX() >= box.minX && pos.getX() < box.maxX
                && pos.getY() >= box.minY && pos.getY() < box.maxY
                && pos.getZ() >= box.minZ && pos.getZ() < box.maxZ;
    }
}