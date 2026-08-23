package edn.lakeopossmc.drivebysable.client;

import edn.lakeopossmc.drivebysable.CableConfig;
import edn.lakeopossmc.drivebysable.DriveBySableMod;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.BuiltInPackSource;
import net.minecraft.server.packs.repository.KnownPack;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.event.AddPackFindersEvent;

import java.util.Optional;

// --- ALTERNATE HUB TEXTURES --- //
// * A built in resource pack
// * Reloads are only triggered when the config value actually changes
@EventBusSubscriber(modid = DriveBySableMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class AndesiteHubTextures {

    private static final String PACK_PATH = "resourcepacks/andesite_hub";

    private static final String PACK_ID = "mod/" + DriveBySableMod.MOD_ID + ":andesite_hub";

    private static Boolean appliedValue = null;

    private AndesiteHubTextures() {
    }

    @SubscribeEvent
    public static void onAddPackFinders(final AddPackFindersEvent event) {
        if (event.getPackType() != PackType.CLIENT_RESOURCES) {
            return;
        }

        final var modInfo = ModList.get()
                .getModContainerById(DriveBySableMod.MOD_ID)
                .orElseThrow()
                .getModInfo();
        final var resourcePath = modInfo.getOwningFile().getFile().findResource(PACK_PATH);
        final var version = modInfo.getVersion().toString();

        event.addRepositorySource(consumer -> {
            final boolean enabled = isEnabled();

            appliedValue = enabled;

            if (!enabled) {
                return;
            }

            final Pack pack = Pack.readMetaAndCreate(
                    new PackLocationInfo(
                            PACK_ID,
                            Component.translatable("drivebysable.pack.andesite_hub"),
                            PackSource.BUILT_IN,
                            Optional.of(new KnownPack("neoforge", PACK_ID, version))),
                    BuiltInPackSource.fromName(path -> new PathPackResources(path, resourcePath)),
                    PackType.CLIENT_RESOURCES,
                    new PackSelectionConfig(true, Pack.Position.TOP, false));

            if (pack != null) {
                consumer.accept(pack);
            }
        });
    }

    @SubscribeEvent
    public static void onConfigReloaded(final ModConfigEvent.Reloading event) {
        if (!DriveBySableMod.MOD_ID.equals(event.getConfig().getModId())) {
            return;
        }
        scheduleReloadIfChanged();
    }

    private static void scheduleReloadIfChanged() {
        final boolean wanted = isEnabled();
        if (appliedValue != null && appliedValue == wanted) {
            return;
        }

        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.options == null) {
            return;
        }

        minecraft.execute(minecraft::reloadResourcePacks);
    }

    private static boolean isEnabled() {
        try {
            return CableConfig.CONFIG.andesiteHub.get();
        } catch (final Exception e) {
            return false;
        }
    }

    @EventBusSubscriber(modid = DriveBySableMod.MOD_ID, value = Dist.CLIENT)
    public static final class JoinListener {

        private JoinListener() {
        }

        @SubscribeEvent
        public static void onJoin(final ClientPlayerNetworkEvent.LoggingIn event) {
            Minecraft.getInstance().execute(AndesiteHubTextures::scheduleReloadIfChanged);
        }
    }
}
