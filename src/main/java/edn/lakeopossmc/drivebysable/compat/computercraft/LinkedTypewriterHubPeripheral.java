package edn.lakeopossmc.drivebysable.compat.computercraft;

import dan200.computercraft.api.lua.LuaFunction;
import dev.simulated_team.simulated.compat.computercraft.peripherals.LinkedTypewriterPeripheral;
import edn.lakeopossmc.drivebysable.blocks.CableTypewriterHubBlockEntity;

public class LinkedTypewriterHubPeripheral extends LinkedTypewriterPeripheral {
    public LinkedTypewriterHubPeripheral(final CableTypewriterHubBlockEntity blockEntity) {
        super(blockEntity);
    }

    @Override
    public String getType() {
        return "linked_typewriter_hub";
    }

    @LuaFunction
    public String getEventPrefix() {
        return ((CableTypewriterHubBlockEntity) this.blockEntity).getComputerEventPrefix();
    }

    @LuaFunction
    public void setEventPrefix(final String eventPrefix) {
        ((CableTypewriterHubBlockEntity) this.blockEntity).setComputerEventPrefix(eventPrefix);
    }

    @LuaFunction
    public boolean isInPromiscuousMode() {
        return ((CableTypewriterHubBlockEntity) this.blockEntity).isInPromiscuousMode();
    }

    @LuaFunction
    public void setPromiscuousMode(final boolean promiscuousMode) {
        ((CableTypewriterHubBlockEntity) this.blockEntity).setPromiscuousMode(promiscuousMode);
    }
}
