package edn.lakeopossmc.drivebysabletweaks;

import edn.stratodonut.drivebywire.DriveByWireMod;
import edn.stratodonut.drivebywire.WireBlocks;
import edn.stratodonut.drivebywire.items.WireCutterItem;
import edn.stratodonut.drivebywire.items.WireItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class WireTweaksItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems("drivebywire");

    public static final DeferredItem<BlockItem> CONTROLLER_HUB_BLOCK = ITEMS.registerSimpleBlockItem("controller_hub", WireTweaksBlocks.CABLE_HUB);
    public static final DeferredItem<BlockItem> TWEAKED_CONTROLLER_HUB_BLOCK = ITEMS.registerSimpleBlockItem("tweaked_controller_hub", WireTweaksBlocks.ADVANCED_CABLE_HUB);

    private WireTweaksItems() {
    }

    public static void register(final IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
