package edn.lakeopossmc.drivebysable.command;

import edn.lakeopossmc.drivebysable.DriveBySableMod;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

// --- THE CUSTOM COMMAND ARGUMENTS --- //
// * Registered so the command tree can be sent to clients
public final class CableCommandArguments {
    private static final DeferredRegister<ArgumentTypeInfo<?, ?>> ARGUMENT_TYPES =
            DeferredRegister.create(Registries.COMMAND_ARGUMENT_TYPE, DriveBySableMod.MOD_ID);

    public static final DeferredHolder<ArgumentTypeInfo<?, ?>, SingletonArgumentInfo<TargetArgument>> TARGET =
            ARGUMENT_TYPES.register("target", () -> ArgumentTypeInfos.registerByClass(
                    TargetArgument.class, SingletonArgumentInfo.contextFree(TargetArgument::target)));

    public static final DeferredHolder<ArgumentTypeInfo<?, ?>, SingletonArgumentInfo<ModuleArgument>> MODULE =
            ARGUMENT_TYPES.register("module", () -> ArgumentTypeInfos.registerByClass(
                    ModuleArgument.class, SingletonArgumentInfo.contextFree(ModuleArgument::module)));

    public static final DeferredHolder<ArgumentTypeInfo<?, ?>, SingletonArgumentInfo<ChannelArgument>> CHANNEL =
            ARGUMENT_TYPES.register("channel", () -> ArgumentTypeInfos.registerByClass(
                    ChannelArgument.class, SingletonArgumentInfo.contextFree(ChannelArgument::channel)));

    private CableCommandArguments() {
    }

    public static void register(final IEventBus modEventBus) {
        ARGUMENT_TYPES.register(modEventBus);
    }
}