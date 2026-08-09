package edn.lakeopossmc.drivebysable.client;

import com.mojang.blaze3d.platform.InputConstants;
import edn.lakeopossmc.drivebysable.DriveBySableMod;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

// --- CLIENT KEY MAPPINGS --- //
// * Registered unbound
@EventBusSubscriber(modid = DriveBySableMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class CableKeyMappings {

    public static final String CATEGORY = "key.categories.drivebysable";

    // * Toggles the greyed out connections so a busy source can be read one channel at a time
    public static final KeyMapping HIDE_INACTIVE_CHANNELS = new KeyMapping(
            "key.drivebysable.hide_inactive_channels",
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            CATEGORY
    );

    private CableKeyMappings() {
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(final RegisterKeyMappingsEvent event) {
        event.register(HIDE_INACTIVE_CHANNELS);
    }
}