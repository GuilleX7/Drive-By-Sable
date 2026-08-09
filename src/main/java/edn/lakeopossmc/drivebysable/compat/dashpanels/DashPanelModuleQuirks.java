package edn.lakeopossmc.drivebysable.compat.dashpanels;

import moth.boxxed.panels.api.module.Module;
import moth.boxxed.panels.api.module.io.IMultiInput;
import moth.boxxed.panels.content.modules.PushButtonModule;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// --- TEMPORARY FIXUPS FOR DASHPANELS 2.1 MODULE QUIRKS --- //
// * Everything in this class works around bugs in a specific Dashpanels release and is meant to be deleted, not maintained
public final class DashPanelModuleQuirks {

    // * Their spelling, kept verbatim
    private static final String BUTTON_PREFIX = "Button ";
    private static final String SELECTED_BUTTON = "Selected Button";
    private static final int PRESSED = 15;

    private DashPanelModuleQuirks() {
    }

    // * Corrected channel list for a multi input module
    public static List<String> correctInputExtensions(@Nullable final Module module, final List<String> reported) {
        if (!(module instanceof PushButtonModule)) {
            return reported;
        }

        final List<String> corrected = new ArrayList<>(reported.size());
        int highestButton = -1;
        for (final String extension : reported) {
            final int index = buttonIndexOf(extension);
            if (index > highestButton) {
                highestButton = index;
            }
        }

        // * Nothing recognisable, leave it alone rather than guess
        if (highestButton < 1) {
            return reported;
        }

        for (final String extension : reported) {
            // * The top entry is the unreachable one
            if (buttonIndexOf(extension) == highestButton) {
                continue;
            }
            corrected.add(extension);

            // * Insert the missing zero right before Button 1 so the list stays in the order the player expects
            if (buttonIndexOf(extension) == 1) {
                corrected.add(corrected.size() - 1, BUTTON_PREFIX + 0);
            }
        }
        return corrected;
    }

    // * Value for one extension, or null when the caller should read the module
    @Nullable
    public static Integer correctInputValue(
            @Nullable final Module module,
            final String extension,
            final Map<String, IMultiInput.AnalogResult> published
    ) {
        if (!(module instanceof PushButtonModule)) {
            return null;
        }

        final int index = buttonIndexOf(extension);
        if (index < 0) {
            return null;
        }

        final IMultiInput.AnalogResult selected = published.get(SELECTED_BUTTON);
        if (selected == null) {
            return null;
        }

        return selected.getAnalog() == index + 1 ? PRESSED : 0;
    }

    // * Index from "Button 3", or -1 when the name is not a button channel
    private static int buttonIndexOf(final String extension) {
        if (extension == null || !extension.startsWith(BUTTON_PREFIX) || extension.equals(SELECTED_BUTTON)) {
            return -1;
        }

        try {
            return Integer.parseInt(extension.substring(BUTTON_PREFIX.length()).trim());
        } catch (final NumberFormatException ignored) {
            return -1;
        }
    }
}