package edn.lakeopossmc.drivebysable.compat.dashpanels;

import edn.lakeopossmc.drivebysable.cable.CableNetworkManager;
import edn.lakeopossmc.drivebysable.compat.ControllerSignalStore;
import moth.boxxed.panels.api.module.Module;
import moth.boxxed.panels.api.module.ModuleType;
import moth.boxxed.panels.api.module.io.IInput;
import moth.boxxed.panels.api.module.io.IMultiInput;
import moth.boxxed.panels.api.module.io.IMultiOutput;
import moth.boxxed.panels.api.module.io.IOutput;
import moth.boxxed.panels.api.module.io.ModuleIOInfo;
import moth.boxxed.panels.api.panel.AbstractPanelBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

// --- BRIDGES DASHPANELS INTO THE CABLE NETWORK --- //
// * Updated for Dashpanels 2.x
public final class DashPanelCableBridge {

    // * Dashpanels itself assumes this exact separator
    public static final String EXTENSION_SEPARATOR = " - ";
    private static final String SINGLE_EXTENSION = "";
    private static final int BOOKKEEPING_INTERVAL_TICKS = 10;
    // * How many consecutive empty scans before panel has no modules left
    private static final int EMPTY_SCAN_GRACE_PASSES = 3;

    private static final Map<Level, Map<BlockPos, PanelState>> STATE = new WeakHashMap<>();

    private DashPanelCableBridge() {
    }

    //#region // --- CHANNEL LISTING --- //

    // * Every input driven channel on the panel
    public static List<String> getSourceChannels(final Level level, final BlockPos pos) {
        return collectChannels(level, pos, null, true);
    }

    // * Only the channels belonging to one module
    public static List<String> getSourceChannels(final Level level, final BlockPos pos, @Nullable final String module) {
        return collectChannels(level, pos, module, true);
    }

    // * Every output driven channel on the panel
    public static List<String> getSinkChannels(final Level level, final BlockPos pos) {
        return collectChannels(level, pos, null, false);
    }

    public static List<String> getSinkChannels(final Level level, final BlockPos pos, @Nullable final String module) {
        return collectChannels(level, pos, module, false);
    }

    private static List<String> collectChannels(
            final Level level,
            final BlockPos pos,
            @Nullable final String moduleFilter,
            final boolean wantSources
    ) {
        final AbstractPanelBlockEntity panel = getPanel(level, pos);
        if (panel == null) {
            return List.of();
        }

        final List<String> channels = new ArrayList<>();
        for (final ModuleIOInfo info : panel.getModules().filterIOModules()) {
            if (moduleFilter != null && !moduleFilter.equals(info.name())) {
                continue;
            }

            switch (info.type()) {
                case INPUT -> {
                    if (wantSources) {
                        channels.add(info.name());
                    }
                }
                case OUTPUT -> {
                    if (!wantSources) {
                        channels.add(info.name());
                    }
                }
                case MULTI_INPUT -> {
                    if (wantSources) {
                        // * Routed through the quirks table, see DashPanelModuleQuirks
                        DashPanelModuleQuirks
                                .correctInputExtensions(panel.getModules().normalGet(info.name()), info.multiExtension())
                                .forEach(extension -> channels.add(channelName(info.name(), extension)));
                    }
                }
                case MULTI_OUTPUT -> {
                    if (!wantSources) {
                        info.multiExtension().forEach(extension -> channels.add(channelName(info.name(), extension)));
                    }
                }
                default -> {
                }
            }
        }
        return channels;
    }

    // * Walk a channel list, wrapping at either end
    @Nullable
    public static String nextSourceChannel(
            final Level level,
            final BlockPos pos,
            @Nullable final String module,
            final String current,
            final boolean forward
    ) {
        return step(getSourceChannels(level, pos, module), current, forward);
    }

    @Nullable
    public static String nextSinkChannel(
            final Level level,
            final BlockPos pos,
            @Nullable final String module,
            final String current,
            final boolean forward
    ) {
        return step(getSinkChannels(level, pos, module), current, forward);
    }

    @Nullable
    private static String step(final List<String> channels, final String current, final boolean forward) {
        if (channels.isEmpty()) {
            return null;
        }

        final int index = channels.indexOf(current);
        if (index == -1) {
            return channels.getFirst();
        }
        return channels.get(Math.floorMod(index + (forward ? 1 : -1), channels.size()));
    }
    //#endregion

    //#region // --- SUB TARGET RESOLUTION --- //

