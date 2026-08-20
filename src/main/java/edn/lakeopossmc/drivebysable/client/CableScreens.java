package edn.lakeopossmc.drivebysable.client;

import edn.lakeopossmc.drivebysable.CableMenus;
import edn.lakeopossmc.drivebysable.DriveBySableMod;
import edn.lakeopossmc.drivebysable.client.screen.BackupDriveScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

// --- SCREEN BINDINGS --- //
@EventBusSubscriber(modid = DriveBySableMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class CableScreens {

    private CableScreens() {
    }

    @SubscribeEvent
    public static void onRegisterScreens(final RegisterMenuScreensEvent event) {
        event.register(CableMenus.BACKUP_DRIVE.get(), BackupDriveScreen::new);
    }
}