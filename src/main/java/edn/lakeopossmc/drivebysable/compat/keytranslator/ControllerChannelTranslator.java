package edn.lakeopossmc.drivebysable.compat.keytranslator;

import edn.lakeopossmc.drivebysable.compat.CableTypewriterHubServerHandler;
import edn.lakeopossmc.drivebysable.compat.PlayerTweakedKeybinds;
import edn.lakeopossmc.drivebysable.compat.TweakedControllerCableServerHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// --- TRANSLATES CHANNELS BETWEEN HUB VOCABULARIES --- //
// * Linked controller keys mirror the players movement binds
// * Tweaked controller buttons and axes resolve, but only when bound to keyboard
public final class ControllerChannelTranslator {

    // * Which channel naming scheme a hub actually speaks
    public enum Vocabulary {
        LINKED_CONTROLLER,
        TWEAKED_CONTROLLER,
        TYPEWRITER
    }

    // * Fallback when no player id is available
    private static final Map<String, String> LINKED_TO_TYPEWRITER = Map.of(
            "keyUp", "keyW",
            "keyDown", "keyS",
            "keyLeft", "keyA",
            "keyRight", "keyD",
            "keyJump", "keySpace",
            "keyShift", "keyLeftShift"
    );

    private static final Map<String, String> TYPEWRITER_TO_LINKED = Map.of(
            "keyW", "keyUp",
            "keyS", "keyDown",
            "keyA", "keyLeft",
            "keyD", "keyRight",
            "keySpace", "keyJump",
            "keyLeftShift", "keyShift"
    );

    // * Which movement keybind slot each linked controller channel resolves against
    private static final Map<String, Integer> MOVEMENT_INDEX = Map.of(
            "keyUp", PlayerMovementKeybinds.FORWARD,
            "keyDown", PlayerMovementKeybinds.BACK,
            "keyLeft", PlayerMovementKeybinds.LEFT,
            "keyRight", PlayerMovementKeybinds.RIGHT,
            "keyJump", PlayerMovementKeybinds.JUMP,
            "keyShift", PlayerMovementKeybinds.SNEAK
    );

    // * Slot 0 to 14 are buttons, 15 to 24 are axes
    private static final Map<String, Integer> TWEAKED_INDEX = buildTweakedIndex();

    private static Map<String, Integer> buildTweakedIndex() {
        final Map<String, Integer> index = new HashMap<>();
        for (int i = 0; i < TweakedControllerCableServerHandler.BUTTON_TO_CHANNEL.length; i++) {
            index.put(TweakedControllerCableServerHandler.BUTTON_TO_CHANNEL[i], i);
        }
        for (int i = 0; i < TweakedControllerCableServerHandler.AXIS_TO_CHANNEL.length; i++) {
            index.put(TweakedControllerCableServerHandler.AXIS_TO_CHANNEL[i], 15 + i);
        }
        return Map.copyOf(index);
    }

    private ControllerChannelTranslator() {
    }

    // * No player id available, falls back
    public static String translate(final String channel, final Vocabulary from, final Vocabulary to) {
        return translate(channel, from, to, null);
    }

    // * No match returns empty
    public static String translate(final String channel, final Vocabulary from, final Vocabulary to, final UUID playerId) {
        if (from == to) {
            return channel;
        }

        if (from == Vocabulary.LINKED_CONTROLLER && to == Vocabulary.TYPEWRITER) {
            if (playerId != null && MOVEMENT_INDEX.containsKey(channel)) {
                final int keycode = resolveKeycode(channel, MOVEMENT_INDEX, false, playerId);
                return keycode == -1 ? "" : CableTypewriterHubServerHandler.KEY_TO_CHANNEL.getOrDefault(keycode, "");
            }
            return LINKED_TO_TYPEWRITER.getOrDefault(channel, "");
        }

        if (from == Vocabulary.TYPEWRITER && to == Vocabulary.LINKED_CONTROLLER) {
            if (playerId != null) {
                final String matched = findChannelForKeycode(typewriterKeycode(channel), MOVEMENT_INDEX, false, playerId);
                if (matched != null) {
                    return matched;
                }
            }
            return TYPEWRITER_TO_LINKED.getOrDefault(channel, "");
        }

        // * Player must have this slot bound to a keyboard key
        if (from == Vocabulary.TWEAKED_CONTROLLER && to == Vocabulary.TYPEWRITER) {
            if (playerId != null && TWEAKED_INDEX.containsKey(channel)) {
                final int keycode = resolveKeycode(channel, TWEAKED_INDEX, true, playerId);
                return keycode == -1 ? "" : CableTypewriterHubServerHandler.KEY_TO_CHANNEL.getOrDefault(keycode, "");
            }
            return "";
        }

        if (from == Vocabulary.TYPEWRITER && to == Vocabulary.TWEAKED_CONTROLLER) {
            if (playerId != null) {
                final String matched = findChannelForKeycode(typewriterKeycode(channel), TWEAKED_INDEX, true, playerId);
                if (matched != null) {
                    return matched;
                }
            }
            return "";
        }

        // * Bridged straight through the players own keycodes
        if (from == Vocabulary.LINKED_CONTROLLER && to == Vocabulary.TWEAKED_CONTROLLER) {
            if (playerId != null && MOVEMENT_INDEX.containsKey(channel)) {
                final int keycode = resolveKeycode(channel, MOVEMENT_INDEX, false, playerId);
                final String matched = findChannelForKeycode(keycode, TWEAKED_INDEX, true, playerId);
                if (matched != null) {
                    return matched;
                }
            }
            return "";
        }

        if (from == Vocabulary.TWEAKED_CONTROLLER && to == Vocabulary.LINKED_CONTROLLER) {
            if (playerId != null && TWEAKED_INDEX.containsKey(channel)) {
                final int keycode = resolveKeycode(channel, TWEAKED_INDEX, true, playerId);
                final String matched = findChannelForKeycode(keycode, MOVEMENT_INDEX, false, playerId);
                if (matched != null) {
                    return matched;
                }
            }
            return "";
        }

        return channel;
    }

    // * Which real key the player has bound for channel
    private static int resolveKeycode(final String channel, final Map<String, Integer> index, final boolean tweaked, final UUID playerId) {
        if (!index.containsKey(channel)) {
            return -1;
        }
        return tweaked
                ? PlayerTweakedKeybinds.getKeycode(playerId, index.get(channel))
                : PlayerMovementKeybinds.getKeycode(playerId, index.get(channel));
    }

    // * Finds whichever channel bound to this exact key
    private static String findChannelForKeycode(final int keycode, final Map<String, Integer> index, final boolean tweaked, final UUID playerId) {
        if (keycode == -1) {
            return null;
        }
        for (final Map.Entry<String, Integer> entry : index.entrySet()) {
            final int candidate = tweaked
                    ? PlayerTweakedKeybinds.getKeycode(playerId, entry.getValue())
                    : PlayerMovementKeybinds.getKeycode(playerId, entry.getValue());
            if (candidate == keycode) {
                return entry.getKey();
            }
        }
        return null;
    }

    private static int typewriterKeycode(final String typewriterChannel) {
        for (final Map.Entry<Integer, String> entry : CableTypewriterHubServerHandler.KEY_TO_CHANNEL.entrySet()) {
            if (entry.getValue().equals(typewriterChannel)) {
                return entry.getKey();
            }
        }
        return -1;
    }
}