    // * Which module the player is aiming at
    @Nullable
    public static String pickModule(final Level level, final BlockPos pos, final Player player) {
        final AbstractPanelBlockEntity panel = getPanel(level, pos);
        if (panel == null || player == null) {
            return null;
        }

        final String selected = panel.getSelectedModule(player);
        if (selected == null || selected.isEmpty()) {
            return null;
        }

        // * Only report modules that still exist and can carry a signal
        return panel.getModules().normalContainsKey(selected) ? selected : null;
    }

    public static List<String> getModules(final Level level, final BlockPos pos) {
        final AbstractPanelBlockEntity panel = getPanel(level, pos);
        if (panel == null) {
            return List.of();
        }

        final List<String> modules = new ArrayList<>();
        panel.getModules().filterIOModules().forEach(info -> modules.add(info.name()));
        return modules;
    }

    @Nullable
    public static String moduleForChannel(final Level level, final BlockPos pos, final String channel) {
        final AbstractPanelBlockEntity panel = getPanel(level, pos);
        if (panel == null || channel == null || channel.isEmpty()) {
            return null;
        }

        String best = null;
        for (final ModuleIOInfo info : panel.getModules().filterIOModules()) {
            final String name = info.name();
            final boolean matches = channel.equals(name) || channel.startsWith(name + EXTENSION_SEPARATOR);
            if (matches && (best == null || name.length() > best.length())) {
                best = name;
            }
        }
        return best;
    }

    public static boolean isSourceModule(final Level level, final BlockPos pos, final String module) {
        final Module resolved = getModule(level, pos, module);
        return resolved instanceof IInput || resolved instanceof IMultiInput;
    }

    public static boolean isSinkModule(final Level level, final BlockPos pos, final String module) {
        final Module resolved = getModule(level, pos, module);
        return resolved instanceof IOutput || resolved instanceof IMultiOutput;
    }
    //#endregion

    //#region // --- SINK SIGNAL DELIVERY --- //

    // * Push a network value into an output module
    public static boolean applySinkSignal(final Level level, final BlockPos pos, final String channel, final int signal) {
        if (level == null || level.isClientSide) {
            return false;
        }

        final AbstractPanelBlockEntity panel = getPanel(level, pos);
        if (panel == null) {
            return false;
        }

        final String moduleName = moduleForChannel(level, pos, channel);
        if (moduleName == null) {
            return false;
        }

        final Module module = panel.getModules().normalGet(moduleName);
        if (module == null) {
            return false;
        }

        final int clamped = Math.clamp(signal, 0, 15);
        boolean applied = false;

        if (module instanceof final IOutput output && moduleName.equals(channel)) {
            output.setAnalog(clamped);
            applied = true;
        } else if (module instanceof final IMultiOutput multiOutput) {
            final String extension = extensionOf(moduleName, channel);
            if (extension != null) {
                final Map<String, IMultiOutput.AnalogRunnable> runnables = new HashMap<>();
                multiOutput.setValues(runnables::put);
                final IMultiOutput.AnalogRunnable runnable = runnables.get(extension);
                if (runnable != null) {
                    runnable.setAnalog(clamped);
                    applied = true;
                }
            }
        }

        if (applied) {
            markPanelChanged(panel);
        }
        return applied;
    }

    private static void markPanelChanged(final AbstractPanelBlockEntity panel) {
        if (panel.getOrCreate() != null) {
            panel.networkUpdate(panel.getOrCreate());
        } else {
            panel.setChanged();
            panel.blockChanged();
        }
    }
    //#endregion

