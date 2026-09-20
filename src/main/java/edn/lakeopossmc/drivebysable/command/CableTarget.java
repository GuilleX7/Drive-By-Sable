package edn.lakeopossmc.drivebysable.command;

import net.minecraft.core.BlockPos;

import javax.annotation.Nullable;

// --- A PARSED @target --- //
// * @coord[x, y, z, source|output], @look[...], @n[...], @rad[blocks, ...], @a[...]
public record CableTarget(Kind kind, Side side, @Nullable BlockPos coords, int radius) {

    // * What a target looks for
    public enum Kind {
        COORD("@coord"),
        LOOK("@look"),
        NEAREST("@n"),
        RADIUS("@rad"),
        ALL("@a");

        private final String selector;

        Kind(final String selector) {
            this.selector = selector;
        }

        public String selector() {
            return selector;
        }

        // * Commands that only make sense on one block accept these three
        public boolean single() {
            return this == COORD || this == LOOK || this == NEAREST;
        }
    }

    // * Which end of a connection the target is after
    public enum Side {
        SOURCE,
        OUTPUT,
        BOTH;

        public boolean wantsSources() {
            return this != OUTPUT;
        }

        public boolean wantsOutputs() {
            return this != SOURCE;
        }
    }

    // * @rad with no distance given
    public static final int DEFAULT_RADIUS = 64;

    public String asCommand() {
        final String inside = switch (kind) {
            case COORD -> coords == null
                    ? ""
                    : coords.getX() + ", " + coords.getY() + ", " + coords.getZ() + sidePart(true);
            case RADIUS -> radius + sidePart(true);
            default -> side == Side.BOTH ? "" : sidePart(false);
        };
        return inside.isEmpty() ? kind.selector() : kind.selector() + "[" + inside + "]";
    }

    private String sidePart(final boolean leading) {
        if (side == Side.BOTH) {
            return "";
        }
        final String word = side == Side.SOURCE ? "source" : "output";
        return leading ? ", " + word : word;
    }
}