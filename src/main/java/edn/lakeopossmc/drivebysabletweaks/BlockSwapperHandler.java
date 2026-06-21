package edn.lakeopossmc.drivebysabletweaks;

import edn.stratodonut.drivebywire.WireBlocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

// --- THE DREADED BLOCK SWAPPER --- //
// * Originally did not work as intended. should be fixed now i think
// * Basically, if you somehow got the og block (probably thru /give) this is supposed to replace them with the new ones
// * This replacement happens when you place the blocks down (supposed to)
@EventBusSubscriber(modid = DriveBySableTweaks.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class BlockSwapperHandler {

    // --- TARGET WHEN OG BLOCK IS PLACED DOWN ---//
    @SubscribeEvent
    public static void onBlockPlaced(final BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }

        final Block placed = event.getPlacedBlock().getBlock();
        final Block replacement = resolveReplacement(placed);
        if (replacement == null) {
            return;
        }

        // --- TRY TO PRESERVE THE OLD SIMPLE BLOCKSTATE FIRST, THEN PLACE THE NEW BLOCK ---//
        final BlockState oldState = event.getPlacedBlock();
        BlockState newState = replacement.defaultBlockState();
        for (var property : oldState.getProperties()) {
            if (newState.hasProperty(property)) {
                newState = copyProperty(newState, oldState, property);
            }
        }

        event.getLevel().setBlock(event.getPos(), newState, 3);
    }

    // --- I FREAKING HATE WARNINGS (NOT RLLY) --- //
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static BlockState copyProperty(BlockState target, BlockState source, net.minecraft.world.level.block.state.properties.Property property) {
        return target.setValue(property, source.getValue(property));
    }

    // --- ACTUAL REPLACEMENT STUFF --- //
    private static Block resolveReplacement(final Block placed) {
        if (placed == WireBlocks.CONTROLLER_HUB.get()) {
            return WireTweaksBlocks.CABLE_HUB.get();
        }
        if (placed == WireBlocks.TWEAKED_CONTROLLER_HUB.get()) {
            return WireTweaksBlocks.ADVANCED_CABLE_HUB.get();
        }
        return null;
    }
}