    //#region // --- PER TICK PANEL SCAN --- //
    // * Push changed input values every tick
    public static void tick(final Level level, final BlockPos pos, final AbstractPanelBlockEntity panel) {
        if (level.isClientSide) {
            return;
        }

        final PanelState state = STATE
                .computeIfAbsent(level, ignored -> new HashMap<>())
                .computeIfAbsent(pos.immutable(), ignored -> new PanelState());

        final boolean runBookkeeping = state.tickCounter++ % BOOKKEEPING_INTERVAL_TICKS == 0;

        final Map<ModuleKey, Map<String, String>> currentSourceChannels = runBookkeeping ? new HashMap<>() : null;
        final Map<ModuleKey, Map<String, String>> currentSinkChannels = runBookkeeping ? new HashMap<>() : null;
        final Set<String> seenSourceChannels = runBookkeeping ? new HashSet<>() : null;
        final Set<String> seenSinkChannels = runBookkeeping ? new HashSet<>() : null;

        for (final ModuleIOInfo info : panel.getModules().filterIOModules()) {
            final Module module = panel.getModules().normalGet(info.name());
            if (module == null) {
                continue;
            }

            final ModuleKey key = runBookkeeping
                    ? new ModuleKey(module.type, module.getPos().x, module.getPos().y)
                    : null;

            switch (info.type()) {
                case INPUT -> {
                    if (module instanceof final IInput input) {
                        final String channel = info.name();
                        if (runBookkeeping) {
                            seenSourceChannels.add(channel);
                            state.channelOwner.put(channel, info.name());
                            currentSourceChannels.computeIfAbsent(key, ignored -> new HashMap<>())
                                    .put(SINGLE_EXTENSION, channel);
                        }
                        pushIfChanged(level, pos, channel, Math.clamp(input.getAnalog(), 0, 15), state.lastValues);
                    }
                }
                case MULTI_INPUT -> {
                    if (module instanceof final IMultiInput multiInput) {
                        state.scratchResults.clear();
                        multiInput.getValues(state.scratchResults::put);
                        // * Same corrected list the channel listing hands out
                        for (final String extension : DashPanelModuleQuirks.correctInputExtensions(module, info.multiExtension())) {
                            final Integer corrected =
                                    DashPanelModuleQuirks.correctInputValue(module, extension, state.scratchResults);
                            final IMultiInput.AnalogResult result = state.scratchResults.get(extension);
                            if (corrected == null && result == null) {
                                continue;
                            }
                            final String channel = channelName(info.name(), extension);
                            if (runBookkeeping) {
                                seenSourceChannels.add(channel);
                                state.channelOwner.put(channel, info.name());
                                currentSourceChannels.computeIfAbsent(key, ignored -> new HashMap<>())
                                        .put(extension, channel);
                            }
                            final int raw = corrected != null ? corrected : result.getAnalog();
                            pushIfChanged(level, pos, channel, Math.clamp(raw, 0, 15), state.lastValues);
                        }
                    }
                }
                case OUTPUT -> {
                    if (runBookkeeping && module instanceof IOutput) {
                        final String channel = info.name();
                        seenSinkChannels.add(channel);
                        state.channelOwner.put(channel, info.name());
                        currentSinkChannels.computeIfAbsent(key, ignored -> new HashMap<>())
                                .put(SINGLE_EXTENSION, channel);
                    }
                }
                case MULTI_OUTPUT -> {
                    if (runBookkeeping && module instanceof IMultiOutput) {
                        for (final String extension : info.multiExtension()) {
                            final String channel = channelName(info.name(), extension);
                            seenSinkChannels.add(channel);
                            state.channelOwner.put(channel, info.name());
                            currentSinkChannels.computeIfAbsent(key, ignored -> new HashMap<>())
                                    .put(extension, channel);
                        }
                    }
                }
                default -> {
                }
            }
        }

        if (!runBookkeeping) {
            return;
        }

        // * An empty panel is ambiguous
        if (panel.getModules().isEmpty()) {
            state.emptyScanStreak++;
        } else {
            state.emptyScanStreak = 0;
        }
        final boolean removalsTrustworthy =
                !panel.getModules().isEmpty() || state.emptyScanStreak >= EMPTY_SCAN_GRACE_PASSES;

        remapRenamedChannels(level, pos, state, currentSourceChannels, true);
        remapRenamedChannels(level, pos, state, currentSinkChannels, false);

        state.lastSourceChannelsByModule.clear();
        state.lastSourceChannelsByModule.putAll(currentSourceChannels);
        state.lastSinkChannelsByModule.clear();
        state.lastSinkChannelsByModule.putAll(currentSinkChannels);

        // * Drop wiring only for channels whose owning module is gone
        state.lastValues.keySet().removeIf(channel -> {
            if (!removalsTrustworthy
                    || seenSourceChannels.contains(channel)
                    || moduleStillPresent(panel, state, channel)) {
                return false;
            }
            CableNetworkManager.removeAllFromSourceChannel(level, pos, channel);
            state.channelOwner.remove(channel);
            return true;
        });

        state.knownSinkChannels.removeIf(channel -> {
            if (!removalsTrustworthy
                    || seenSinkChannels.contains(channel)
                    || moduleStillPresent(panel, state, channel)) {
                return false;
            }
            CableNetworkManager.removeAllToModuleSink(level, pos, channel);
            state.channelOwner.remove(channel);
            return true;
        });
        state.knownSinkChannels.addAll(seenSinkChannels);

        // * Re-assert what the network thinks each output should read
        final Map<String, Integer> networkValues = CableNetworkManager.get(level).getModuleSinkSignals(pos);
        for (final String channel : seenSinkChannels) {
            applySinkSignal(level, pos, channel, networkValues.getOrDefault(channel, 0));
        }
    }

