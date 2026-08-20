package edn.lakeopossmc.drivebysable.client;

import edn.lakeopossmc.drivebysable.CableConfig;
import edn.lakeopossmc.drivebysable.DriveBySableMod;
import edn.lakeopossmc.drivebysable.cable.BackupDriveBounds;
import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import java.util.List;

// --- THE CLIENT HALF OF THE GOGGLE TOOLTIP --- //
// * Kept apart from the block entity because everything here is client only
public final class BackupDriveGoggleClient {

    private static final String PREFIX = "goggles.";

    private BackupDriveGoggleClient() {
    }

    private static LangBuilder lang(final String key, final Object... args) {
        return new LangBuilder(DriveBySableMod.MOD_ID).translate(key, args);
    }

    // * Whether this player is charged for cables at all
    public static boolean showsCableCost() {
        final Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player != null
                && CableConfig.CONFIG.shouldConsumeCables.get()
                && !minecraft.player.hasInfiniteMaterials();
    }

    public static void appendRegionInfo(
            final BlockPos drivePos,
            final BlockPos offset,
            final BlockPos size,
            final int rotation,
            final List<Component> tooltip
    ) {
        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        final int[] tally = BackupDrivePreview.tallyFor(
                minecraft.level,
                drivePos,
                BackupDriveBounds.of(
                        drivePos,
                        new int[] {offset.getX(), offset.getY(), offset.getZ()},
                        new int[] {size.getX(), size.getY(), size.getZ()},
                        rotation
                )
        );

        final int valid = tally[BackupDrivePreview.TALLY_VALID];
        final int partial = tally[BackupDrivePreview.TALLY_PARTIAL];
        final int unreachable = tally[BackupDrivePreview.TALLY_SOURCE_UNREACHABLE];
        final int outputsOutside = tally[BackupDrivePreview.TALLY_OUTPUTS_OUTSIDE];
        final int invalid = unreachable + outputsOutside;

        if (valid + partial + invalid == 0) {
            lang(PREFIX + "no_data").style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
            return;
        }

        count(tooltip, "valid_sources", valid, ChatFormatting.GREEN);

        // * Each count brings only explanations that currently apply
        count(tooltip, "partial_sources", partial, ChatFormatting.GOLD);
        if (partial > 0) {
            reason(tooltip, "reason_outputs_outside", ChatFormatting.GOLD);
        }

        count(tooltip, "invalid_sources", invalid, ChatFormatting.RED);
        if (unreachable > 0) {
            reason(tooltip, "reason_other_level", ChatFormatting.RED);
        }
        if (outputsOutside > 0) {
            reason(tooltip, "reason_all_outside", ChatFormatting.RED);
        }

        // * Only while there is something to correct
        if (partial + invalid > 0) {
            lang(PREFIX + "fix_issues").style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
        }
    }

    // * Label then count
    private static void count(
            final List<Component> tooltip,
            final String key,
            final int amount,
            final ChatFormatting colour
    ) {
        lang(PREFIX + key)
                .style(ChatFormatting.GRAY)
                .space()
                .add(lang(PREFIX + "number", amount).style(colour))
                .forGoggles(tooltip, 1);
    }

    private static void reason(final List<Component> tooltip, final String key, final ChatFormatting colour) {
        lang(PREFIX + key).style(colour).forGoggles(tooltip, 2);
    }
}