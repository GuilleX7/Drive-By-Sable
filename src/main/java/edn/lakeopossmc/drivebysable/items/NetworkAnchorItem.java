package edn.lakeopossmc.drivebysable.items;

import com.simibubi.create.foundation.item.TooltipHelper;
import edn.lakeopossmc.drivebysable.CableConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.List;

// --- CONFIG GATED NETWORK ANCHOR --- //
// * Disabling the extension has to make the item unobtainable
public class NetworkAnchorItem extends BlockItem {

    private static final String TOOLTIP_KEY = "block.drivebysable.network_anchor.tooltip";

    private static final Style GOLD_DARK = Style.EMPTY.withColor(0xC7954B);
    private static final Style GOLD_LIGHT = Style.EMPTY.withColor(0xEEDA78);

    public NetworkAnchorItem(final Block block, final Properties properties) {
        super(block, properties);
    }

    //#region // --- TOOLTIP --- //
    @Override
    public void appendHoverText(final ItemStack stack, final TooltipContext context,
                                final List<Component> tooltip, final TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);

        final boolean shiftDown = Screen.hasShiftDown();

        final Component shiftKey = Component.translatable("create.tooltip.keyShift")
                .copy().withStyle(shiftDown ? ChatFormatting.WHITE : ChatFormatting.GRAY);
        tooltip.add(Component.translatable("create.tooltip.holdForDescription", shiftKey)
                .withStyle(ChatFormatting.DARK_GRAY));

        if (!shiftDown) return;

        tooltip.add(Component.empty());

        final MutableComponent summary = Component.translatable(TOOLTIP_KEY + ".summary");
        TooltipHelper.cutTextComponent(summary, GOLD_DARK, GOLD_LIGHT).forEach(tooltip::add);

        tooltip.add(Component.empty());

        // * Only show pairs that actually have translations
        for (int i = 1; i <= 4; i++) {
            final String conditionKey = TOOLTIP_KEY + ".condition" + i;
            final String behaviourKey = TOOLTIP_KEY + ".behaviour" + i;

            if (!I18n.exists(conditionKey) || !I18n.exists(behaviourKey)) continue;

            tooltip.add(Component.translatable(conditionKey).withStyle(ChatFormatting.GRAY));

            final MutableComponent behaviour = Component.translatable(behaviourKey);
            TooltipHelper.cutTextComponent(behaviour, GOLD_DARK, GOLD_LIGHT)
                    .forEach(line -> tooltip.add(Component.literal("  ").append(line)));
        }
    }
    //#endregion

    @Override
    public boolean isEnabled(final FeatureFlagSet enabledFeatures) {
        return super.isEnabled(enabledFeatures) && isExtensionEnabled();
    }

    public static boolean isExtensionEnabled() {
        try {
            return CableConfig.CONFIG.networkAnchor.get();
        } catch (final Exception e) {
            return true;
        }
    }
}