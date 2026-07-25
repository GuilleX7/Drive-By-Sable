package edn.lakeopossmc.drivebysable.compat.keytranslator;

import com.getitemfromblock.create_tweaked_controllers.config.ModClientConfig;
import com.getitemfromblock.create_tweaked_controllers.controller.TweakedControlsUtil;
import com.getitemfromblock.create_tweaked_controllers.input.GenericInput;

// --- READS THE PLAYERS ACTUAL TWEAKED CONTROLLER KEY BINDS --- //
// * Only ever called from a mod presence guarded call site
public final class TweakedKeybindResolver {
    private TweakedKeybindResolver() {
    }

    // * Gamepad mode has no keyboard correspondence at all
    public static int[] resolveAll() {
        final int[] keycodes = new int[25];
        if (!ModClientConfig.USE_CUSTOM_MAPPINGS.get()) {
            java.util.Arrays.fill(keycodes, -1);
            return keycodes;
        }

        for (int i = 0; i < 25; i++) {
            final GenericInput input = TweakedControlsUtil.profile.layout[i];
            keycodes[i] = input != null && input.GetType() == GenericInput.InputType.KEYBOARD_KEY
                    ? input.GetValue()
                    : -1;
        }

        return keycodes;
    }
}