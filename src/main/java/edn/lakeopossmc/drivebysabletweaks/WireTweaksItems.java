package edn.lakeopossmc.drivebysabletweaks;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

// --- ITEM REGISTRY --- //
// * Defines general item stuff for the game to recognize
public final class WireTweaksItems {
    // --- CREATE REGISTRY UNDER MODID --- //
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems("drivebysabletweaks");

    // --- REGISTER MY FREAKING ITEMS --- //
    public static final DeferredItem<BlockItem> CONTROLLER_HUB_BLOCK = ITEMS.registerSimpleBlockItem("cable_hub", WireTweaksBlocks.CABLE_HUB);
    public static final DeferredItem<BlockItem> TWEAKED_CONTROLLER_HUB_BLOCK = ITEMS.registerSimpleBlockItem("advanced_cable_hub", WireTweaksBlocks.ADVANCED_CABLE_HUB);
    public static final DeferredItem<Item> CABLE_IO_BUS = ITEMS.registerSimpleItem("cable_io_bus");
    public static final DeferredItem<Item> INCOMPLETE_CABLE_IO_BUS = ITEMS.registerSimpleItem("incomplete_cable_io_bus");

    private WireTweaksItems() {
    }

    public static void register(final IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
