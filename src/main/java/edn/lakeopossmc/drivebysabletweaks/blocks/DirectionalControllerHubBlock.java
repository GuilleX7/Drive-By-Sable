package edn.lakeopossmc.drivebysabletweaks.blocks;

import com.simibubi.create.AllItems;
import edn.stratodonut.drivebywire.WireSounds;
import edn.stratodonut.drivebywire.compat.LinkedControllerWireServerHandler;
import edn.stratodonut.drivebywire.util.HubItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.Arrays;
import java.util.List;

// --- SIMPLIFIED DIRECTIONAL VARIANT OF CONTROLLER HUB --- //
// * Shape handler no longer needs to be retyped, all handled in main class (yay!)
// * Still keeps check for correct controller type
// * Lists channels correctly while maintaining the og logic - again, no retyping needed (yay! x2)
public class DirectionalControllerHubBlock extends AbstractDirectionalHubBlock {
    // --- DEF FOR CHANNELS --- //
    private static final List<String> CHANNELS = Arrays.stream(LinkedControllerWireServerHandler.KEY_TO_CHANNEL).toList();

    // --- GET PROPS FROM MAIN --- //
    public DirectionalControllerHubBlock(final Properties properties) {
        super(properties);
    }

    // --- TELL MAIN THE CHANNELS TO LIST ON USE --- //
    @Override
    protected List<String> channels() {
        return CHANNELS;
    }

    // --- CHECK FOR LINKED CONTROLLER --- //
    @Override
    protected ItemInteractionResult useItemOn(
            final ItemStack itemStack,
            final BlockState state,
            final Level level,
            final BlockPos blockPos,
            final Player player,
            final InteractionHand interactionHand,
            final BlockHitResult hitResult
    ) {
        if (!AllItems.LINKED_CONTROLLER.isIn(itemStack)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (!level.isClientSide()) {
            HubItem.putHub(itemStack, blockPos);
            level.playSound(null, blockPos, WireSounds.PLUG_IN.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
            player.displayClientMessage(Component.literal("Controller connected!"), true);
        }

        return ItemInteractionResult.SUCCESS;
    }
}