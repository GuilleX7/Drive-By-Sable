package edn.lakeopossmc.drivebysable;

import edn.lakeopossmc.drivebysable.recipe.CableConfigCondition;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.level.storage.WorldData;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.resource.ResourcePackLoader;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;

// --- AUTOMATIC /reload WHEN A RECIPE AFFECTING CONFIG OPTION CHANGES --- //
public final class CableConfigReload {

    // * Values in effect during the last datapack load
    private static volatile boolean[] applied = null;

    private static final AtomicBoolean queued = new AtomicBoolean(false);

    private static boolean reloading = false;

    private static boolean rerun = false;

    private CableConfigReload() {
    }

    // * Fires at the start of every datapack load
    public static void onAddReloadListener(final AddReloadListenerEvent event) {
        applied = snapshot();
    }

    // * Only Reloading fires on an actual edit
    public static void onConfigReloading(final ModConfigEvent.Reloading event) {
        final ModConfig config = event.getConfig();
        if (!DriveBySableMod.MOD_ID.equals(config.getModId()) || config.getType() != ModConfig.Type.COMMON) {
            return;
        }
        requestReload();
    }

    private static void requestReload() {
        final MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }

        if (!hasChanged() || !queued.compareAndSet(false, true)) {
            return;
        }

        server.execute(() -> {
            queued.set(false);

            if (reloading) {
                rerun = true;
                return;
            }

            if (!hasChanged()) {
                return;
            }

            reloading = true;
            try {
                reloadDatapacks(server);
            } finally {
                reloading = false;
            }

            if (rerun) {
                rerun = false;
                requestReload();
            }
        });
    }

    private static boolean hasChanged() {
        final boolean[] last = applied;
        final boolean[] current = snapshot();
        return last != null && current != null && !Arrays.equals(last, current);
    }

    // * Null means the config was not readable
    private static boolean[] snapshot() {
        final CableConfigCondition.Option[] options = CableConfigCondition.Option.values();
        final boolean[] values = new boolean[options.length];

        try {
            for (int i = 0; i < options.length; i++) {
                values[i] = options[i].get();
            }
        } catch (final Exception e) {
            // * Config not loaded yet
            return null;
        }

        return values;
    }

    // * Mirrors the vanilla /reload command
    private static void reloadDatapacks(final MinecraftServer server) {
        final PackRepository repository = server.getPackRepository();
        final Collection<String> selected = repository.getSelectedIds();
        final Collection<String> toLoad = discoverNewPacks(repository, server.getWorldData(), selected);

        DriveBySableMod.LOGGER.info("Config option changed, reloading datapacks to apply the new recipes");

        server.reloadResources(toLoad).exceptionally(throwable -> {
            DriveBySableMod.LOGGER.warn("Failed to reload datapacks after a config change", throwable);
            server.getPlayerList().broadcastSystemMessage(Component.translatable("commands.reload.failure"), false);
            return null;
        });
    }

    // * Copy of ReloadCommand.discoverNewPacks
    private static Collection<String> discoverNewPacks(
            final PackRepository repository,
            final WorldData worldData,
            final Collection<String> selectedIds
    ) {
        repository.reload();

        final Collection<String> result = new ArrayList<>(selectedIds);
        final Collection<String> disabled = worldData.getDataConfiguration().dataPacks().getDisabled();

        for (final String id : repository.getAvailableIds()) {
            if (!disabled.contains(id) && !result.contains(id)) {
                result.add(id);
            }
        }

        ResourcePackLoader.reorderNewlyDiscoveredPacks(result, selectedIds, repository);
        return result;
    }
}