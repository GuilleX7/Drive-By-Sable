package edn.lakeopossmc.drivebysable.items;

import com.simibubi.create.foundation.item.TooltipHelper;
import edn.lakeopossmc.drivebysable.client.ClientCableNetworkHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLLoader;

import java.util.List;

// --- ITEM CLASS FOR CABLE --- //
// * Builds tooltip, actual connect logic lives client side
public class CableItem extends Item {

    private static final String TOOLTIP_KEY = "item.drivebysable.cable.tooltip";

    private static final Style GOLD_DARK = Style.EMPTY.withColor(0xC7954B);
    private static final Style GOLD_LIGHT = Style.EMPTY.withColor(0xEEDA78);

    public CableItem(final Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(final UseOnContext context) {
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide());
    }

    @Override
    public InteractionResult onItemUseFirst(final ItemStack stack, final UseOnContext context) {
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide());
    }

    // * Glint while a source is selected
    @Override
    public boolean isFoil(final ItemStack stack) {
        if (FMLLoader.getDist() != Dist.CLIENT) return false;
        if (!ClientCableNetworkHandler.isInSetupMode()) return false;
        final net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.player == null) return false;
        return mc.player.getMainHandItem() == stack || mc.player.getOffhandItem() == stack;
    }

    @Override
    public void appendHoverText(final ItemStack stack, final TooltipContext context,
                                final List<Component> tooltip, final TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);

        // * Check if shift held
        final boolean shiftDown = Screen.hasShiftDown();

        // * Find translation key for tip Hold [Shift] hint and apply color
        final Component shiftKey = Component.translatable("create.tooltip.keyShift")
                .copy().withStyle(shiftDown ? ChatFormatting.WHITE : ChatFormatting.GRAY);
        tooltip.add(Component.translatable("create.tooltip.holdForDescription", shiftKey)
                .withStyle(ChatFormatting.DARK_GRAY));

        if (!shiftDown) return;

        tooltip.add(Component.empty());

        final MutableComponent summary = Component.translatable(TOOLTIP_KEY + ".summary");
        TooltipHelper.cutTextComponent(summary, GOLD_DARK, GOLD_LIGHT).forEach(tooltip::add);
    }
}