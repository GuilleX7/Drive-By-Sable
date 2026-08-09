package edn.lakeopossmc.drivebysable.mixin.compat.dashpanels;

import edn.lakeopossmc.drivebysable.cable.ModuleSinkTarget;
import edn.lakeopossmc.drivebysable.cable.MultiChannelCableSource;
import edn.lakeopossmc.drivebysable.cable.SubTargetCableEndpoint;
import edn.lakeopossmc.drivebysable.compat.dashpanels.DashPanelCableBridge;
import edn.lakeopossmc.drivebysable.compat.dashpanels.DashPanelModuleGeometry;
import edn.lakeopossmc.drivebysable.util.CableOutlineBox;
import moth.boxxed.panels.api.panel.AbstractPanelBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.util.List;

// --- MAKES EVERY DASHPANEL A MODULE LEVEL CABLE ENDPOINT --- //
// * Pseudo since the mod may not be loaded
@Pseudo
@Mixin(AbstractPanelBlock.class)
public abstract class MixinAbstractPanelBlock implements MultiChannelCableSource, ModuleSinkTarget, SubTargetCableEndpoint {

    //#region // --- SOURCES --- //
    @Override
    public List<String> cable$getChannels(final Level level, final BlockPos pos) {
        return DashPanelCableBridge.getSourceChannels(level, pos);
    }

    @Override
    public List<String> cable$getChannels(final Level level, final BlockPos pos, @Nullable final String subTarget) {
        return DashPanelCableBridge.getSourceChannels(level, pos, subTarget);
    }

    @Override
    public String cable$nextChannel(final Level level, final BlockPos pos, final String current, final boolean forward) {
        return DashPanelCableBridge.nextSourceChannel(level, pos, null, current, forward);
    }

    @Override
    public String cable$nextChannel(
            final Level level,
            final BlockPos pos,
            @Nullable final String subTarget,
            final String current,
            final boolean forward
    ) {
        return DashPanelCableBridge.nextSourceChannel(level, pos, subTarget, current, forward);
    }

    @Override
    public boolean cable$hasSubTargets() {
        return true;
    }
    //#endregion

    //#region // --- SINKS --- //
    @Override
    public List<String> cable$getSinkChannels(final Level level, final BlockPos pos) {
        return DashPanelCableBridge.getSinkChannels(level, pos);
    }

    @Override
    public List<String> cable$getSinkChannels(final Level level, final BlockPos pos, @Nullable final String subTarget) {
        return DashPanelCableBridge.getSinkChannels(level, pos, subTarget);
    }

    @Override
    public String cable$nextSinkChannel(
            final Level level,
            final BlockPos pos,
            @Nullable final String subTarget,
            final String current,
            final boolean forward
    ) {
        return DashPanelCableBridge.nextSinkChannel(level, pos, subTarget, current, forward);
    }

    @Override
    public boolean cable$applySinkSignal(final Level level, final BlockPos pos, final String channel, final int signal) {
        return DashPanelCableBridge.applySinkSignal(level, pos, channel, signal);
    }
    //#endregion

    //#region // --- SUB TARGETS --- //
    @Override
    public String cable$pickSubTarget(final Level level, final BlockPos pos, final Player player) {
        return DashPanelCableBridge.pickModule(level, pos, player);
    }

    @Override
    public List<String> cable$getSubTargets(final Level level, final BlockPos pos) {
        return DashPanelCableBridge.getModules(level, pos);
    }

    @Override
    public String cable$subTargetForChannel(final Level level, final BlockPos pos, final String channel) {
        return DashPanelCableBridge.moduleForChannel(level, pos, channel);
    }

    @Override
    public boolean cable$isSourceSubTarget(final Level level, final BlockPos pos, final String subTarget) {
        return DashPanelCableBridge.isSourceModule(level, pos, subTarget);
    }

    @Override
    public boolean cable$isSinkSubTarget(final Level level, final BlockPos pos, final String subTarget) {
        return DashPanelCableBridge.isSinkModule(level, pos, subTarget);
    }

    @Override
    public List<CableOutlineBox> cable$getSubTargetOutline(final Level level, final BlockPos pos, final String subTarget) {
        return DashPanelModuleGeometry.outlineFor(level, pos, subTarget);
    }
    //#endregion

    // * Clear bridge state when the block is actually replaced
    @Inject(
            method = "onRemove",
            at = @At("TAIL")
    )
    private void drivebysable$clearCableBridge(
            final BlockState state,
            final Level level,
            final BlockPos pos,
            final BlockState newState,
            final boolean movedByPiston,
            final CallbackInfo ci
    ) {
        if (!state.is(newState.getBlock()) && level instanceof final ServerLevel serverLevel) {
            DashPanelCableBridge.clear(serverLevel, pos);
        }
    }
}
