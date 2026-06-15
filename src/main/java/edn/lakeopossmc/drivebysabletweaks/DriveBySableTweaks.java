package edn.lakeopossmc.drivebysabletweaks;

import com.mojang.logging.LogUtils;
import edn.stratodonut.drivebywire.*;
import edn.stratodonut.drivebywire.network.WirePackets;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(DriveBySableTweaks.MOD_ID)
public class DriveBySableTweaks {
    public static final String MOD_ID = "drivebysabletweaks";
    public static final Logger LOGGER = LogUtils.getLogger();

    public DriveBySableTweaks(final IEventBus modEventBus, final ModContainer modContainer, final Dist dist) {
//        modContainer.registerConfig(ModConfig.Type.COMMON, WireConfig.CONFIG_SPEC);
//        if (dist == Dist.CLIENT) {
//            modContainer.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
//        }
        WireTweaksBlocks.register(modEventBus);
        WireTweaksItems.register(modEventBus);
//        WireCreativeTabs.register(modEventBus);
    }

    public static ResourceLocation asResource(final String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
