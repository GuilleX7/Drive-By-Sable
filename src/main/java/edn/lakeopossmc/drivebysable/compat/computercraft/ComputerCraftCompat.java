package edn.lakeopossmc.drivebysable.compat.computercraft;

import com.simibubi.create.compat.Mods;
import dan200.computercraft.api.peripheral.PeripheralCapability;
import edn.lakeopossmc.drivebysable.CableBlockEntities;
import edn.lakeopossmc.drivebysable.blocks.CableHubBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

public class ComputerCraftCompat {
    private ComputerCraftCompat() {
    }

    public static boolean isLoaded() {
        return Mods.COMPUTERCRAFT.isLoaded();
    }

    public static void register(final IEventBus modBus) {
        if (!ComputerCraftCompat.isLoaded()) {
            return;
        }

        modBus.addListener((final RegisterCapabilitiesEvent event) -> {
            ComputerCraftCompat.registerPeripherals(event);
        });
    }

    public static void registerPeripherals(final RegisterCapabilitiesEvent event) {
        final var peripheralCapability = PeripheralCapability.get();
        if (peripheralCapability == null) {
            return;
        }

        final var cableTypewriterHubHolder = CableBlockEntities.CABLE_TYPEWRITER_HUB;
        if (cableTypewriterHubHolder != null) {
            final var cableTypewriterHub = cableTypewriterHubHolder.get();
            if (cableTypewriterHub != null) {
                event.registerBlockEntity(peripheralCapability, cableTypewriterHub,
                        (b, d) -> new LinkedTypewriterHubPeripheral(b));
            }
        }

        final var cableHubHolder = CableBlockEntities.CABLE_HUB;
        if (cableHubHolder != null) {
            final var cableHub = cableHubHolder.get();
            if (cableHub != null) {
                event.registerBlockEntity(peripheralCapability, cableHub,
                        (b, d) -> new CableHubPeripheral(b));
            }
        }
    }

    public static void handleCableHubKeyPress(final Level level, final BlockPos blockPos, final int button,
            final boolean pressed, final boolean wasPressed) {
        if (!ComputerCraftCompat.isLoaded()) {
            return;
        }

        BlockEntity blockEntity = level.getBlockEntity(blockPos);
        if (blockEntity == null || !(blockEntity instanceof CableHubBlockEntity)) {
            return;
        }

        final CableHubBlockEntity cableHub = (CableHubBlockEntity) blockEntity;
        if (cableHub.computerHandler == null) {
            return;
        }

        if (pressed) {
            cableHub.computerHandler.queueEvent(cableHub.getComputerEventName("button"), button, wasPressed);
        } else {
            cableHub.computerHandler.queueEvent(cableHub.getComputerEventName("button_up"), button);
        }
    }
}
