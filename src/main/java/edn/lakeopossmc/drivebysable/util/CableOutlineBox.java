package edn.lakeopossmc.drivebysable.util;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.function.BiConsumer;

// --- ARBITRARILY ORIENTED BOX IN WORLD SPACE --- //
public record CableOutlineBox(Vec3[] corners) {

    // * Corner index is a bit mask
    private static final int[][] EDGES = {
            {0, 1}, {2, 3}, {4, 5}, {6, 7},
            {0, 2}, {1, 3}, {4, 6}, {5, 7},
            {0, 4}, {1, 5}, {2, 6}, {3, 7}
    };

    public CableOutlineBox {
        if (corners.length != 8) {
            throw new IllegalArgumentException("A box needs exactly 8 corners, got " + corners.length);
        }
    }

    // * Build from a local space box plus the matrix that maps to world
    public static CableOutlineBox of(final AABB local, final Matrix4f localToWorld, final Vec3 worldOrigin) {
        final Vec3[] corners = new Vec3[8];
        for (int index = 0; index < 8; index++) {
            final float x = (float) ((index & 1) == 0 ? local.minX : local.maxX);
            final float y = (float) ((index & 2) == 0 ? local.minY : local.maxY);
            final float z = (float) ((index & 4) == 0 ? local.minZ : local.maxZ);

            final Vector3f transformed = localToWorld.transformPosition(new Vector3f(x, y, z));
            corners[index] = new Vec3(
                    worldOrigin.x + transformed.x,
                    worldOrigin.y + transformed.y,
                    worldOrigin.z + transformed.z
            );
        }
        return new CableOutlineBox(corners);
    }

    // * Straight axis aligned box
    public static CableOutlineBox of(final AABB world) {
        final Vec3[] corners = new Vec3[8];
        for (int index = 0; index < 8; index++) {
            corners[index] = new Vec3(
                    (index & 1) == 0 ? world.minX : world.maxX,
                    (index & 2) == 0 ? world.minY : world.maxY,
                    (index & 4) == 0 ? world.minZ : world.maxZ
            );
        }
        return new CableOutlineBox(corners);
    }

    public void forEachEdge(final BiConsumer<Vec3, Vec3> consumer) {
        for (final int[] edge : EDGES) {
            consumer.accept(corners[edge[0]], corners[edge[1]]);
        }
    }

    public int edgeCount() {
        return EDGES.length;
    }

    // * Midpoint of the box, used as the cable line endpoint
    public Vec3 center() {
        double x = 0;
        double y = 0;
        double z = 0;
        for (final Vec3 corner : corners) {
            x += corner.x;
            y += corner.y;
            z += corner.z;
        }
        return new Vec3(x / 8D, y / 8D, z / 8D);
    }

    // * Slightly puffed up copy
    public CableOutlineBox inflated(final double amount) {
        final Vec3 center = center();
        final Vec3[] grown = new Vec3[8];
        for (int index = 0; index < 8; index++) {
            final Vec3 corner = corners[index];
            final Vec3 offset = corner.subtract(center);
            final double length = offset.length();
            grown[index] = length < 1.0E-6D
                    ? corner
                    : center.add(offset.scale((length + amount) / length));
        }
        return new CableOutlineBox(grown);
    }
}
