package edn.lakeopossmc.drivebysable.compat;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// --- CACHES EACH PLAYERS TWEAKED CONTROLLER KEYBOARD BINDS --- //
// * Client sends the resolved keycodes and stores them here
// * Slot order matches BUTTON_TO_CHANNEL then AXIS_TO_CHANNEL
public final class PlayerTweakedKeybinds {
    private static final Map<UUID, int[]> KEYBINDS = new HashMap<>();

    private PlayerTweakedKeybinds() {
    }

    public static void update(final UUID player, final int[] keycodes) {
        KEYBINDS.put(player, keycodes);
    }

    public static int getKeycode(final UUID player, final int index) {
        final int[] keys = KEYBINDS.get(player);
        return keys == null || index >= keys.length ? -1 : keys[index];
    }
}