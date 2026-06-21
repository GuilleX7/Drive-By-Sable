package edn.lakeopossmc.drivebysabletweaks;

import edn.lakeopossmc.drivebysabletweaks.blocks.DirectionalControllerHubBlock;
import edn.lakeopossmc.drivebysabletweaks.blocks.DirectionalTweakedControllerHubBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

// --- BLOCK REGISTRY --- //
// * Defines general block stuff for the game to recognize
public final class WireTweaksBlocks {
    // --- CREATE REGISTRY UNDER MODID --- //
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks("drivebysabletweaks");

    // --- REGISTER MY FREAKING BLOCKS --- //
    public static final DeferredBlock<DirectionalControllerHubBlock> CABLE_HUB = BLOCKS.register(
            "cable_hub",
            () -> new DirectionalControllerHubBlock(commonProperties())
    );
    public static final DeferredBlock<DirectionalTweakedControllerHubBlock> ADVANCED_CABLE_HUB = BLOCKS.register(
            "advanced_cable_hub",
            () -> new DirectionalTweakedControllerHubBlock(commonProperties())
    );

    private WireTweaksBlocks() {
    }

    public static void register(final IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
    }

    // --- BLOCK PROPERTIES FOR ALL --- //
    private static BlockBehaviour.Properties commonProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_ORANGE)
                .sound(SoundType.COPPER)
                .strength(3.0F, 6.0F)
                .requiresCorrectToolForDrops();
    }
}