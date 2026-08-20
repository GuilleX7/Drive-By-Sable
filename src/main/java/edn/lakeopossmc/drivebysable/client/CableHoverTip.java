package edn.lakeopossmc.drivebysable.client;

import com.simibubi.create.CreateClient;
import edn.lakeopossmc.drivebysable.DriveBySableMod;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.ArrayList;
import java.util.List;

// --- HOVER TIP FOR THE CABLE TOOLS --- //
@EventBusSubscriber(modid = DriveBySableMod.MOD_ID, value = Dist.CLIENT)
public final class CableHoverTip {

    private static final List<MutableComponent> pinnedLines = new ArrayList<>();
    private static int pinnedTicks;

    private static final int SETTLED_WARMUP = 10;

    private static final int SETTLED_HOVER_TICKS = 6;

    private static final int FADE_GRACE_TICKS = 15;
    private static int fadeGraceTicks;
    private static boolean showing;

    private CableHoverTip() {
    }

    //#region // --- PER TICK TIPS --- //
    // * Ignored while a pinned tip is up
    public static void show(final List<MutableComponent> tip) {
        if (pinnedTicks > 0) {
            return;
        }

        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.player.isSpectator()) {
            return;
        }

        // * Copied for the same reason
        showing = true;
        CreateClient.VALUE_SETTINGS_HANDLER.showHoverTip(List.copyOf(tip));
    }

    // * Called at the start of every tick
    public static void clear() {
        if (pinnedTicks <= 0 && fadeGraceTicks <= 0) {
            showing = false;
        }
    }
    //#endregion

    //#region // --- PINNED TIPS --- //
    public static void pin(final List<MutableComponent> lines, final int ticks) {
        pinnedLines.clear();
        pinnedLines.addAll(lines);
        pinnedTicks = ticks;

        CreateClient.VALUE_SETTINGS_HANDLER.hoverWarmup = SETTLED_WARMUP;

    }

    @SubscribeEvent
    public static void onClientTick(final ClientTickEvent.Post event) {
        if (pinnedTicks <= 0 || pinnedLines.isEmpty()) {
            return;
        }

        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            pinnedTicks = 0;
            fadeGraceTicks = 0;
            return;
        }

        if (minecraft.screen != null) {
            return;
        }

        pinnedTicks--;
        showing = true;
        fadeGraceTicks = FADE_GRACE_TICKS;

        CreateClient.VALUE_SETTINGS_HANDLER.hoverWarmup = SETTLED_WARMUP;
        CreateClient.VALUE_SETTINGS_HANDLER.showHoverTip(List.copyOf(pinnedLines));

        CreateClient.VALUE_SETTINGS_HANDLER.hoverTicks = SETTLED_HOVER_TICKS;

        if (pinnedTicks <= 0) {

            pinnedLines.clear();
        }
    }
    //#endregion

    // * Counted down separately from the pinned timer
    @SubscribeEvent
    public static void onFadeTick(final ClientTickEvent.Post event) {
        if (pinnedTicks <= 0 && fadeGraceTicks > 0) {
            fadeGraceTicks--;
        }
    }

    public static boolean isShowing() {
        return showing;
    }
}