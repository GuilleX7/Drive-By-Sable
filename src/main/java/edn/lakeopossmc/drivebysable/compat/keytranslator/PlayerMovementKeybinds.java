package edn.lakeopossmc.drivebysable.compat.keytranslator;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// --- CACHES EACH PLAYERS REAL MOVEMENT KEYBINDS --- //
// * Used when translating linked controller channels to typewriter channels
public final class PlayerMovementKeybinds {
    private static final Map<UUID, int[]> KEYBINDS = new HashMap<>();

    // * Index order matches linked controller
    public static final int FORWARD = 0;
    public static final int BACK = 1;
    public static final int LEFT = 2;
    public static final int RIGHT = 3;
    public static final int JUMP = 4;
    public static final int SNEAK = 5;

    private PlayerMovementKeybinds() {
    }

    public static void update(
            final UUID player,
            final int forward,
            final int back,
            final int left,
            final int right,
            final int jump,
            final int sneak
    ) {
        KEYBINDS.put(player, new int[] {forward, back, left, right, jump, sneak});
    }

    // * -1 for any slot means not a keyboard key
    public static int getKeycode(final UUID player, final int index) {
        final int[] keys = KEYBINDS.get(player);
        return keys == null ? -1 : keys[index];
    }
}