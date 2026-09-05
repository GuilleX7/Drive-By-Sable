package edn.lakeopossmc.drivebysable.compat.computercraft;

import java.util.List;

import org.jspecify.annotations.Nullable;

import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import edn.lakeopossmc.drivebysable.blocks.CableHubBlockEntity;
import edn.lakeopossmc.drivebysable.compat.LinkedControllerCableServerHandler;

public class CableHubPeripheral implements IPeripheral {
    private final CableHubBlockEntity blockEntity;

    public CableHubPeripheral(final CableHubBlockEntity blockEntity) {
        this.blockEntity = blockEntity;
    }

    @Override
    public String getType() {
        return "cable_hub";
    }

    @Override
    public void attach(IComputerAccess computer) {
        IPeripheral.super.attach(computer);
        if (this.blockEntity.computerHandler != null) {
            this.blockEntity.computerHandler.attach(computer);
        }
    }
    
    @Override
    public void detach(IComputerAccess computer) {
        IPeripheral.super.detach(computer);
        if (this.blockEntity.computerHandler != null) {
            this.blockEntity.computerHandler.detach(computer);
        }
    }

    @Override
    public boolean equals(@Nullable IPeripheral other) {
        return other == this;
    }

    @LuaFunction
    public List<Integer> getPressedButtons() {
        return LinkedControllerCableServerHandler.getPressed(blockEntity.getLevel(), blockEntity.getBlockPos());
    }

    @LuaFunction 
    public String getEventPrefix() {
        return this.blockEntity.getComputerEventPrefix();
    }

    @LuaFunction 
    public void setEventPrefix(String eventPrefix) {
        this.blockEntity.setComputerEventPrefix(eventPrefix);
    }
}
