package com.drmangotea.tfmg.content.decoration.tanks;

import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Shared base; capability registration lives on the concrete subclasses
 *  (AluminumTankBlockEntity, CastIronTankBlockEntity) so each material has
 *  its own BlockEntityType and never forms a multiblock with the other. */
public class TFMGFluidTankBlockEntity extends FluidTankBlockEntity {

    public TFMGFluidTankBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }
}
