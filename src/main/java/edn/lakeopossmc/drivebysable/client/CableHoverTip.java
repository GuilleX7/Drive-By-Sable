package edn.lakeopossmc.drivebysable.client;

import com.simibubi.create.CreateClient;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.MutableComponent;

import java.util.List;

// --- HOVER TIP FOR THE CABLE TOOLS --- //
public final class CableHoverTip {

    private static boolean showing;

    private CableHoverTip() {
    }

    // * Hand the lines to Create
    public static void show(final List<MutableComponent> tip) {
        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.player.isSpectator()) {
            return;
        }

        showing = true;
        CreateClient.VALUE_SETTINGS_HANDLER.showHoverTip(tip);
    }

    // * Called at the start of every tick
    public static void clear() {
        showing = false;
    }

    // * Read by the mixin
    public static boolean isShowing() {
        return showing;
    }
}