package edn.lakeopossmc.drivebysable.blocks;

import com.simibubi.create.content.equipment.clipboard.ClipboardCloneable;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

// --- THE CABLE HALF OF THE TYPEWRITER HUB'S CLIPBOARD --- //
// * This class enables the main class to directly carry over the linked typewriters copy/paste method
// * That way the cable code is kept separate and linked variant can cross-talk with the hub variant
public class CableTypewriterHubConnectionClipboard extends BlockEntityBehaviour implements ClipboardCloneable {
    public static final BehaviourType<CableTypewriterHubConnectionClipboard> TYPE = new BehaviourType<>();

    private final CableTypewriterHubBlockEntity hub;

    // * Whether the last real paste landed on a channel this hub owns
    private boolean pastedConnections;

    public CableTypewriterHubConnectionClipboard(final CableTypewriterHubBlockEntity hub) {
        super(hub);
        this.hub = hub;
    }

    @Override
    public BehaviourType<?> getType() {
        return TYPE;
    }

    @Override
    public String getClipboardKey() {
        return CableHubBlockEntity.CLIPBOARD_KEY;
    }

    @Override
    public boolean writeToClipboard(final HolderLookup.Provider registries, final CompoundTag tag,
                                    final Direction face) {
        return this.hub.writeConnectionsToClipboard(tag);
    }

    @Override
    public boolean readFromClipboard(final HolderLookup.Provider registries, final CompoundTag tag,
                                     final Player player, final Direction face, final boolean simulate) {
        final boolean matched = this.hub.applyConnectionsFromClipboard(tag, player, simulate);
        if (!simulate) {
            this.pastedConnections = matched;
        }
        return matched;
    }

    // * Read once and cleared, so a later paste cannot inherit this one's result
    public boolean consumePastedConnections() {
        final boolean pasted = this.pastedConnections;
        this.pastedConnections = false;
        return pasted;
    }
}