package com.drmangotea.tfmg.content.decoration.tanks.cast_iron;

import com.drmangotea.tfmg.content.decoration.tanks.TFMGFluidTankBlockEntity;
import com.drmangotea.tfmg.mixin.accessor.FluidTankBlockEntityAccessor;
import com.drmangotea.tfmg.registry.TFMGBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/** Distinct BlockEntity type so cast iron tanks never form a multiblock with aluminum. */
public class CastIronTankBlockEntity extends TFMGFluidTankBlockEntity {

    public CastIronTankBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                TFMGBlockEntities.CAST_IRON_FLUID_TANK_BE.get(),
                (be, context) -> {
                    if (((FluidTankBlockEntityAccessor) be).tfmg$getFluidCapability() == null)
                        ((FluidTankBlockEntityAccessor) be).tfmg$refreshCapability();
                    return ((FluidTankBlockEntityAccessor) be).tfmg$getFluidCapability();
                }
        );
    }
}