    // * Is the module that owns this channel still on the panel?
    private static boolean moduleStillPresent(
            final AbstractPanelBlockEntity panel,
            final PanelState state,
            final String channel
    ) {
        final String owner = state.channelOwner.get(channel);
        if (owner != null) {
            return panel.getModules().normalContainsKey(owner);
        }

        // * No recorded owner
        for (final ModuleIOInfo info : panel.getModules().filterIOModules()) {
            final String name = info.name();
            if (channel.equals(name) || channel.startsWith(name + EXTENSION_SEPARATOR)) {
                return true;
            }
        }
        return false;
    }

    // * Same module identity, different channel name means it got renamed
    private static void remapRenamedChannels(
            final Level level,
            final BlockPos pos,
            final PanelState state,
            final Map<ModuleKey, Map<String, String>> current,
            final boolean sources
    ) {
        final Map<ModuleKey, Map<String, String>> previous =
                sources ? state.lastSourceChannelsByModule : state.lastSinkChannelsByModule;

        for (final Map.Entry<ModuleKey, Map<String, String>> entry : current.entrySet()) {
            final Map<String, String> previousExtensions = previous.get(entry.getKey());
            if (previousExtensions == null) {
                continue;
            }

            for (final Map.Entry<String, String> extensionEntry : entry.getValue().entrySet()) {
                final String oldChannel = previousExtensions.get(extensionEntry.getKey());
                final String newChannel = extensionEntry.getValue();
                if (oldChannel == null || oldChannel.equals(newChannel)) {
                    continue;
                }

                if (sources) {
                    CableNetworkManager.remapSourceChannel(level, pos, oldChannel, newChannel);
                    final Integer previousValue = state.lastValues.remove(oldChannel);
                    if (previousValue != null) {
                        state.lastValues.put(newChannel, previousValue);
                    }
                } else {
                    CableNetworkManager.remapModuleSink(level, pos, oldChannel, newChannel);
                    state.knownSinkChannels.remove(oldChannel);
                    state.knownSinkChannels.add(newChannel);
                }
                state.channelOwner.remove(oldChannel);
            }
        }
    }
    //#endregion

    public static void clear(final Level level, final BlockPos pos) {
        final Map<BlockPos, PanelState> perLevel = STATE.get(level);
        if (perLevel != null) {
            perLevel.remove(pos);
        }
        ControllerSignalStore.clear(level, pos);
    }

    //#region // --- HELPERS --- //
    public static String channelName(final String module, final String extension) {
        return extension == null || extension.isEmpty() ? module : module + EXTENSION_SEPARATOR + extension;
    }

    @Nullable
    private static String extensionOf(final String module, final String channel) {
        final String prefix = module + EXTENSION_SEPARATOR;
        return channel.startsWith(prefix) ? channel.substring(prefix.length()) : null;
    }

    private static void pushIfChanged(
            final Level level,
            final BlockPos pos,
            final String channel,
            final int value,
            final Map<String, Integer> lastValues
    ) {
        final Integer previous = lastValues.put(channel, value);
        if (previous == null || previous != value) {
            ControllerSignalStore.setSignal(level, pos, channel, value);
        }
    }

    @Nullable
    private static Module getModule(final Level level, final BlockPos pos, final String module) {
        final AbstractPanelBlockEntity panel = getPanel(level, pos);
        return panel == null || module == null ? null : panel.getModules().normalGet(module);
    }

    @Nullable
    public static AbstractPanelBlockEntity getPanel(final Level level, final BlockPos pos) {
        return level != null && level.getBlockEntity(pos) instanceof final AbstractPanelBlockEntity panel ? panel : null;
    }

    private record ModuleKey(ModuleType<?> type, int x, int y) {
    }
    //#endregion

    // * Per panel tick state, keyed off level and pos
    private static final class PanelState {
        private final Map<String, Integer> lastValues = new HashMap<>();
        private final Map<ModuleKey, Map<String, String>> lastSourceChannelsByModule = new HashMap<>();
        private final Map<ModuleKey, Map<String, String>> lastSinkChannelsByModule = new HashMap<>();
        private final Set<String> knownSinkChannels = new HashSet<>();
        // * Which module each channel belongs to
        private final Map<String, String> channelOwner = new HashMap<>();
        private final Map<String, IMultiInput.AnalogResult> scratchResults = new HashMap<>();
        private int tickCounter;
        private int emptyScanStreak;
    }
}