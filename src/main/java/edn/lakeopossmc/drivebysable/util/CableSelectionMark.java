package edn.lakeopossmc.drivebysable.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

// --- THE SELECTED SOURCE WRITTEN ONTO THE TOOL --- //
public final class CableSelectionMark {
    private static final String ROOT_KEY = "DriveBySableSelection";
    private static final String POS_KEY = "Pos";
    private static final String MODULE_KEY = "Module";
    private static final String DIMENSION_KEY = "Dimension";

    // * Module is null when the whole block is the source
    public record Selection(BlockPos source, @Nullable String module, ResourceKey<Level> dimension) {
    }

    private CableSelectionMark() {
    }

    public static boolean has(final ItemStack stack) {
        final CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && data.copyTag().contains(ROOT_KEY, Tag.TAG_COMPOUND);
    }

    @Nullable
    public static Selection get(final ItemStack stack) {
        final CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return null;
        }

        final CompoundTag root = data.copyTag();
        if (!root.contains(ROOT_KEY, Tag.TAG_COMPOUND)) {
            return null;
        }

        final CompoundTag mark = root.getCompound(ROOT_KEY);
        if (!mark.contains(POS_KEY, Tag.TAG_LONG)) {
            return null;
        }

        final ResourceLocation dimension = ResourceLocation.tryParse(mark.getString(DIMENSION_KEY));
        if (dimension == null) {
            return null;
        }

        return new Selection(
                BlockPos.of(mark.getLong(POS_KEY)),
                mark.contains(MODULE_KEY, Tag.TAG_STRING) ? mark.getString(MODULE_KEY) : null,
                ResourceKey.create(Registries.DIMENSION, dimension)
        );
    }

    public static void put(
            final ItemStack stack,
            final BlockPos source,
            @Nullable final String module,
            final ResourceKey<Level> dimension
    ) {
        if (stack.isEmpty()) {
            return;
        }

        final CustomData existing = stack.get(DataComponents.CUSTOM_DATA);
        final CompoundTag root = existing == null ? new CompoundTag() : existing.copyTag();

        final CompoundTag mark = new CompoundTag();
        mark.putLong(POS_KEY, source.asLong());
        if (module != null) {
            mark.putString(MODULE_KEY, module);
        }
        mark.putString(DIMENSION_KEY, dimension.location().toString());

        root.put(ROOT_KEY, mark);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
    }

    // * Hands the selection from one tool to another
    public static boolean transfer(final ItemStack from, final ItemStack to) {
        final Selection selection = get(from);
        if (selection == null || to.isEmpty()) {
            return false;
        }

        put(to, selection.source(), selection.module(), selection.dimension());
        remove(from);
        return true;
    }

    public static void remove(final ItemStack stack) {
        final CustomData existing = stack.get(DataComponents.CUSTOM_DATA);
        if (existing == null) {
            return;
        }

        final CompoundTag root = existing.copyTag();
        if (!root.contains(ROOT_KEY)) {
            return;
        }

        root.remove(ROOT_KEY);

        if (root.isEmpty()) {
            stack.remove(DataComponents.CUSTOM_DATA);
        } else {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
        }
    }
}