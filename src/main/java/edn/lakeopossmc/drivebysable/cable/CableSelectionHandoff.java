package edn.lakeopossmc.drivebysable.cable;

import edn.lakeopossmc.drivebysable.CableItems;
import edn.lakeopossmc.drivebysable.DriveBySableMod;
import edn.lakeopossmc.drivebysable.util.CableSelectionMark;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingSwapItemsEvent;

// --- THE SELECTION FOLLOWS A CABLE / CUTTER HAND SWAP --- //
// * Hands selection data back and forth when hotswapping tools
@EventBusSubscriber(modid = DriveBySableMod.MOD_ID)
public final class CableSelectionHandoff {
    private CableSelectionHandoff() {
    }

    @SubscribeEvent
    public static void onSwapHands(final LivingSwapItemsEvent.Hands event) {
        // * Named from where they are going
        final ItemStack toMainHand = event.getItemSwappedToMainHand();
        final ItemStack toOffHand = event.getItemSwappedToOffHand();

        if (!isOppositeTools(toMainHand, toOffHand)) {
            return;
        }

        if (CableSelectionMark.has(toMainHand)) {
            return;
        }

        CableSelectionMark.transfer(toOffHand, toMainHand);
    }

    // * One cable and one cutter, in either order
    private static boolean isOppositeTools(final ItemStack first, final ItemStack second) {
        return (first.is(CableItems.CABLE.get()) && second.is(CableItems.CABLE_CUTTER.get()))
                || (first.is(CableItems.CABLE_CUTTER.get()) && second.is(CableItems.CABLE.get()));
    }
}