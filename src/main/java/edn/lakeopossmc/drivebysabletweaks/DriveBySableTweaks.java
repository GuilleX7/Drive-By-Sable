package edn.lakeopossmc.drivebysabletweaks;

import com.mojang.logging.LogUtils;
import edn.stratodonut.drivebywire.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import org.slf4j.Logger;

@Mod(DriveBySableTweaks.MOD_ID)
public class DriveBySableTweaks {
    public static final String MOD_ID = "drivebysabletweaks";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final ResourceKey<CreativeModeTab> DRIVE_BY_WIRE_TAB =
            ResourceKey.create(Registries.CREATIVE_MODE_TAB, ResourceLocation.fromNamespaceAndPath("drivebywire", "base"));

    public DriveBySableTweaks(final IEventBus modEventBus, final ModContainer modContainer, final Dist dist) {
//        modContainer.registerConfig(ModConfig.Type.COMMON, WireConfig.CONFIG_SPEC);
//        if (dist == Dist.CLIENT) {
//            modContainer.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
//        }
        WireTweaksBlocks.register(modEventBus);
        WireTweaksItems.register(modEventBus);
        modEventBus.addListener(this::buildContents);
    }

    // fix creative tab so non-directionals are unavailable
    @SubscribeEvent
    public void buildContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == DRIVE_BY_WIRE_TAB) {

            // find the hardcoded creative tab item registers
            net.minecraft.world.item.ItemStack oldHub = edn.stratodonut.drivebywire.WireItems.CONTROLLER_HUB_BLOCK.get().getDefaultInstance();
            net.minecraft.world.item.ItemStack oldTweakedHub = edn.stratodonut.drivebywire.WireItems.TWEAKED_CONTROLLER_HUB_BLOCK.get().getDefaultInstance();

            // kill the old hardcoded item data from before
            event.remove(oldHub, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            event.remove(oldTweakedHub, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);

            // find my new items
            net.minecraft.world.item.ItemStack newHub = WireTweaksItems.CONTROLLER_HUB_BLOCK.get().getDefaultInstance();
            net.minecraft.world.item.ItemStack newTweakedHub = WireTweaksItems.TWEAKED_CONTROLLER_HUB_BLOCK.get().getDefaultInstance();

            // kill any accidental clones or dupes
            event.remove(newHub, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            event.remove(newTweakedHub, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);

            // force neoforge to accept the entries
            event.accept(newHub);
            event.accept(newTweakedHub);
        }
    }

    public static ResourceLocation asResource(final String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
